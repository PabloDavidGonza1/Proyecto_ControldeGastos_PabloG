package miumg.edu.gt.controlgastos.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors

// Los datos del clima que mostramos en pantalla
data class Clima(
    val temperatura: Double,
    val codigo: Int,
    val maxima: Double,
    val minima: Double,
    val probLluvia: Int,
    val ciudad: String,
    val actualizado: Long
) {
    companion object {
        // Convierte el código del clima (tabla internacional WMO) en un texto
        fun descripcionDe(codigo: Int): String = when (codigo) {
            0 -> "Despejado"
            1 -> "Mayormente despejado"
            2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Niebla"
            51, 53, 55, 56, 57 -> "Llovizna"
            61 -> "Lluvia ligera"
            63 -> "Lluvia"
            65, 66, 67 -> "Lluvia fuerte"
            71, 73, 75, 77, 85, 86 -> "Nieve"
            80, 81 -> "Chubascos"
            82 -> "Chubascos fuertes"
            95 -> "Tormenta eléctrica"
            96, 99 -> "Tormenta con granizo"
            else -> "Sin información"
        }
    }
}

// Una ciudad encontrada al buscar por nombre
data class Lugar(
    val nombre: String,
    val detalle: String,
    val latitud: Double,
    val longitud: Double
)

object ServicioClima {

    private const val ARCHIVO = "cache_clima"
    private const val VIGENCIA_MS = 30 * 60 * 1000L   // 30 minutos

    // Todo lo de internet se hace en un hilo aparte para no trabar la pantalla
    private val hilo = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())

    // ---------- Lo último que se guardó ----------
    fun guardado(c: Context): Clima? {
        val p = c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        if (!p.contains("temp")) return null
        return Clima(
            temperatura = p.getFloat("temp", 0f).toDouble(),
            codigo = p.getInt("codigo", 0),
            maxima = p.getFloat("max", 0f).toDouble(),
            minima = p.getFloat("min", 0f).toDouble(),
            probLluvia = p.getInt("lluvia", 0),
            ciudad = p.getString("ciudad", "") ?: "",
            actualizado = p.getLong("hora", 0L)
        )
    }

    private fun guardar(c: Context, k: Clima) {
        c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit()
            .putFloat("temp", k.temperatura.toFloat())
            .putInt("codigo", k.codigo)
            .putFloat("max", k.maxima.toFloat())
            .putFloat("min", k.minima.toFloat())
            .putInt("lluvia", k.probLluvia)
            .putString("ciudad", k.ciudad)
            .putLong("hora", k.actualizado)
            .apply()
    }

    // Se usa al cambiar de ciudad, para no mostrar el clima de la anterior
    fun borrarCache(c: Context) {
        c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // ---------- Cargar el clima ----------
    // "alTerminar" se llama primero con lo que ya se tenía guardado (puede ser null)
    // y, si hace falta, otra vez con el dato nuevo. Siempre en el hilo principal.
    fun cargar(c: Context, alTerminar: (Clima?) -> Unit) {
        val app = c.applicationContext
        val previo = guardado(app)
        val vigente = previo != null &&
                System.currentTimeMillis() - previo.actualizado < VIGENCIA_MS

        // Se muestra enseguida lo último que se tenía
        alTerminar(previo)
        if (vigente) return

        hilo.execute {
            val nuevo = try {
                pedir(app)
            } catch (e: Exception) {
                null   // sin internet o error: se queda lo anterior
            }
            if (nuevo != null) {
                guardar(app, nuevo)
                principal.post { alTerminar(nuevo) }
            }
        }
    }

    private fun pedir(c: Context): Clima {
        val ciudad = Preferencias.ciudad(c)
        val lat = String.format(Locale.US, "%.4f", Preferencias.latitud(c))
        val lon = String.format(Locale.US, "%.4f", Preferencias.longitud(c))

        val direccion = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=1"

        val json = JSONObject(descargar(direccion))
        val actual = json.getJSONObject("current")
        val dia = json.getJSONObject("daily")

        return Clima(
            temperatura = actual.getDouble("temperature_2m"),
            codigo = actual.getInt("weather_code"),
            maxima = dia.getJSONArray("temperature_2m_max").getDouble(0),
            minima = dia.getJSONArray("temperature_2m_min").getDouble(0),
            probLluvia = dia.getJSONArray("precipitation_probability_max").optInt(0, 0),
            ciudad = ciudad,
            actualizado = System.currentTimeMillis()
        )
    }

    // ---------- Buscar ciudades por nombre ----------
    fun buscarLugares(nombre: String, alTerminar: (List<Lugar>?) -> Unit) {
        hilo.execute {
            val resultado = try {
                val direccion = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                        URLEncoder.encode(nombre, "UTF-8") +
                        "&count=8&language=es&format=json"

                val json = JSONObject(descargar(direccion))
                val lista = mutableListOf<Lugar>()

                // Si no hay coincidencias, "results" no viene en la respuesta
                val arreglo = json.optJSONArray("results")
                if (arreglo != null) {
                    for (i in 0 until arreglo.length()) {
                        val o = arreglo.getJSONObject(i)
                        val detalle = listOf(o.optString("admin1", ""), o.optString("country", ""))
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        lista.add(
                            Lugar(
                                nombre = o.getString("name"),
                                detalle = detalle,
                                latitud = o.getDouble("latitude"),
                                longitud = o.getDouble("longitude")
                            )
                        )
                    }
                }
                lista
            } catch (e: Exception) {
                null
            }
            principal.post { alTerminar(resultado) }
        }
    }

    // ---------- Descarga de una dirección web ----------
    private fun descargar(direccion: String): String {
        val conexion = URL(direccion).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 8000
            conexion.readTimeout = 8000
            if (conexion.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Error ${conexion.responseCode}")
            }
            return conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }
}
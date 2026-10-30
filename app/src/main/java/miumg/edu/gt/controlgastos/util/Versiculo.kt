package miumg.edu.gt.controlgastos.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.core.text.HtmlCompat
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.random.Random

// Lo que se muestra en la tarjeta
data class Versiculo(
    val referencia: String,
    val texto: String,
    val version: String
)

object ServicioVersiculo {

    // Versión de la Biblia. Si quieres otra, cambia solo este código.
    private const val VERSION = "RV1960"

    // Se cambió el nombre para empezar con un guardado limpio
    private const val ARCHIVO = "cache_versiculo_v2"

    // Tamaño permitido del versículo (en letras) y cuántos capítulos probar como máximo
    private const val MIN_LETRAS = 50
    private const val MAX_LETRAS = 260
    private const val INTENTOS = 6

    private val hilo = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())

    // Libros de donde se eligen los versículos: número, nombre y cantidad de capítulos
    private data class Libro(val id: Int, val nombre: String, val capitulos: Int)

    private val libros = listOf(
        Libro(19, "Salmo", 150),
        Libro(20, "Proverbios", 31),
        Libro(21, "Eclesiastés", 12),
        Libro(23, "Isaías", 66),
        Libro(40, "Mateo", 28),
        Libro(41, "Marcos", 16),
        Libro(42, "Lucas", 24),
        Libro(43, "Juan", 21),
        Libro(44, "Hechos", 28),
        Libro(45, "Romanos", 16),
        Libro(46, "1 Corintios", 16),
        Libro(47, "2 Corintios", 13),
        Libro(48, "Gálatas", 6),
        Libro(49, "Efesios", 6),
        Libro(50, "Filipenses", 4),
        Libro(51, "Colosenses", 4),
        Libro(52, "1 Tesalonicenses", 5),
        Libro(53, "2 Tesalonicenses", 3),
        Libro(54, "1 Timoteo", 6),
        Libro(55, "2 Timoteo", 4),
        Libro(56, "Tito", 3),
        Libro(57, "Filemón", 1),
        Libro(58, "Hebreos", 13),
        Libro(59, "Santiago", 5),
        Libro(60, "1 Pedro", 5),
        Libro(61, "2 Pedro", 3),
        Libro(62, "1 Juan", 5),
        Libro(63, "2 Juan", 1),
        Libro(64, "3 Juan", 1),
        Libro(65, "Judas", 1)
    )

    // El versículo que se eligió: una clave única, cómo se cita y su texto
    private data class Elegido(val clave: String, val referencia: String, val texto: String)

    // "alTerminar" se llama primero con el último versículo guardado (puede ser null)
    // y, si el guardado no es de hoy, otra vez con el nuevo. Siempre en el hilo principal.
    fun cargar(c: Context, alTerminar: (Versiculo?) -> Unit) {
        val app = c.applicationContext
        val p = app.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        val dia = LocalDate.now().toEpochDay()

        // Se muestra enseguida lo último que se tenía
        alTerminar(guardado(p))
        if (esDeHoy(p, dia)) return

        hilo.execute {
            // Otro pedido pudo terminar antes: no se elige otro versículo
            if (esDeHoy(p, dia)) {
                principal.post { alTerminar(guardado(p)) }
                return@execute
            }

            val nuevo = try {
                elegir(p)
            } catch (e: Exception) {
                null   // sin internet o error: se queda lo anterior
            }

            if (nuevo != null) {
                // Se apunta el versículo para que no vuelva a salir
                val vistos = HashSet<String>(p.getStringSet("vistos", emptySet()) ?: emptySet())
                vistos.add(nuevo.clave)

                p.edit()
                    .putLong("dia", dia)
                    .putString("referencia", nuevo.referencia)
                    .putString("texto", nuevo.texto)
                    .putString("version", VERSION)
                    .putStringSet("vistos", vistos)
                    .apply()

                principal.post { alTerminar(Versiculo(nuevo.referencia, nuevo.texto, VERSION)) }
            }
        }
    }

    private fun guardado(p: SharedPreferences): Versiculo? {
        val texto = p.getString("texto", null) ?: return null
        val referencia = p.getString("referencia", null) ?: return null
        return Versiculo(referencia, texto, p.getString("version", VERSION) ?: VERSION)
    }

    private fun esDeHoy(p: SharedPreferences, dia: Long): Boolean =
        p.contains("texto") &&
                p.getLong("dia", -1L) == dia &&
                p.getString("version", null) == VERSION

    // ---------- Elegir un versículo nuevo ----------
    private fun elegir(p: SharedPreferences): Elegido? {
        val vistos = p.getStringSet("vistos", emptySet()) ?: emptySet()

        repeat(INTENTOS) {
            val libro = libros.random()
            val capitulo = Random.nextInt(1, libro.capitulos + 1)

            // De ese capítulo, solo los que sirven y no se han mostrado antes
            val candidatos = versiculosDe(libro, capitulo).filter { (numero, texto) ->
                "${libro.id}:$capitulo:$numero" !in vistos && sirve(texto)
            }

            if (candidatos.isNotEmpty()) {
                val (numero, texto) = candidatos.random()
                return Elegido(
                    clave = "${libro.id}:$capitulo:$numero",
                    referencia = "${libro.nombre} $capitulo:$numero",
                    texto = texto
                )
            }
        }
        return null   // no hubo suerte: se vuelve a intentar la próxima vez
    }

    // Un versículo sirve si tiene buen tamaño, cierra su idea y no es una lista de familias
    private fun sirve(texto: String): Boolean {
        if (texto.length !in MIN_LETRAS..MAX_LETRAS) return false
        if (texto.contains("engendr", ignoreCase = true)) return false
        return texto.last() in ".!?"
    }

    // Pide el capítulo completo y devuelve (número de versículo, texto limpio)
    private fun versiculosDe(libro: Libro, capitulo: Int): List<Pair<Int, String>> {
        val direccion = "https://bolls.life/get-text/$VERSION/${libro.id}/$capitulo/"
        val arreglo = JSONArray(descargar(direccion).trim())

        val lista = mutableListOf<Pair<Int, String>>()
        for (i in 0 until arreglo.length()) {
            val o = arreglo.getJSONObject(i)
            lista.add(o.getInt("verse") to limpiar(o.getString("text")))
        }
        return lista
    }

    // El texto llega en HTML: se quitan marcas extra y etiquetas
    private fun limpiar(html: String): String {
        val sinMarcas = html
            .replace(Regex("<S>.*?</S>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<sup>.*?</sup>", RegexOption.IGNORE_CASE), "")
        return HtmlCompat.fromHtml(sinMarcas, HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

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
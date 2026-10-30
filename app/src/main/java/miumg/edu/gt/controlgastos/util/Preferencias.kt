package miumg.edu.gt.controlgastos.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Preferencias {

    private const val ARCHIVO = "ajustes_control_gastos"
    private const val NOMBRE = "nombre"
    private const val MODO_TEMA = "modo_tema"
    private const val DIEZMO = "diezmo_activo"
    private const val CIUDAD = "ciudad_nombre"
    private const val CIUDAD_LAT = "ciudad_lat"
    private const val CIUDAD_LON = "ciudad_lon"

    private fun prefs(c: Context) =
        c.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    // ---------- Nombre ----------
    fun nombre(c: Context): String = prefs(c).getString(NOMBRE, "") ?: ""

    fun guardarNombre(c: Context, valor: String) {
        prefs(c).edit().putString(NOMBRE, valor).apply()
    }

    // ---------- Tema ----------
    fun modoTema(c: Context): Int =
        prefs(c).getInt(MODO_TEMA, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun guardarModoTema(c: Context, modo: Int) {
        prefs(c).edit().putInt(MODO_TEMA, modo).apply()
        AppCompatDelegate.setDefaultNightMode(modo)
    }

    // ---------- Diezmo ----------
    fun diezmoActivo(c: Context): Boolean = prefs(c).getBoolean(DIEZMO, true)

    fun guardarDiezmo(c: Context, activo: Boolean) {
        prefs(c).edit().putBoolean(DIEZMO, activo).apply()
    }

    // ---------- Ciudad del clima ----------
    fun ciudad(c: Context): String =
        prefs(c).getString(CIUDAD, "Ciudad de Guatemala") ?: "Ciudad de Guatemala"

    fun latitud(c: Context): Double =
        prefs(c).getString(CIUDAD_LAT, null)?.toDoubleOrNull() ?: 14.6349

    fun longitud(c: Context): Double =
        prefs(c).getString(CIUDAD_LON, null)?.toDoubleOrNull() ?: -90.5069

    fun guardarCiudad(c: Context, nombre: String, latitud: Double, longitud: Double) {
        prefs(c).edit()
            .putString(CIUDAD, nombre)
            .putString(CIUDAD_LAT, latitud.toString())
            .putString(CIUDAD_LON, longitud.toString())
            .apply()
    }
}
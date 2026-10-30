package miumg.edu.gt.controlgastos.util

object Diezmo {

    const val PORCENTAJE = 0.10

    // Cuánto se aparta de unos ingresos
    fun monto(ingresos: Double, activo: Boolean): Double =
        if (activo) ingresos * PORCENTAJE else 0.0

    // Lo que realmente puedes usar de esos ingresos
    fun neto(ingresos: Double, activo: Boolean): Double =
        ingresos - monto(ingresos, activo)
}
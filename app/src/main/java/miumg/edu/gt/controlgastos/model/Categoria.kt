package miumg.edu.gt.controlgastos.model

enum class Categoria(val nombre: String, val tipo: TipoMovimiento) {
    // Ingresos
    SALARIO("Salario", TipoMovimiento.INGRESO),
    NEGOCIO("Negocio", TipoMovimiento.INGRESO),
    VENTAS("Ventas", TipoMovimiento.INGRESO),
    HONORARIOS("Honorarios", TipoMovimiento.INGRESO),
    BONO("Bonos", TipoMovimiento.INGRESO),
    INVERSION("Inversiones", TipoMovimiento.INGRESO),
    REGALO("Regalo", TipoMovimiento.INGRESO),
    OTRO_INGRESO("Otros ingresos", TipoMovimiento.INGRESO),

    // Egresos
    COMIDA("Comida", TipoMovimiento.EGRESO),
    SUPERMERCADO("Supermercado", TipoMovimiento.EGRESO),
    TRANSPORTE("Transporte", TipoMovimiento.EGRESO),
    COMBUSTIBLE("Combustible", TipoMovimiento.EGRESO),
    VIVIENDA("Vivienda", TipoMovimiento.EGRESO),
    SERVICIOS("Servicios", TipoMovimiento.EGRESO),
    SALUD("Salud", TipoMovimiento.EGRESO),
    EDUCACION("Educación", TipoMovimiento.EGRESO),
    ENTRETENIMIENTO("Entretenimiento", TipoMovimiento.EGRESO),
    ROPA("Ropa y calzado", TipoMovimiento.EGRESO),
    COMPRAS("Compras", TipoMovimiento.EGRESO),
    MASCOTAS("Mascotas", TipoMovimiento.EGRESO),
    DONACIONES("Ofrendas y donaciones", TipoMovimiento.EGRESO),
    SUSCRIPCIONES("Suscripciones", TipoMovimiento.EGRESO),
    DEUDAS("Deudas y préstamos", TipoMovimiento.EGRESO),
    OTRO_EGRESO("Otros gastos", TipoMovimiento.EGRESO);

    companion object {
        fun porTipo(tipo: TipoMovimiento): List<Categoria> =
            values().filter { it.tipo == tipo }

        // Los registros viejos guardaron el nombre interno (COMIDA);
        // los nuevos guardan el nombre visible (Comida).
        // Esta función convierte cualquiera de los dos en el nombre visible.
        fun nombreVisible(guardado: String): String =
            values().firstOrNull { it.name == guardado }?.nombre ?: guardado
    }
}
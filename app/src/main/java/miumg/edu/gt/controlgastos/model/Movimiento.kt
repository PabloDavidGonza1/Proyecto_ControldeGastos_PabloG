package miumg.edu.gt.controlgastos.model

data class Movimiento(
    val id: Long = 0,
    val tipo: TipoMovimiento,
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val fecha: Long
)
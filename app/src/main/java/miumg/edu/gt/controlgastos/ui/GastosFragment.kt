package miumg.edu.gt.controlgastos.ui

import miumg.edu.gt.controlgastos.model.TipoMovimiento

class GastosFragment : ListaMovimientosFragment() {
    override val tipo = TipoMovimiento.EGRESO
}
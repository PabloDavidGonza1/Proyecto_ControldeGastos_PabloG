package miumg.edu.gt.controlgastos.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.databinding.ItemMovimientoBinding
import miumg.edu.gt.controlgastos.model.Movimiento
import miumg.edu.gt.controlgastos.model.TipoMovimiento
import miumg.edu.gt.controlgastos.util.Formato

class MovimientoAdapter(
    private val alTocar: (Movimiento) -> Unit = {}
) : RecyclerView.Adapter<MovimientoAdapter.VH>() {

    private var items: List<Movimiento> = emptyList()

    fun submit(lista: List<Movimiento>) {
        items = lista
        notifyDataSetChanged()
    }

    class VH(val b: ItemMovimientoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemMovimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val ctx = holder.itemView.context
        val esIngreso = m.tipo == TipoMovimiento.INGRESO

        // Toda la fila abre la edición de este movimiento
        holder.b.root.setOnClickListener { alTocar(m) }

        val color = ContextCompat.getColor(ctx, if (esIngreso) R.color.income else R.color.expense)
        val suave = ContextCompat.getColor(ctx, if (esIngreso) R.color.income_soft else R.color.expense_soft)

        holder.b.tvIcono.text = if (esIngreso) "↑" else "↓"
        holder.b.tvIcono.setTextColor(color)
        holder.b.tvIcono.backgroundTintList = ColorStateList.valueOf(suave)

        holder.b.tvCategoria.text = m.categoria
        holder.b.tvDetalle.text =
            if (m.descripcion.isBlank()) Formato.fecha(m.fecha)
            else "${m.descripcion} · ${Formato.fecha(m.fecha)}"

        holder.b.tvMonto.text = (if (esIngreso) "+ " else "- ") + Formato.moneda(m.monto)
        holder.b.tvMonto.setTextColor(color)
    }
}
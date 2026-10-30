package miumg.edu.gt.controlgastos.util

import android.view.View
import androidx.core.content.ContextCompat
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.databinding.BloqueDisponibleBinding

object DisponibleMes {

    fun pintar(
        b: BloqueDisponibleBinding,
        anterior: Double,
        balanceMes: Double,
        etiqueta: String
    ) {
        val ctx = b.root.context
        val total = anterior + balanceMes

        b.tvDispEtiqueta.text = etiqueta
        b.tvDispTotal.text = Formato.moneda(total)
        b.tvDispTotal.setTextColor(
            ContextCompat.getColor(ctx, if (total < 0) R.color.expense else R.color.income)
        )
        b.tvDispAnterior.text = Formato.moneda(anterior)
        b.tvDispMes.text = Formato.moneda(balanceMes)

        val nota = when {
            total < 0 -> "Estás en negativo: has gastado más de lo que has ingresado."
            anterior < 0 -> "Arrastras un saldo negativo de meses anteriores."
            else -> ""
        }
        b.tvDispNota.text = nota
        b.tvDispNota.visibility = if (nota.isEmpty()) View.GONE else View.VISIBLE
    }
}
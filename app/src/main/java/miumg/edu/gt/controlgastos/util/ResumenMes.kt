package miumg.edu.gt.controlgastos.util

import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.databinding.BloqueResumenBinding
import kotlin.math.roundToInt

object ResumenMes {

    fun pintar(b: BloqueResumenBinding, ingresos: Double, gastos: Double, usarDiezmo: Boolean) {
        val ctx = b.root.context
        val diezmo = Diezmo.monto(ingresos, usarDiezmo)
        val queda = ingresos - diezmo - gastos
        val sinDatos = ingresos == 0.0 && gastos == 0.0
        val pct = if (ingresos > 0) (gastos / ingresos * 100).roundToInt() else 0

        // Frase corta con color según cómo vas
        val (colorId, mensaje) = when {
            sinDatos ->
                R.color.text_secondary to "Aún no hay movimientos en este mes."
            ingresos == 0.0 ->
                R.color.expense to "Hay gastos, pero no hay ingresos registrados."
            queda < 0 ->
                R.color.expense to "Gastaste más de lo que tenías disponible este mes."
            queda <= ingresos * 0.15 ->
                R.color.warning to "Ojo: has gastado el $pct% de tus ingresos y te queda poco."
            else ->
                R.color.income to "¡Vas bien! Has gastado el $pct% de tus ingresos."
        }
        b.tvSub.text = mensaje
        b.tvSub.setTextColor(ContextCompat.getColor(ctx, colorId))

        // Nota pequeña bajo la barra
        b.tvNotaBarra.text =
            if (ingresos > 0) "La barra completa son tus ingresos del mes: ${Formato.moneda(ingresos)}"
            else "La barra se llena cuando registres ingresos."

        // Barra única: cada parte ocupa el espacio de su peso
        val total = if (queda >= 0) ingresos else diezmo + gastos
        if (total > 0.0) {
            peso(b.vDiezmo, diezmo)
            peso(b.vGastos, gastos)
            peso(b.vLibre, maxOf(queda, 0.0))
        } else {
            peso(b.vDiezmo, 0.0)
            peso(b.vGastos, 0.0)
            peso(b.vLibre, 0.0)
        }

        // Leyenda (suma = ingresos del mes)
        b.filaDiezmo.visibility = if (usarDiezmo) View.VISIBLE else View.GONE
        b.tvLegDiezmo.text = Formato.moneda(diezmo)
        b.tvLegGastos.text = Formato.moneda(gastos)
        if (queda >= 0) {
            b.tvLegLibreNombre.text = "Te queda de este mes"
            b.tvLegLibre.text = Formato.moneda(queda)
        } else {
            b.tvLegLibreNombre.text = "Te pasaste por"
            b.tvLegLibre.text = Formato.moneda(-queda)
        }
    }

    private fun peso(v: View, valor: Double) {
        val lp = v.layoutParams as LinearLayout.LayoutParams
        lp.weight = valor.toFloat()
        v.layoutParams = lp
    }
}
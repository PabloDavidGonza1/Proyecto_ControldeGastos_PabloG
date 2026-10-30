package miumg.edu.gt.controlgastos.util

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.databinding.ItemCategoriaBinding
import kotlin.math.roundToInt

object ListaCategorias {

    private val paleta = listOf(
        "#3B82F6", "#F59E0B", "#EC4899", "#06B6D4", "#8B5CF6",
        "#84CC16", "#F97316", "#64748B", "#14B8A6", "#A855F7"
    ).map { Color.parseColor(it) }

    fun pintar(
        contenedor: LinearLayout,
        datos: Map<String, Double>,
        textoVacio: String,
        maximo: Int = 10
    ) {
        contenedor.removeAllViews()
        val ctx = contenedor.context

        if (datos.isEmpty()) {
            val dp = ctx.resources.displayMetrics.density
            val aviso = TextView(ctx).apply {
                text = textoVacio
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
            }
            contenedor.addView(aviso)
            return
        }

        val total = datos.values.sum()
        val inflater = LayoutInflater.from(ctx)

        datos.entries
            .sortedByDescending { it.value }
            .take(maximo)
            .forEachIndexed { i, e ->
                val fila = ItemCategoriaBinding.inflate(inflater, contenedor, false)
                val color = paleta[i % paleta.size]
                val pct = e.value / total * 100

                fila.vColor.backgroundTintList = ColorStateList.valueOf(color)
                fila.tvNombre.text = e.key
                fila.tvValor.text = "${Formato.moneda(e.value)} · ${pct.roundToInt()}%"
                fila.pbCategoria.setIndicatorColor(color)
                fila.pbCategoria.setProgressCompat(pct.roundToInt(), false)
                contenedor.addView(fila.root)
            }
    }
}
package miumg.edu.gt.controlgastos.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

object Bordes {
    fun superior(vista: View) {
        val base = vista.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(vista) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = base + barras.top)
            insets
        }
        ViewCompat.requestApplyInsets(vista)
    }
}
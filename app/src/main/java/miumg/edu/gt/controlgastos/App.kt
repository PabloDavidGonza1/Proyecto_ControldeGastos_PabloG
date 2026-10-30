package miumg.edu.gt.controlgastos

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import miumg.edu.gt.controlgastos.util.Preferencias

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Preferencias.modoTema(this))
    }
}
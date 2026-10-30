package miumg.edu.gt.controlgastos.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.databinding.ActivityMainBinding
import miumg.edu.gt.controlgastos.databinding.DialogNombreBinding
import miumg.edu.gt.controlgastos.util.Preferencias

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pantalla inicial (solo la primera vez; luego Android la restaura)
        if (savedInstanceState == null) mostrar(InicioFragment())

        binding.bottomNav.setOnItemSelectedListener { opcion ->
            val fragmento: Fragment = when (opcion.itemId) {
                R.id.nav_ingresos -> IngresosFragment()
                R.id.nav_gastos -> GastosFragment()
                R.id.nav_ajustes -> AjustesFragment()
                else -> InicioFragment()
            }
            mostrar(fragmento)
            true
        }

        if (Preferencias.nombre(this).isBlank()) pedirNombre()
    }

    private fun mostrar(fragmento: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.contenedor, fragmento)
            .commit()
    }

    // Cuadro de bienvenida que pregunta el nombre (primera vez)
    private fun pedirNombre() {
        val vista = DialogNombreBinding.inflate(layoutInflater)

        val dialogo: AlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle("¡Bienvenido!")
            .setMessage("Para personalizar tu experiencia, ¿cómo te llamas?")
            .setView(vista.root)
            .setCancelable(false)
            .setPositiveButton("Continuar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = vista.etNombre.text.toString().trim()
                if (nombre.isEmpty()) {
                    vista.tilNombre.error = "Escribe tu nombre"
                } else {
                    Preferencias.guardarNombre(this, nombre)
                    dialogo.dismiss()
                    (supportFragmentManager.findFragmentById(R.id.contenedor) as? InicioFragment)
                        ?.actualizarSaludo()
                }
            }
        }
        dialogo.show()
    }
}

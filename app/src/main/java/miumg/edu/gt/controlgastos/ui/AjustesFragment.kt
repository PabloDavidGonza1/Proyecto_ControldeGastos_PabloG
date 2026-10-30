package miumg.edu.gt.controlgastos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.databinding.FragmentAjustesBinding
import miumg.edu.gt.controlgastos.util.Bordes
import miumg.edu.gt.controlgastos.util.Lugar
import miumg.edu.gt.controlgastos.util.Preferencias
import miumg.edu.gt.controlgastos.util.ServicioClima

class AjustesFragment : Fragment() {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        Bordes.superior(binding.header)

        // --- Nombre ---
        binding.etNombre.setText(Preferencias.nombre(ctx))
        binding.btnGuardarNombre.setOnClickListener {
            val nombre = binding.etNombre.text.toString().trim()
            if (nombre.isEmpty()) {
                binding.tilNombre.error = "Escribe tu nombre"
                return@setOnClickListener
            }
            binding.tilNombre.error = null
            Preferencias.guardarNombre(ctx, nombre)
            Toast.makeText(ctx, "Nombre actualizado", Toast.LENGTH_SHORT).show()
        }

        // --- Tema ---
        val seleccionado = when (Preferencias.modoTema(ctx)) {
            AppCompatDelegate.MODE_NIGHT_NO -> R.id.btnTemaClaro
            AppCompatDelegate.MODE_NIGHT_YES -> R.id.btnTemaOscuro
            else -> R.id.btnTemaSistema
        }
        binding.toggleTema.check(seleccionado)
        binding.toggleTema.addOnButtonCheckedListener { _, id, marcado ->
            if (!marcado) return@addOnButtonCheckedListener
            val modo = when (id) {
                R.id.btnTemaClaro -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.btnTemaOscuro -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (modo != Preferencias.modoTema(ctx)) Preferencias.guardarModoTema(ctx, modo)
        }

        // --- Clima: ciudad ---
        binding.tvCiudadActual.text = "Ciudad actual: ${Preferencias.ciudad(ctx)}"
        binding.btnBuscarCiudad.setOnClickListener {
            val texto = binding.etBuscarCiudad.text.toString().trim()
            if (texto.length < 2) {
                binding.tilBuscarCiudad.error = "Escribe al menos 2 letras"
                return@setOnClickListener
            }
            binding.tilBuscarCiudad.error = null
            binding.btnBuscarCiudad.isEnabled = false

            ServicioClima.buscarLugares(texto) { lugares ->
                val b = _binding ?: return@buscarLugares
                b.btnBuscarCiudad.isEnabled = true
                when {
                    lugares == null -> Toast.makeText(
                        ctx, "No se pudo buscar. Revisa tu conexión a internet.",
                        Toast.LENGTH_LONG
                    ).show()
                    lugares.isEmpty() -> Toast.makeText(
                        ctx, "No encontré esa ciudad. Prueba con otro nombre.",
                        Toast.LENGTH_LONG
                    ).show()
                    else -> mostrarLugares(lugares)
                }
            }
        }

        // --- Diezmo ---
        binding.swDiezmo.isChecked = Preferencias.diezmoActivo(ctx)
        binding.swDiezmo.setOnCheckedChangeListener { _, activo ->
            Preferencias.guardarDiezmo(ctx, activo)
        }
    }

    // Lista para que el usuario elija su ciudad entre las encontradas
    private fun mostrarLugares(lugares: List<Lugar>) {
        val ctx = context ?: return
        val etiquetas = lugares.map {
            if (it.detalle.isBlank()) it.nombre else "${it.nombre} (${it.detalle})"
        }.toTypedArray()

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Elige tu ciudad")
            .setItems(etiquetas) { _, i ->
                val lugar = lugares[i]
                Preferencias.guardarCiudad(ctx, lugar.nombre, lugar.latitud, lugar.longitud)
                ServicioClima.borrarCache(ctx)   // para no mostrar el clima de la ciudad anterior
                _binding?.tvCiudadActual?.text = "Ciudad actual: ${lugar.nombre}"
                _binding?.etBuscarCiudad?.setText("")
                Toast.makeText(ctx, "Ciudad actualizada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
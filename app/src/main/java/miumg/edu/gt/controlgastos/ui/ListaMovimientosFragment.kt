package miumg.edu.gt.controlgastos.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.data.DatabaseHelper
import miumg.edu.gt.controlgastos.databinding.FragmentListaBinding
import miumg.edu.gt.controlgastos.model.TipoMovimiento
import miumg.edu.gt.controlgastos.util.Bordes
import miumg.edu.gt.controlgastos.util.Formato

abstract class ListaMovimientosFragment : Fragment() {

    protected abstract val tipo: TipoMovimiento

    private var _binding: FragmentListaBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper

    private val adapter = MovimientoAdapter { m ->
        AgregarMovimientoActivity.abrir(requireContext(), m.tipo, m.id)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper(requireContext())
        Bordes.superior(binding.header)

        val esIngreso = tipo == TipoMovimiento.INGRESO
        val color = ContextCompat.getColor(
            requireContext(), if (esIngreso) R.color.income else R.color.expense
        )

        binding.tvTitulo.text = if (esIngreso) "Mis ingresos" else "Mis gastos"
        binding.tvEtiqueta.text = if (esIngreso) "Total de ingresos" else "Total de gastos"
        binding.tvVacio.text = if (esIngreso)
            "Aún no has registrado ingresos.\nToca el botón para agregar el primero."
        else
            "Aún no has registrado gastos.\nToca el botón para agregar el primero."

        binding.fabAgregar.text = if (esIngreso) "Agregar ingreso" else "Agregar gasto"
        binding.fabAgregar.backgroundTintList = ColorStateList.valueOf(color)
        binding.fabAgregar.setTextColor(Color.WHITE)
        binding.fabAgregar.setOnClickListener {
            AgregarMovimientoActivity.abrir(requireContext(), tipo)
        }

        binding.rvLista.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLista.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    override fun onDestroyView() {
        db.close()
        _binding = null
        super.onDestroyView()
    }

    private fun cargar() {
        val lista = db.obtenerPorTipo(tipo)
        adapter.submit(lista)
        binding.tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        binding.tvTotal.text = Formato.moneda(lista.sumOf { it.monto })
        binding.tvCantidad.text = when (lista.size) {
            0 -> "Sin registros"
            1 -> "1 registro · toca para editar"
            else -> "${lista.size} registros · toca uno para editar"
        }
    }
}
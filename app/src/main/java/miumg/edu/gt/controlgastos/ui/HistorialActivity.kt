package miumg.edu.gt.controlgastos.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import miumg.edu.gt.controlgastos.data.DatabaseHelper
import miumg.edu.gt.controlgastos.databinding.ActivityHistorialBinding
import miumg.edu.gt.controlgastos.model.TipoMovimiento
import miumg.edu.gt.controlgastos.util.Diezmo
import miumg.edu.gt.controlgastos.util.DisponibleMes
import miumg.edu.gt.controlgastos.util.Formato
import miumg.edu.gt.controlgastos.util.ListaCategorias
import miumg.edu.gt.controlgastos.util.Periodo
import miumg.edu.gt.controlgastos.util.Preferencias
import miumg.edu.gt.controlgastos.util.ResumenMes
import java.time.YearMonth

class HistorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialBinding
    private lateinit var db: DatabaseHelper

    private var mes: YearMonth = Periodo.actual()

    private val adapter = MovimientoAdapter { m ->
        AgregarMovimientoActivity.abrir(this, m.tipo, m.id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        ajustarBordes()

        binding.btnVolver.setOnClickListener { finish() }

        binding.rvMovimientos.layoutManager = LinearLayoutManager(this)
        binding.rvMovimientos.adapter = adapter

        binding.selectorMes.btnMesAnterior.setOnClickListener {
            mes = mes.minusMonths(1)
            cargar()
        }
        binding.selectorMes.btnMesSiguiente.setOnClickListener {
            mes = mes.plusMonths(1)
            cargar()
        }
        binding.selectorMes.tvMes.setOnClickListener {
            mes = Periodo.actual()
            cargar()
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun ajustarBordes() {
        val headerTop = binding.header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.updatePadding(top = headerTop + bars.top)
            binding.root.updatePadding(bottom = bars.bottom)
            insets
        }
    }

    private fun cargar() {
        binding.selectorMes.tvMes.text = Periodo.nombre(mes)

        val desde = Periodo.inicio(mes)
        val hasta = Periodo.fin(mes)
        val usarDiezmo = Preferencias.diezmoActivo(this)

        val ingresos = db.totalPorTipo(TipoMovimiento.INGRESO, desde, hasta)
        val gastos = db.totalPorTipo(TipoMovimiento.EGRESO, desde, hasta)

        // Todo ya sin el diezmo
        val balanceMes = Diezmo.neto(ingresos, usarDiezmo) - gastos
        val anterior = Diezmo.neto(db.totalPorTipo(TipoMovimiento.INGRESO, 0L, desde), usarDiezmo) -
                db.totalPorTipo(TipoMovimiento.EGRESO, 0L, desde)

        binding.tvBalance.text = Formato.moneda(balanceMes)

        val etiqueta = if (mes == Periodo.actual())
            "Dinero disponible hoy"
        else
            "Disponible al cerrar ${Periodo.nombre(mes)}"
        DisponibleMes.pintar(binding.disponible, anterior, balanceMes, etiqueta)

        ResumenMes.pintar(binding.resumen, ingresos, gastos, usarDiezmo)

        binding.tvTituloIngresos.text = "Ingresos · ${Formato.moneda(ingresos)}"
        ListaCategorias.pintar(
            binding.contenedorIngresos,
            db.totalPorCategoria(TipoMovimiento.INGRESO, desde, hasta),
            "No hay ingresos en este mes."
        )

        binding.tvTituloGastos.text = "Gastos · ${Formato.moneda(gastos)}"
        ListaCategorias.pintar(
            binding.contenedorGastos,
            db.totalPorCategoria(TipoMovimiento.EGRESO, desde, hasta),
            "No hay gastos en este mes."
        )

        val movimientos = db.obtenerMovimientos(desde, hasta)
        adapter.submit(movimientos)
        binding.tvTituloMovs.text = "Todos los movimientos (${movimientos.size})"
        binding.tvVacio.visibility = if (movimientos.isEmpty()) View.VISIBLE else View.GONE
    }
}
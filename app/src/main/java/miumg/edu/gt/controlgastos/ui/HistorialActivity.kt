package miumg.edu.gt.controlgastos.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import miumg.edu.gt.controlgastos.R
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

        // Si se intenta pasar de los límites, cargar() lo regresa al último mes permitido
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

    // Primer y último mes que tiene sentido visitar:
    // desde el mes del movimiento más antiguo hasta el mes actual
    // (o hasta el del movimiento más reciente, si alguno tiene fecha futura)
    private fun limites(): Pair<YearMonth, YearMonth> {
        val actual = Periodo.actual()
        val fechas = db.obtenerMovimientos().map { it.fecha }
        val menor = fechas.minOrNull() ?: return actual to actual
        val mayor = fechas.maxOrNull() ?: return actual to actual
        return minOf(Periodo.mesDe(menor), actual) to maxOf(Periodo.mesDe(mayor), actual)
    }

    // Una flecha apagada se ve tenue y no responde
    private fun marcar(flecha: View, activa: Boolean) {
        flecha.isEnabled = activa
        flecha.alpha = if (activa) 1f else 0.3f
    }

    private fun cargar() {
        val (primero, ultimo) = limites()
        mes = mes.coerceIn(primero, ultimo)
        marcar(binding.selectorMes.btnMesAnterior, mes.isAfter(primero))
        marcar(binding.selectorMes.btnMesSiguiente, mes.isBefore(ultimo))

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

        // Plantita del mes que se está viendo
        val estado = estadoPlanta(ingresos, gastos, usarDiezmo)
        binding.planta.mostrar(estado)
        if (estado == EstadoPlanta.SEMILLA && mes != Periodo.actual()) {
            binding.tvEstadoPlanta.text = "Sin movimientos"
            binding.tvEstadoPlanta.setTextColor(
                ContextCompat.getColor(this, R.color.text_secondary)
            )
            binding.tvPlantaAyuda.text = "No hay ingresos ni gastos registrados en este mes."
        } else {
            binding.tvEstadoPlanta.text = estado.titulo
            binding.tvEstadoPlanta.setTextColor(ContextCompat.getColor(this, estado.colorId))
            binding.tvPlantaAyuda.text = estado.ayuda
        }

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
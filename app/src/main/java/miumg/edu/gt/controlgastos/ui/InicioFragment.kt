package miumg.edu.gt.controlgastos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.data.DatabaseHelper
import miumg.edu.gt.controlgastos.databinding.FragmentInicioBinding
import miumg.edu.gt.controlgastos.model.TipoMovimiento
import miumg.edu.gt.controlgastos.util.Bordes
import miumg.edu.gt.controlgastos.util.Clima
import miumg.edu.gt.controlgastos.util.Diezmo
import miumg.edu.gt.controlgastos.util.Formato
import miumg.edu.gt.controlgastos.util.ListaCategorias
import miumg.edu.gt.controlgastos.util.Periodo
import miumg.edu.gt.controlgastos.util.Preferencias
import miumg.edu.gt.controlgastos.util.ResumenMes
import miumg.edu.gt.controlgastos.util.ServicioClima
import miumg.edu.gt.controlgastos.util.ServicioVersiculo
import java.util.Calendar
import kotlin.math.roundToInt

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper

    // Al tocar un movimiento se abre la pantalla de edición
    private val adapter = MovimientoAdapter { m ->
        AgregarMovimientoActivity.abrir(requireContext(), m.tipo, m.id)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper(requireContext())
        Bordes.superior(binding.header)

        binding.rvMovimientos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMovimientos.adapter = adapter

        binding.btnIngreso.setOnClickListener {
            AgregarMovimientoActivity.abrir(requireContext(), TipoMovimiento.INGRESO)
        }
        binding.btnEgreso.setOnClickListener {
            AgregarMovimientoActivity.abrir(requireContext(), TipoMovimiento.EGRESO)
        }
        binding.btnHistorial.setOnClickListener {
            startActivity(Intent(requireContext(), HistorialActivity::class.java))
        }
    }

    // Se ejecuta al abrir y cada vez que se vuelve a esta pantalla.
    override fun onResume() {
        super.onResume()
        actualizar()
        cargarClima()
        cargarVersiculo()
    }

    override fun onDestroyView() {
        db.close()
        _binding = null
        super.onDestroyView()
    }

    // Lo llama también MainActivity después de pedir el nombre
    fun actualizarSaludo() {
        val b = _binding ?: return
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val saludo = when {
            hora < 12 -> "Buenos días,"
            hora < 19 -> "Buenas tardes,"
            else -> "Buenas noches,"
        }
        val nombre = Preferencias.nombre(requireContext())
        b.tvSaludo.text = saludo
        b.tvNombre.text = if (nombre.isBlank()) "Bienvenido" else nombre
    }

    // ---------- VERSÍCULO DEL DÍA ----------
    private fun cargarVersiculo() {
        val ctx = context ?: return
        ServicioVersiculo.cargar(ctx) { versiculo ->
            // La pantalla pudo cerrarse mientras se descargaba
            val b = _binding ?: return@cargar

            if (versiculo == null) {
                b.cardVersiculo.visibility = View.GONE
                return@cargar
            }
            b.cardVersiculo.visibility = View.VISIBLE
            b.tvVersiculo.text = "“${versiculo.texto}”"
            b.tvVersiculoRef.text = "${versiculo.referencia} (${versiculo.version})"
        }
    }

    // ---------- CLIMA ----------
    private fun cargarClima() {
        val ctx = context ?: return
        ServicioClima.cargar(ctx) { clima ->
            // La pantalla pudo cerrarse mientras se descargaba
            val b = _binding ?: return@cargar

            if (clima == null) {
                b.chipClima.visibility = View.GONE
                return@cargar
            }
            b.chipClima.visibility = View.VISIBLE
            b.ivClima.setImageResource(iconoClima(clima.codigo))
            b.tvClima.text = "${clima.temperatura.roundToInt()}°"
            b.tvClimaDesc.text = Clima.descripcionDe(clima.codigo)
            b.tvClimaDetalle.text =
                "Máx ${clima.maxima.roundToInt()}° · Mín ${clima.minima.roundToInt()}°"
            b.tvClimaLluvia.text = "Lluvia ${clima.probLluvia}% · ${clima.ciudad}"
        }
    }

    // Elige el dibujo según el código del clima y la hora del día
    private fun iconoClima(codigo: Int): Int {
        val hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val esDeNoche = hora < 6 || hora >= 18
        return when (codigo) {
            0, 1 -> if (esDeNoche) R.drawable.ic_clima_luna else R.drawable.ic_clima_sol
            2 -> if (esDeNoche) R.drawable.ic_clima_nublado else R.drawable.ic_clima_parcial
            3 -> R.drawable.ic_clima_nublado
            45, 48 -> R.drawable.ic_clima_niebla
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.ic_clima_lluvia
            71, 73, 75, 77, 85, 86 -> R.drawable.ic_clima_nieve
            95, 96, 99 -> R.drawable.ic_clima_tormenta
            else -> R.drawable.ic_clima_nublado
        }
    }

    private fun actualizar() {
        actualizarSaludo()

        val mes = Periodo.actual()
        val desde = Periodo.inicio(mes)
        val hasta = Periodo.fin(mes)
        val usarDiezmo = Preferencias.diezmoActivo(requireContext())

        val ingresos = db.totalPorTipo(TipoMovimiento.INGRESO, desde, hasta)
        val egresos = db.totalPorTipo(TipoMovimiento.EGRESO, desde, hasta)

        // Ingresos del mes ya sin el diezmo
        val ingresosNetos = Diezmo.neto(ingresos, usarDiezmo)

        // Lo que traes de meses anteriores (también sin el diezmo de esos meses)
        val anterior = Diezmo.neto(db.totalPorTipo(TipoMovimiento.INGRESO, 0L, desde), usarDiezmo) -
                db.totalPorTipo(TipoMovimiento.EGRESO, 0L, desde)

        // Encabezado: un solo número grande
        binding.tvMesActual.text = Periodo.nombre(mes)
        binding.tvDisponible.text = Formato.moneda(anterior + ingresosNetos - egresos)
        binding.tvIngresos.text = Formato.moneda(ingresos)
        binding.tvEgresos.text = Formato.moneda(egresos)

        val textoTraes = when {
            anterior > 0 -> "Incluye ${Formato.moneda(anterior)} que te sobraron de meses anteriores"
            anterior < 0 -> "Incluye ${Formato.moneda(-anterior)} que debes de meses anteriores"
            else -> ""
        }
        binding.tvTraes.text = textoTraes
        binding.tvTraes.visibility = if (textoTraes.isEmpty()) View.GONE else View.VISIBLE

        // Diezmo del mes (empieza en Q 0.00 cada mes)
        binding.cardDiezmo.visibility = if (usarDiezmo) View.VISIBLE else View.GONE
        binding.tvTituloDiezmo.text = "Diezmo de ${Periodo.nombre(mes)}"
        binding.tvDiezmo.text = Formato.moneda(Diezmo.monto(ingresos, usarDiezmo))
        binding.tvDiezmoDetalle.text =
            "El 10% de los ingresos de este mes. Ya está descontado de tu dinero disponible."

        // Barra de colores
        ResumenMes.pintar(binding.resumen, ingresos, egresos, usarDiezmo)

        // Gastos por categoría (los 5 mayores)
        ListaCategorias.pintar(
            binding.contenedorCategorias,
            db.totalPorCategoria(TipoMovimiento.EGRESO, desde, hasta),
            "Aún no has registrado gastos este mes.",
            maximo = 5
        )

        // Últimos movimientos del mes
        val recientes = db.obtenerMovimientos(desde, hasta).take(5)
        adapter.submit(recientes)
        binding.tvVacio.text =
            "No hay movimientos en ${Periodo.nombre(mes)}.\nAgrega uno con los botones de arriba."
        binding.tvVacio.visibility = if (recientes.isEmpty()) View.VISIBLE else View.GONE
    }
}
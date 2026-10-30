package miumg.edu.gt.controlgastos.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.data.DatabaseHelper
import miumg.edu.gt.controlgastos.databinding.ActivityAgregarMovimientoBinding
import miumg.edu.gt.controlgastos.databinding.DialogNombreBinding
import miumg.edu.gt.controlgastos.model.Categoria
import miumg.edu.gt.controlgastos.model.Movimiento
import miumg.edu.gt.controlgastos.model.TipoMovimiento
import miumg.edu.gt.controlgastos.util.Diezmo
import miumg.edu.gt.controlgastos.util.Formato
import miumg.edu.gt.controlgastos.util.Periodo
import miumg.edu.gt.controlgastos.util.Preferencias
import java.util.Locale
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward

abstract class AgregarMovimientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "movimiento_id"
        private const val TEXTO_NUEVA = "➕  Nueva categoría…"

        // Abre la pantalla para crear (id = -1) o para editar (id del registro)
        fun abrir(contexto: Context, tipo: TipoMovimiento, id: Long = -1L) {
            val clase = if (tipo == TipoMovimiento.INGRESO)
                AgregarIngresoActivity::class.java
            else
                AgregarEgresoActivity::class.java
            contexto.startActivity(Intent(contexto, clase).putExtra(EXTRA_ID, id))
        }
    }

    protected abstract val tipo: TipoMovimiento

    private lateinit var binding: ActivityAgregarMovimientoBinding
    private lateinit var db: DatabaseHelper

    // Categorías disponibles (las de fábrica + las que creó el usuario)
    private var opciones: List<String> = emptyList()
    private var categoriaSeleccionada: String? = null
    private var fechaSeleccionada: Long = System.currentTimeMillis()
    private var movimientoEditado: Movimiento? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        binding = ActivityAgregarMovimientoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id != -1L) movimientoEditado = db.obtenerPorId(id)

        ajustarBordes()
        configurarTextos()
        configurarCategorias()
        cargarSiEdita()
        configurarFecha()

        binding.btnVolver.setOnClickListener { finish() }
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnEliminar.setOnClickListener { confirmarEliminar() }
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun ajustarBordes() {
        val headerTop = binding.header.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val teclado = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.header.updatePadding(top = headerTop + bars.top)
            binding.root.updatePadding(bottom = maxOf(bars.bottom, teclado.bottom))
            insets
        }
    }

    private fun configurarTextos() {
        val esIngreso = tipo == TipoMovimiento.INGRESO
        val editando = movimientoEditado != null
        val color = ContextCompat.getColor(
            this, if (esIngreso) R.color.income else R.color.expense
        )
        val palabra = if (esIngreso) "ingreso" else "egreso"

        binding.tvTitulo.text = if (editando) "Editar $palabra" else "Nuevo $palabra"
        binding.tvSubtitulo.text = when {
            editando -> "Modifica los datos y guarda los cambios"
            esIngreso -> "Registra el dinero que recibes"
            else -> "Registra el dinero que gastas"
        }
        binding.btnGuardar.text = if (editando) "Guardar cambios" else "Guardar $palabra"
        binding.btnGuardar.backgroundTintList = ColorStateList.valueOf(color)
    }

    // ---------- CATEGORÍAS ----------
    private fun cargarOpciones() {
        opciones = Categoria.porTipo(tipo).map { it.nombre } + db.categoriasPersonalizadas(tipo)
        binding.actvCategoria.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                opciones + TEXTO_NUEVA
            )
        )
        binding.actvCategoria.setText(categoriaSeleccionada ?: "", false)
    }

    private fun configurarCategorias() {
        cargarOpciones()
        binding.actvCategoria.setOnItemClickListener { _, _, posicion, _ ->
            if (posicion == opciones.size) {
                // Eligió "Nueva categoría": se deja lo que había y se abre el cuadro
                binding.actvCategoria.setText(categoriaSeleccionada ?: "", false)
                pedirNuevaCategoria()
            } else {
                categoriaSeleccionada = opciones[posicion]
                binding.tilCategoria.error = null
            }
        }
    }

    private fun pedirNuevaCategoria() {
        val vista = DialogNombreBinding.inflate(layoutInflater)
        vista.tilNombre.hint = "Nombre de la categoría"

        val titulo = if (tipo == TipoMovimiento.INGRESO)
            "Nueva categoría de ingreso" else "Nueva categoría de gasto"

        val dialogo: AlertDialog = MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setView(vista.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Agregar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = vista.etNombre.text.toString().trim()
                    .replaceFirstChar { it.uppercase() }

                if (nombre.isEmpty()) {
                    vista.tilNombre.error = "Escribe un nombre"
                    return@setOnClickListener
                }

                val existente = opciones.firstOrNull { it.equals(nombre, ignoreCase = true) }
                if (existente != null) {
                    categoriaSeleccionada = existente
                    Toast.makeText(this, "Esa categoría ya existe, la seleccioné", Toast.LENGTH_SHORT).show()
                } else {
                    db.insertarCategoria(nombre, tipo)
                    categoriaSeleccionada = nombre
                }

                binding.tilCategoria.error = null
                cargarOpciones()
                dialogo.dismiss()
            }
        }
        dialogo.show()
    }

    // Si estamos editando, rellena los campos con los datos guardados
    private fun cargarSiEdita() {
        val m = movimientoEditado ?: return
        binding.etMonto.setText(String.format(Locale.US, "%.2f", m.monto))
        categoriaSeleccionada = m.categoria
        binding.actvCategoria.setText(m.categoria, false)
        fechaSeleccionada = m.fecha
        binding.etDescripcion.setText(m.descripcion)
        binding.btnEliminar.visibility = View.VISIBLE
    }

    // ---------- FECHA ----------
    private fun configurarFecha() {
        mostrarFecha()
        binding.etFecha.setOnClickListener {
            // Solo se pueden elegir días hasta hoy
            val restricciones = CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now())
                .build()

            val selector = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona la fecha")
                .setSelection(Formato.localAUtc(fechaSeleccionada))
                .setCalendarConstraints(restricciones)
                .build()
            selector.addOnPositiveButtonClickListener { utc ->
                fechaSeleccionada = Formato.utcALocal(utc)
                mostrarFecha()
            }
            selector.show(supportFragmentManager, "selector_fecha")
        }
    }

    private fun mostrarFecha() {
        binding.etFecha.setText(Formato.fecha(fechaSeleccionada))
    }

    // ---------- GUARDAR ----------
    private fun guardar() {
        val monto = binding.etMonto.text.toString()
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()

        if (monto == null || monto <= 0.0) {
            binding.tilMonto.error = "Ingresa un monto mayor a 0"
            return
        }
        binding.tilMonto.error = null

        val categoria = categoriaSeleccionada ?: run {
            binding.tilCategoria.error = "Selecciona una categoría"
            return
        }

        val movimiento = Movimiento(
            id = movimientoEditado?.id ?: 0L,
            tipo = tipo,
            monto = monto,
            categoria = categoria,
            descripcion = binding.etDescripcion.text.toString().trim(),
            fecha = fechaSeleccionada
        )

        // Aviso: ¿el gasto supera el dinero disponible (ya sin el diezmo)?
        if (tipo == TipoMovimiento.EGRESO) {
            val usarDiezmo = Preferencias.diezmoActivo(this)
            val mes = Periodo.mesDe(fechaSeleccionada)
            val hasta = Periodo.fin(mes)

            val ingresos = Diezmo.neto(
                db.totalPorTipo(TipoMovimiento.INGRESO, 0L, hasta), usarDiezmo
            )
            val egresos = db.totalPorTipo(TipoMovimiento.EGRESO, 0L, hasta)

            // Si editas un gasto, su monto anterior se "devuelve" antes de comparar
            val devuelto = movimientoEditado
                ?.takeIf { it.tipo == TipoMovimiento.EGRESO && it.fecha < hasta }
                ?.monto ?: 0.0

            val disponible = ingresos - (egresos - devuelto)

            if (monto > disponible) {
                mostrarAdvertencia(movimiento, disponible, Periodo.nombre(mes), usarDiezmo)
                return
            }
        }

        persistir(movimiento)
    }

    private fun mostrarAdvertencia(
        movimiento: Movimiento,
        disponible: Double,
        nombreMes: String,
        conDiezmo: Boolean
    ) {
        val aclaracion = if (conDiezmo) " (ya sin el diezmo)" else ""
        MaterialAlertDialogBuilder(this)
            .setTitle("Estás gastando de más")
            .setMessage(
                "Este gasto (${Formato.moneda(movimiento.monto)}) es mayor al dinero " +
                        "que tienes disponible hasta $nombreMes " +
                        "(${Formato.moneda(maxOf(disponible, 0.0))})$aclaracion, contando lo " +
                        "que traes de meses anteriores.\n\n" +
                        "¿Deseas registrarlo de todas formas?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, registrar") { _, _ -> persistir(movimiento) }
            .show()
    }

    private fun persistir(movimiento: Movimiento) {
        val exito = if (movimientoEditado != null)
            db.actualizarMovimiento(movimiento) > 0
        else
            db.insertarMovimiento(movimiento) != -1L

        if (exito) {
            val mesGuardado = Periodo.mesDe(movimiento.fecha)
            val mensaje = if (mesGuardado == Periodo.actual())
                "Guardado correctamente"
            else
                "Guardado en ${Periodo.nombre(mesGuardado)}. Búscalo en el historial."
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "No se pudo guardar. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarEliminar() {
        val m = movimientoEditado ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Seguro que quieres eliminarlo? Esta acción no se puede deshacer.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                db.eliminarMovimiento(m.id)
                Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
            .show()
    }
}
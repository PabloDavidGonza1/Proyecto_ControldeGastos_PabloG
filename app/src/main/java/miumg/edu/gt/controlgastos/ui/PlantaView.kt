package miumg.edu.gt.controlgastos.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import miumg.edu.gt.controlgastos.R
import miumg.edu.gt.controlgastos.util.Diezmo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Los seis estados de la plantita.
// "destino" son los valores a los que se acerca, en este orden:
// altura, tamaño de hojas, verde (0 = café, 1 = verde), caída, flores y ánimo (-1 triste, 1 feliz)
enum class EstadoPlanta(
    val titulo: String,
    val ayuda: String,
    val colorId: Int,
    val destino: FloatArray,
    val durmiendo: Boolean = false
) {
    SEMILLA(
        "Esperando ingresos",
        "Registra tu primer ingreso para sembrar tu plantita.",
        R.color.text_secondary,
        floatArrayOf(0f, 0f, 1f, 0f, 0f, 0.3f),
        durmiendo = true
    ),
    FLORES(
        "Floreciendo",
        "Te queda más del 70% de tus ingresos del mes. ¡Sigue así!",
        R.color.income,
        floatArrayOf(1f, 1f, 1f, 0f, 1f, 1f)
    ),
    FRONDOSA(
        "Creciendo fuerte",
        "Te queda entre el 40% y el 70% de tus ingresos del mes.",
        R.color.income,
        floatArrayOf(0.85f, 0.95f, 1f, 0f, 0f, 0.6f)
    ),
    CAIDA(
        "Necesita cuidado",
        "Te queda entre el 15% y el 40%. Cuida tus gastos.",
        R.color.warning,
        floatArrayOf(0.62f, 0.75f, 0.8f, 0.4f, 0f, 0f)
    ),
    AMARILLA(
        "Muy sedienta",
        "Te queda menos del 15% de tus ingresos. Es momento de ahorrar.",
        R.color.warning,
        floatArrayOf(0.5f, 0.62f, 0.42f, 0.7f, 0f, -0.55f)
    ),
    MARCHITA(
        "Marchita",
        "Gastaste más de lo que te ingresó este mes.",
        R.color.expense,
        floatArrayOf(0.38f, 0.5f, 0f, 1f, 0f, -1f)
    )
}

// Decide el estado según cuánto te queda de los ingresos del mes (ya sin el diezmo).
// Para cambiar qué tan estricta es la plantita, cambia estos porcentajes.
fun estadoPlanta(ingresos: Double, gastos: Double, usarDiezmo: Boolean): EstadoPlanta {
    if (ingresos <= 0.0) {
        return if (gastos > 0.0) EstadoPlanta.MARCHITA else EstadoPlanta.SEMILLA
    }
    val queda = ingresos - Diezmo.monto(ingresos, usarDiezmo) - gastos
    val fraccion = queda / ingresos
    return when {
        fraccion < 0.0 -> EstadoPlanta.MARCHITA
        fraccion < 0.15 -> EstadoPlanta.AMARILLA
        fraccion < 0.40 -> EstadoPlanta.CAIDA
        fraccion < 0.70 -> EstadoPlanta.FRONDOSA
        else -> EstadoPlanta.FLORES
    }
}

class PlantaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density

    // Valores actuales (van cambiando poco a poco hacia los del estado nuevo)
    private val pose = EstadoPlanta.SEMILLA.destino.copyOf()
    private var estado: EstadoPlanta? = null
    private var durmiendo = true
    private var brisa = 0f   // de -1 a 1, para que la planta se mueva suavemente

    private val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val camino = Path()
    private val rect = RectF()
    private val punto = FloatArray(2)

    private val colorMaceta = Color.parseColor("#D9774A")
    private val colorBorde = Color.parseColor("#E58B5C")
    private val colorTierra = Color.parseColor("#5B3A29")
    private val colorRasgos = Color.parseColor("#3B2A20")
    private val colorSemilla = Color.parseColor("#8A5A34")
    private val colorMarron = Color.parseColor("#8B6B3E")
    private val colorAmarillo = Color.parseColor("#D7B93A")
    private val colorVerde = Color.parseColor("#43A047")
    private val colorPetalo = Color.parseColor("#FF8FB1")
    private val colorCentro = Color.parseColor("#FFD54F")
    private val colorMejilla = Color.parseColor("#FF9E9E")
    private val colorSueno = Color.parseColor("#9AA9BD")

    private val animadorBrisa = ValueAnimator.ofFloat(-1f, 1f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            brisa = it.animatedValue as Float
            invalidate()
        }
    }
    private var animadorCambio: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animadorBrisa.isStarted) animadorBrisa.start()
    }

    override fun onDetachedFromWindow() {
        animadorBrisa.cancel()
        animadorCambio?.cancel()
        super.onDetachedFromWindow()
    }

    // Cambia de estado con una animación suave
    fun mostrar(nuevo: EstadoPlanta) {
        if (nuevo == estado) return
        estado = nuevo
        durmiendo = nuevo.durmiendo

        animadorCambio?.cancel()
        val desde = pose.copyOf()
        val hasta = nuevo.destino
        animadorCambio = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedValue as Float
                for (i in pose.indices) pose[i] = mezcla(desde[i], hasta[i], t)
                invalidate()
            }
            start()
        }
    }

    // ---------- Dibujo ----------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val cx = w / 2f
        val tope = h * 0.64f           // parte de arriba de la maceta
        val fondo = h * 0.97f          // parte de abajo
        val anchoArriba = w * 0.50f
        val anchoAbajo = w * 0.36f

        // Cuerpo de la maceta
        relleno.color = colorMaceta
        relleno.pathEffect = CornerPathEffect(14f * dp)
        camino.reset()
        camino.moveTo(cx - anchoArriba / 2f, tope + 10f * dp)
        camino.lineTo(cx + anchoArriba / 2f, tope + 10f * dp)
        camino.lineTo(cx + anchoAbajo / 2f, fondo)
        camino.lineTo(cx - anchoAbajo / 2f, fondo)
        camino.close()
        canvas.drawPath(camino, relleno)
        relleno.pathEffect = null

        // Borde de la maceta
        rect.set(
            cx - anchoArriba / 2f - 6f * dp, tope,
            cx + anchoArriba / 2f + 6f * dp, tope + 16f * dp
        )
        relleno.color = colorBorde
        canvas.drawRoundRect(rect, 8f * dp, 8f * dp, relleno)

        // Tierra
        rect.set(
            cx - anchoArriba / 2f + 4f * dp, tope - 7f * dp,
            cx + anchoArriba / 2f - 4f * dp, tope + 7f * dp
        )
        relleno.color = colorTierra
        canvas.drawOval(rect, relleno)

        // La planta
        dibujarPlanta(canvas, cx, tope, w, h)

        // La carita, en el cuerpo de la maceta
        val cuerpoArriba = tope + 16f * dp
        dibujarCarita(canvas, cx, (cuerpoArriba + fondo) / 2f, (anchoArriba + anchoAbajo) / 2f)

        // Si está dormida (semilla), se dibujan las "z"
        if (durmiendo) {
            relleno.color = colorSueno
            relleno.textSize = 13f * dp
            canvas.drawText("z", cx + anchoArriba * 0.55f, tope - 14f * dp, relleno)
            relleno.textSize = 18f * dp
            canvas.drawText("Z", cx + anchoArriba * 0.55f + 10f * dp, tope - 30f * dp, relleno)
        }
    }

    private fun dibujarPlanta(canvas: Canvas, cx: Float, base: Float, w: Float, h: Float) {
        val altura = pose[0]
        val hojas = pose[1]
        val verde = pose[2].coerceIn(0f, 1f)
        val caida = pose[3]
        val flores = pose[4]

        // Sin tallo todavía: se ve la semilla sobre la tierra
        if (altura < 0.06f) {
            relleno.color = colorSemilla
            rect.set(cx - 7f * dp, base - 9f * dp, cx + 7f * dp, base - 1f * dp)
            canvas.drawOval(rect, relleno)
            return
        }

        val colorH = colorHoja(verde)

        // Tallo: una curva desde la tierra hasta la punta
        val largo = h * 0.46f * altura
        val inclinacion = caida * w * 0.13f
        val oscilacion = brisa * w * 0.02f * (1f - caida * 0.6f) * altura
        val puntaX = cx + inclinacion + oscilacion
        val puntaY = base - largo * (1f - caida * 0.18f)
        val ctrlX = cx + inclinacion * 0.1f
        val ctrlY = base - largo * 0.65f

        trazo.color = ColorUtils.blendARGB(colorH, Color.BLACK, 0.3f)
        trazo.strokeWidth = w * 0.035f
        camino.reset()
        camino.moveTo(cx, base)
        camino.quadTo(ctrlX, ctrlY, puntaX, puntaY)
        canvas.drawPath(camino, trazo)

        // Tres pares de hojas; van apareciendo conforme crece
        val posiciones = floatArrayOf(0.32f, 0.56f, 0.78f)
        val umbrales = floatArrayOf(0f, 0.25f, 0.5f)
        val tamanos = floatArrayOf(1f, 1.05f, 0.85f)
        val largoBase = w * 0.20f * hojas

        for (i in posiciones.indices) {
            val aparece = ((altura - umbrales[i]) / 0.2f).coerceIn(0f, 1f)
            if (aparece <= 0f) continue
            puntoEn(posiciones[i], cx, base, ctrlX, ctrlY, puntaX, puntaY)
            val largoHoja = largoBase * tamanos[i] * aparece
            val angulo = mezcla(-32f, 70f, caida) - i * 5f
            hoja(canvas, punto[0], punto[1], angulo, largoHoja, colorH)
            hoja(canvas, punto[0], punto[1], 180f - angulo, largoHoja, colorH)
        }

        // Brote de arriba (si hay flor, la flor ocupa su lugar)
        if (flores < 0.3f) {
            val aparece = ((altura - 0.15f) / 0.2f).coerceIn(0f, 1f)
            val angulo = mezcla(-70f, 10f, caida)
            hoja(canvas, puntaX, puntaY, angulo, largoBase * 0.9f * aparece, colorH)
            hoja(canvas, puntaX, puntaY, 180f - angulo, largoBase * 0.9f * aparece, colorH)
        }

        // Flor en la punta
        if (flores > 0.02f) {
            val r = w * 0.065f * flores
            val fy = puntaY - r * 0.4f
            relleno.color = colorPetalo
            for (k in 0 until 5) {
                val a = k * 2f * PI.toFloat() / 5f - PI.toFloat() / 2f
                canvas.drawCircle(
                    puntaX + cos(a) * r * 1.05f,
                    fy + sin(a) * r * 1.05f,
                    r * 0.8f,
                    relleno
                )
            }
            relleno.color = colorCentro
            canvas.drawCircle(puntaX, fy, r * 0.75f, relleno)
        }
    }

    // Una hoja: sale de (x, y) apuntando en el ángulo dado (en grados)
    private fun hoja(canvas: Canvas, x: Float, y: Float, angulo: Float, largo: Float, color: Int) {
        if (largo < 1f) return
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(angulo)

        val ancho = largo * 0.38f
        camino.reset()
        camino.moveTo(0f, 0f)
        camino.quadTo(largo * 0.5f, -ancho, largo, 0f)
        camino.quadTo(largo * 0.5f, ancho, 0f, 0f)
        relleno.color = color
        canvas.drawPath(camino, relleno)

        // Nervio del centro
        trazo.color = ColorUtils.blendARGB(color, Color.BLACK, 0.25f)
        trazo.strokeWidth = maxOf(1f, largo * 0.04f)
        canvas.drawLine(0f, 0f, largo * 0.85f, 0f, trazo)
        canvas.restore()
    }

    private fun dibujarCarita(canvas: Canvas, cx: Float, cy: Float, ancho: Float) {
        val animo = pose[5]
        val separacion = ancho * 0.21f
        val r = ancho * 0.05f
        val yOjos = cy - ancho * 0.06f
        val lados = intArrayOf(-1, 1)

        if (durmiendo) {
            // Ojos cerrados
            trazo.color = colorRasgos
            trazo.strokeWidth = ancho * 0.03f
            for (lado in lados) {
                val ox = cx + lado * separacion
                rect.set(ox - r, yOjos - r, ox + r, yOjos + r)
                canvas.drawArc(rect, 0f, 180f, false, trazo)
            }
        } else {
            // Ojos con un pequeño brillo
            for (lado in lados) {
                val ox = cx + lado * separacion
                relleno.color = colorRasgos
                canvas.drawCircle(ox, yOjos, r, relleno)
                relleno.color = Color.WHITE
                canvas.drawCircle(ox - r * 0.3f, yOjos - r * 0.3f, r * 0.35f, relleno)
            }
            // Cejas preocupadas cuando está triste
            if (animo < -0.15f) {
                trazo.color = colorRasgos
                trazo.strokeWidth = ancho * 0.025f
                for (lado in lados) {
                    val ox = cx + lado * separacion
                    val afuera = ox + lado * r * 1.6f
                    val adentro = ox - lado * r * 1.4f
                    canvas.drawLine(afuera, yOjos - r * 2.2f, adentro, yOjos - r * 3.6f, trazo)
                }
            }
            // Mejillas cuando está feliz
            if (animo > 0.5f) {
                relleno.color = ColorUtils.setAlphaComponent(colorMejilla, 110)
                for (lado in lados) {
                    val ox = cx + lado * separacion
                    canvas.drawCircle(ox + lado * r * 2.4f, yOjos + r * 3.2f, r * 1.2f, relleno)
                }
            }
        }

        // Boca: sonrisa, línea o ceño, según el ánimo
        val ym = cy + ancho * 0.14f
        val mitad = ancho * 0.12f
        val curva = ancho * 0.16f
        trazo.color = colorRasgos
        trazo.strokeWidth = ancho * 0.03f
        camino.reset()
        camino.moveTo(cx - mitad, ym)
        camino.quadTo(cx, ym + animo * curva, cx + mitad, ym)
        canvas.drawPath(camino, trazo)
    }

    // ---------- Ayudas ----------
    private fun mezcla(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    // De café (0) a amarillo (0.5) a verde (1)
    private fun colorHoja(v: Float): Int =
        if (v < 0.5f) ColorUtils.blendARGB(colorMarron, colorAmarillo, v / 0.5f)
        else ColorUtils.blendARGB(colorAmarillo, colorVerde, (v - 0.5f) / 0.5f)

    // Punto de una curva (el tallo) en la posición t, de 0 a 1
    private fun puntoEn(
        t: Float, x0: Float, y0: Float, xc: Float, yc: Float, x1: Float, y1: Float
    ) {
        val u = 1f - t
        punto[0] = u * u * x0 + 2f * u * t * xc + t * t * x1
        punto[1] = u * u * y0 + 2f * u * t * yc + t * t * y1
    }
}
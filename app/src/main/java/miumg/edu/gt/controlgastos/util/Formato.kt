package miumg.edu.gt.controlgastos.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

object Formato {

    fun moneda(monto: Double): String {
        val texto = "Q " + String.format(Locale.US, "%,.2f", Math.abs(monto))
        return if (monto < 0) "-$texto" else texto
    }

    fun fecha(millis: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale("es", "GT")).format(Date(millis))

    // El selector de fechas trabaja en UTC; estas funciones evitan que
    // la fecha se corra un día por la zona horaria.
    fun utcALocal(utcMillis: Long): Long {
        val dia = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return dia.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun localAUtc(localMillis: Long): Long {
        val dia = Instant.ofEpochMilli(localMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return dia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
}
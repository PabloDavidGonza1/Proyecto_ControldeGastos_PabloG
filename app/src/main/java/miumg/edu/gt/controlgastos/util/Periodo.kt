package miumg.edu.gt.controlgastos.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object Periodo {

    private val zona: ZoneId get() = ZoneId.systemDefault()

    fun actual(): YearMonth = YearMonth.now()

    // Primer instante del mes (incluido)
    fun inicio(m: YearMonth = YearMonth.now()): Long =
        m.atDay(1).atStartOfDay(zona).toInstant().toEpochMilli()

    // Primer instante del mes siguiente (excluido)
    fun fin(m: YearMonth = YearMonth.now()): Long =
        m.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant().toEpochMilli()

    fun mesDe(millis: Long): YearMonth =
        YearMonth.from(Instant.ofEpochMilli(millis).atZone(zona))

    fun nombre(m: YearMonth = YearMonth.now()): String {
        val n = m.month.getDisplayName(TextStyle.FULL, Locale("es", "GT"))
        return n.replaceFirstChar { it.uppercase() } + " " + m.year
    }
}
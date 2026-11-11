package com.javi.controlhorario

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

data class Split(val normalMs: Long, val highMs: Long) {
    val totalMs: Long get() = normalMs + highMs
    operator fun plus(o: Split) = Split(normalMs + o.normalMs, highMs + o.highMs)
    companion object { val ZERO = Split(0, 0) }
}

/**
 * Franjas de ALTA DEMANDA (según horas_alta_demanda.xlsx):
 *  - Lunes a jueves: 07:00-10:00 y 16:00-20:00
 *  - Viernes: 13:00-24:00
 *  - Sábado y domingo: 00:00-06:00
 * Todo lo demás es hora normal.
 * Un fichaje que cruza la medianoche se reparte por día natural, así que
 * viernes 22:00 -> sábado 02:00 cuenta entero como alta demanda.
 */
object HoursCalculator {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Franjas en minutos desde las 00:00 (inicio, fin). */
    fun windows(d: DayOfWeek): List<Pair<Int, Int>> = when (d) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY ->
            listOf(7 * 60 to 10 * 60, 16 * 60 to 20 * 60)
        DayOfWeek.FRIDAY -> listOf(13 * 60 to 24 * 60)
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> listOf(0 to 6 * 60)
    }

    private fun ms(date: LocalDate, minutes: Int): Long =
        date.atStartOfDay().plusMinutes(minutes.toLong()).atZone(zone).toInstant().toEpochMilli()

    fun dayBounds(date: LocalDate): Pair<Long, Long> = ms(date, 0) to ms(date, 24 * 60)

    /** Parte del intervalo [s,e] que cae en `date`, separada en normal / alta demanda. */
    fun split(date: LocalDate, s: Long, e: Long): Split {
        val (ds, de) = dayBounds(date)
        val cs = max(s, ds)
        val ce = min(e, de)
        if (ce <= cs) return Split.ZERO
        var high = 0L
        for ((a, b) in windows(date.dayOfWeek)) {
            high += max(0L, min(ce, ms(date, b)) - max(cs, ms(date, a)))
        }
        return Split((ce - cs) - high, high)
    }

    fun forDay(date: LocalDate, entries: List<Entry>, now: Long): Split =
        entries.fold(Split.ZERO) { acc, en -> acc + split(date, en.start, en.end ?: now) }

    fun fmt(ms: Long): String {
        val totalMin = ms / 60000
        return "%dh %02dm".format(totalMin / 60, totalMin % 60)
    }
}

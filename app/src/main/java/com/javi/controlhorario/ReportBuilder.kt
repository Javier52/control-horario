package com.javi.controlhorario

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.min

object ReportBuilder {
    private val loc = Locale("es", "ES")
    private val dayFmt = DateTimeFormatter.ofPattern("EEEE dd/MM/yyyy", loc)
    private val shortFmt = DateTimeFormatter.ofPattern("EEE dd/MM", loc)
    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val zone get() = ZoneId.systemDefault()

    fun mondayOf(d: LocalDate): LocalDate = d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun time(ms: Long): String = Instant.ofEpochMilli(ms).atZone(zone).format(hm)

    /** Fichajes (recortados al día) que tocan `date`. */
    fun clippedEntries(date: LocalDate, entries: List<Entry>, now: Long): List<Triple<Long, Long, Entry>> {
        val (ds, de) = HoursCalculator.dayBounds(date)
        return entries.mapNotNull {
            val s = max(it.start, ds); val e = min(it.end ?: now, de)
            if (e > s) Triple(s, e, it) else null
        }
    }

    fun daily(date: LocalDate, entries: List<Entry>, now: Long): String {
        val sb = StringBuilder()
        sb.appendLine("INFORME DIARIO DE HORAS - ${date.format(dayFmt)}")
        sb.appendLine("=".repeat(48))
        sb.appendLine()
        sb.appendLine("FICHAJES DEL DÍA")
        val list = clippedEntries(date, entries, now)
        if (list.isEmpty()) sb.appendLine("  (sin fichajes)")
        list.forEach { (s, e, en) ->
            val sp = HoursCalculator.split(date, s, e)
            val abierto = if (en.end == null) " [fichaje abierto]" else ""
            sb.appendLine("  ${time(s)} - ${time(e)}  total ${HoursCalculator.fmt(sp.totalMs)}  " +
                "(normal ${HoursCalculator.fmt(sp.normalMs)} | alta demanda ${HoursCalculator.fmt(sp.highMs)})$abierto")
        }
        val day = HoursCalculator.forDay(date, entries, now)
        sb.appendLine()
        sb.appendLine("TOTAL DEL DÍA")
        sb.appendLine("  Horas normales:      ${HoursCalculator.fmt(day.normalMs)}")
        sb.appendLine("  Horas alta demanda:  ${HoursCalculator.fmt(day.highMs)}")
        sb.appendLine("  Total:               ${HoursCalculator.fmt(day.totalMs)}")
        sb.appendLine()

        val monday = mondayOf(date)
        sb.appendLine("RESUMEN SEMANAL (${monday.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))})")
        var week = Split.ZERO
        for (i in 0..6) {
            val d = monday.plusDays(i.toLong())
            val s = HoursCalculator.forDay(d, entries, now)
            week += s
            sb.appendLine("  ${d.format(shortFmt).padEnd(11)} normal ${HoursCalculator.fmt(s.normalMs).padEnd(8)} alta ${HoursCalculator.fmt(s.highMs).padEnd(8)} total ${HoursCalculator.fmt(s.totalMs)}")
        }
        sb.appendLine("  " + "-".repeat(46))
        sb.appendLine("  SEMANA      normal ${HoursCalculator.fmt(week.normalMs).padEnd(8)} alta ${HoursCalculator.fmt(week.highMs).padEnd(8)} total ${HoursCalculator.fmt(week.totalMs)}")
        sb.appendLine()
        sb.appendLine("Franjas de alta demanda: L-J 07-10 y 16-20 | V 13-24 | S-D 00-06")
        return sb.toString()
    }

    fun subject(date: LocalDate) = "Informe de horas ${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
}

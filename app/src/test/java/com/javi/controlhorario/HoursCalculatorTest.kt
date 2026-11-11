package com.javi.controlhorario

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HoursCalculatorTest {
    private fun ms(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val H = 3_600_000L

    @Test fun lunesMezcla() {
        // Lunes 2026-11-09, 06:00-11:00 => alta 07-10 (3h), normal 2h
        val s = HoursCalculator.split(LocalDate.of(2026, 11, 9), ms(2026, 11, 9, 6), ms(2026, 11, 9, 11))
        assertEquals(3 * H, s.highMs); assertEquals(2 * H, s.normalMs)
    }

    @Test fun lunesTarde() {
        // 15:00-21:00 => alta 16-20 (4h), normal 2h
        val s = HoursCalculator.split(LocalDate.of(2026, 11, 9), ms(2026, 11, 9, 15), ms(2026, 11, 9, 21))
        assertEquals(4 * H, s.highMs); assertEquals(2 * H, s.normalMs)
    }

    @Test fun viernesNocheCruzaMedianoche() {
        // Viernes 2026-11-13 22:00 -> sábado 02:00 => todo alta demanda
        val a = ms(2026, 11, 13, 22); val b = ms(2026, 11, 14, 2)
        val vie = HoursCalculator.split(LocalDate.of(2026, 11, 13), a, b)
        val sab = HoursCalculator.split(LocalDate.of(2026, 11, 14), a, b)
        assertEquals(2 * H, vie.highMs); assertEquals(0L, vie.normalMs)
        assertEquals(2 * H, sab.highMs); assertEquals(0L, sab.normalMs)
    }

    @Test fun viernesManana() {
        // Viernes 09:00-15:00 => alta 13-15 (2h), normal 4h
        val s = HoursCalculator.split(LocalDate.of(2026, 11, 13), ms(2026, 11, 13, 9), ms(2026, 11, 13, 15))
        assertEquals(2 * H, s.highMs); assertEquals(4 * H, s.normalMs)
    }

    @Test fun domingoDia() {
        // Domingo 10:00-12:00 => normal
        val s = HoursCalculator.split(LocalDate.of(2026, 11, 15), ms(2026, 11, 15, 10), ms(2026, 11, 15, 12))
        assertEquals(0L, s.highMs); assertEquals(2 * H, s.normalMs)
    }
}

package com.javi.controlhorario

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.concurrent.thread

private val NormalColor = Color(0xFF1E88E5)
private val HighColor = Color(0xFFFB8C00)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scheduler.schedule(this)
        val store = Store(this)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { App(store) } } }
    }
}

private class DayData(val date: LocalDate, val split: Split, val spans: List<Triple<Long, Long, Entry>>)

@Composable
private fun App(store: Store) {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(store.entries()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    val active = entries.lastOrNull { it.end == null }
    val today = LocalDate.now()
    val monday = ReportBuilder.mondayOf(today).plusWeeks(weekOffset.toLong())
    val days = (0..6).map {
        val d = monday.plusDays(it.toLong())
        DayData(d, HoursCalculator.forDay(d, entries, now), ReportBuilder.clippedEntries(d, entries, now))
    }
    val week = days.fold(Split.ZERO) { a, d -> a + d.split }
    val todaySplit = HoursCalculator.forDay(today, entries, now)
    val loc = Locale("es", "ES")

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Control horario", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showSettings = true }) { Text("Ajustes") }
            }
        }
        item {
            Button(
                onClick = { entries = store.toggle() },
                modifier = Modifier.fillMaxWidth().height(88.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (active == null) Color(0xFF2E7D32) else Color(0xFFC62828))
            ) {
                Text(if (active == null) "FICHAR ENTRADA" else "FICHAR SALIDA", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    if (active != null) {
                        Text("En curso desde ${ReportBuilder.time(active.start)}  ·  ${HoursCalculator.fmt(now - active.start)}", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                    } else Text("Sin fichaje abierto", fontWeight = FontWeight.Medium)
                    Text("Hoy: ${HoursCalculator.fmt(todaySplit.totalMs)}")
                    Text("   Normales ${HoursCalculator.fmt(todaySplit.normalMs)}  ·  Alta demanda ${HoursCalculator.fmt(todaySplit.highMs)}")
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { weekOffset-- }) { Text("◀ Anterior") }
                Text("${monday.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${monday.plusDays(6).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", fontWeight = FontWeight.Bold)
                TextButton(onClick = { weekOffset++ }, enabled = weekOffset < 0) { Text("Siguiente ▶") }
            }
        }
        item { WeekChart(days) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Legend(NormalColor, "Normal"); Legend(HighColor, "Alta demanda")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total semana: ${HoursCalculator.fmt(week.totalMs)}", fontWeight = FontWeight.Bold)
                    Text("Normales ${HoursCalculator.fmt(week.normalMs)}  ·  Alta demanda ${HoursCalculator.fmt(week.highMs)}")
                }
            }
        }
        days.forEach { d ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(d.date.format(DateTimeFormatter.ofPattern("EEEE dd/MM", loc)).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                        Text("Total ${HoursCalculator.fmt(d.split.totalMs)}  (normal ${HoursCalculator.fmt(d.split.normalMs)} · alta ${HoursCalculator.fmt(d.split.highMs)})", fontSize = 13.sp)
                        d.spans.forEach { (s, e, en) ->
                            val sp = HoursCalculator.split(d.date, s, e)
                            Text("  ${ReportBuilder.time(s)} - ${ReportBuilder.time(e)}${if (en.end == null) " (en curso)" else ""}   n ${HoursCalculator.fmt(sp.normalMs)} / a ${HoursCalculator.fmt(sp.highMs)}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showSettings) SettingsDialog(store, onClose = { showSettings = false }, onSendNow = {
        Toast.makeText(ctx, "Enviando informe…", Toast.LENGTH_SHORT).show()
        thread {
            val msg = try {
                val n = System.currentTimeMillis()
                EmailSender.send(store.settings(), ReportBuilder.subject(LocalDate.now()), ReportBuilder.daily(LocalDate.now(), store.entries(), n))
                "Informe enviado"
            } catch (e: Exception) { "Error: ${e.message}" }
            android.os.Handler(android.os.Looper.getMainLooper()).post { Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
        }
    })
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(12.dp)) { drawRect(color) }
        Spacer(Modifier.width(6.dp)); Text(text, fontSize = 13.sp)
    }
}

/** Gráfica de barras apiladas: horas normales (azul) + alta demanda (naranja) por día. */
@Composable
private fun WeekChart(days: List<DayData>) {
    val labels = listOf("L", "M", "X", "J", "V", "S", "D")
    val maxMs = maxOf(days.maxOf { it.split.totalMs }, 3_600_000L)
    val maxH = kotlin.math.ceil(maxMs / 3_600_000.0).toFloat()
    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val left = 36.dp.toPx(); val bottom = 28.dp.toPx(); val top = 22.dp.toPx()
        val chartH = size.height - bottom - top
        val slot = (size.width - left) / 7f
        val barW = slot * 0.6f
        val paint = android.graphics.Paint().apply { textSize = 12.sp.toPx(); color = android.graphics.Color.DKGRAY; isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }
        drawIntoCanvas { c ->
            for (g in 0..4) {
                val y = top + chartH - chartH * g / 4f
                drawLine(Color(0x33000000), Offset(left, y), Offset(size.width, y))
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                c.nativeCanvas.drawText("%.0fh".format(maxH * g / 4f), left - 4.dp.toPx(), y + 4.dp.toPx(), paint)
            }
            paint.textAlign = android.graphics.Paint.Align.CENTER
            days.forEachIndexed { i, d ->
                val x = left + slot * i + (slot - barW) / 2
                val hN = chartH * (d.split.normalMs / 3_600_000f) / maxH
                val hH = chartH * (d.split.highMs / 3_600_000f) / maxH
                val baseY = top + chartH
                drawRect(NormalColor, Offset(x, baseY - hN), Size(barW, hN))
                drawRect(HighColor, Offset(x, baseY - hN - hH), Size(barW, hH))
                if (d.split.totalMs > 0) c.nativeCanvas.drawText(HoursCalculator.fmt(d.split.totalMs).replace(" ", ""), x + barW / 2, baseY - hN - hH - 4.dp.toPx(), paint)
                c.nativeCanvas.drawText(labels[i], x + barW / 2, size.height - 8.dp.toPx(), paint)
            }
        }
    }
}

@Composable
private fun SettingsDialog(store: Store, onClose: () -> Unit, onSendNow: () -> Unit) {
    val cur = remember { store.settings() }
    var to by remember { mutableStateOf(cur.to) }
    var user by remember { mutableStateOf(cur.smtpUser) }
    var pass by remember { mutableStateOf(cur.smtpPass) }
    var hour by remember { mutableStateOf(cur.hour.toString()) }
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Informe diario por email") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(to, { to = it }, label = { Text("Enviar a") }, singleLine = true)
                OutlinedTextField(user, { user = it }, label = { Text("Cuenta Gmail remitente") }, singleLine = true)
                OutlinedTextField(pass, { pass = it }, label = { Text("Contraseña de aplicación") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Hora de envío (0-23)") }, singleLine = true)
                TextButton(onClick = {
                    store.saveSettings(Settings(to.trim(), user.trim(), pass.trim(), (hour.toIntOrNull() ?: 22).coerceIn(0, 23)))
                    onSendNow()
                }) { Text("Guardar y enviar informe ahora") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                store.saveSettings(Settings(to.trim(), user.trim(), pass.trim(), (hour.toIntOrNull() ?: 22).coerceIn(0, 23)))
                Scheduler.schedule(ctx)
                onClose()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } }
    )
}

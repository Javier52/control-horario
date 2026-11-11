package com.javi.controlhorario

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class DailyReportWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val store = Store(applicationContext)
        val settings = store.settings()
        return try {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()
            EmailSender.send(settings, ReportBuilder.subject(today), ReportBuilder.daily(today, store.entries(), now))
            Scheduler.schedule(applicationContext, append = true)   // programa el de mañana
            Result.success()
        } catch (e: Exception) {
            if (settings.smtpPass.isBlank()) {
                Scheduler.schedule(applicationContext, append = true)
                Result.failure()
            } else Result.retry()
        }
    }
}

object Scheduler {
    private const val NAME = "informe_diario"

    /** Programa el próximo envío a la hora configurada (hoy si aún no ha pasado, si no mañana). */
    fun schedule(ctx: Context, append: Boolean = false) {
        val hour = Store(ctx).settings().hour
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val req = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(NAME, if (append) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, req)
    }
}

package com.example.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.observability.MonitorErrores
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Programador centralizado de trabajos en segundo plano de WorkManager para mantener actualizada
 * la caché local de productos en Room sin bloquear la UI ni abusar de batería/red.
 */
object ProgramadorSincronizacionProductos {

    const val UNIQUE_PERIODIC_SYNC_NAME = "elegan_periodic_catalog_sync"
    const val UNIQUE_IMMEDIATE_SYNC_NAME = "elegan_immediate_catalog_sync"
    const val TAG_CATALOG_SYNC = "elegan_catalog_sync"

    const val PERIODIC_INTERVAL_HOURS = 6L
    const val PERIODIC_FLEX_HOURS = 1L

    fun construirSolicitudPeriodica(
        pageSize: Int = SincronizadorProductosWorker.DEFAULT_SYNC_PAGE_SIZE,
        maxPages: Int = SincronizadorProductosWorker.DEFAULT_MAX_SYNC_PAGES
    ): PeriodicWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        return PeriodicWorkRequestBuilder<SincronizadorProductosWorker>(
            repeatInterval = PERIODIC_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = PERIODIC_FLEX_HOURS,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.SECONDS
            )
            .setInputData(
                workDataOf(
                    SincronizadorProductosWorker.KEY_PAGE_SIZE to pageSize,
                    SincronizadorProductosWorker.KEY_MAX_PAGES to maxPages
                )
            )
            .addTag(TAG_CATALOG_SYNC)
            .build()
    }

    fun construirSolicitudInmediata(
        pageSize: Int = SincronizadorProductosWorker.DEFAULT_SYNC_PAGE_SIZE,
        maxPages: Int = SincronizadorProductosWorker.DEFAULT_MAX_SYNC_PAGES
    ): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        return OneTimeWorkRequestBuilder<SincronizadorProductosWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15L,
                TimeUnit.SECONDS
            )
            .setInputData(
                workDataOf(
                    SincronizadorProductosWorker.KEY_PAGE_SIZE to pageSize,
                    SincronizadorProductosWorker.KEY_MAX_PAGES to maxPages
                )
            )
            .addTag(TAG_CATALOG_SYNC)
            .build()
    }

    /**
     * Registra la sincronización periódica única (cada 6h con red y batería saludable).
     * Usa [ExistingPeriodicWorkPolicy.KEEP] para respetar el calendario vigente entre reinicios.
     */
    fun programarSincronizacionPeriodica(context: Context): Boolean {
        return runCatching {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_SYNC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                construirSolicitudPeriodica()
            )
            true
        }.onFailure { error ->
            MonitorErrores.registrarAdvertencia(
                tag = "ProgramadorSyncCatalogo",
                mensaje = "No se pudo programar la sincronización periódica de productos.",
                error = error
            )
        }.getOrDefault(false)
    }

    /**
     * Encola una sincronización única en segundo plano con restricción `NetworkType.CONNECTED`.
     * Si el dispositivo está offline, WorkManager la ejecutará automáticamente al recuperar red.
     */
    fun solicitarSincronizacionInmediata(
        context: Context,
        forceReplace: Boolean = false
    ): Boolean {
        return runCatching {
            val workManager = WorkManager.getInstance(context.applicationContext)
            val policy = if (forceReplace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
            workManager.enqueueUniqueWork(
                UNIQUE_IMMEDIATE_SYNC_NAME,
                policy,
                construirSolicitudInmediata()
            )
            true
        }.onFailure { error ->
            MonitorErrores.registrarAdvertencia(
                tag = "ProgramadorSyncCatalogo",
                mensaje = "No se pudo encolar la sincronización inmediata de productos.",
                error = error
            )
        }.getOrDefault(false)
    }

    /**
     * Emite `true` mientras algún trabajo de sincronización del catálogo esté ejecutándose.
     */
    fun observarSincronizacionActiva(context: Context): Flow<Boolean> {
        val workManager = runCatching {
            WorkManager.getInstance(context.applicationContext)
        }.getOrNull() ?: return flowOf(false)

        val immediateFlow = workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_IMMEDIATE_SYNC_NAME)
        val periodicFlow = workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_SYNC_NAME)

        return combine(immediateFlow, periodicFlow) { immediateInfos, periodicInfos ->
            (immediateInfos + periodicInfos).any { it.state == WorkInfo.State.RUNNING }
        }
            .catch { emit(false) }
    }
}

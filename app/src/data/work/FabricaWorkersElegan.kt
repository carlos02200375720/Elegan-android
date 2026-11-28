package com.example.data.work

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.data.local.EleganDatabase
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi

/**
 * Fábrica de Workers de WorkManager para inyectar dependencias (API y base Room) sin acoplamiento
 * estático y facilitar pruebas unitarias deterministas con `TestListenableWorkerBuilder`.
 */
class FabricaWorkersElegan internal constructor(
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service },
    private val databaseProvider: (Context) -> EleganDatabase = { ctx ->
        EleganDatabase.getInstance(ctx.applicationContext)
    },
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) : WorkerFactory() {

    constructor() : this(
        apiProvider = { ApiAutenticacionMovil.service },
        databaseProvider = { ctx -> EleganDatabase.getInstance(ctx.applicationContext) },
        clockMs = { System.currentTimeMillis() }
    )

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SincronizadorProductosWorker::class.java.name -> SincronizadorProductosWorker(
                appContext = appContext,
                workerParams = workerParameters,
                apiProvider = apiProvider,
                databaseProvider = { databaseProvider(appContext) },
                clockMs = clockMs
            )
            else -> null
        }
    }
}

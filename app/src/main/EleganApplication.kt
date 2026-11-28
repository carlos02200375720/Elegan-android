package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.di.ContenedorDependenciasApp
import com.example.data.di.ContenedorDependenciasElegan
import com.example.data.di.ProveedorContenedorElegan
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ClienteHttpElegan

class EleganApplication : Application(), ImageLoaderFactory, ProveedorContenedorElegan {
    override val contenedor: ContenedorDependenciasElegan by lazy {
        ContenedorDependenciasApp(this)
    }

    override fun onCreate() {
        super.onCreate()
        ClienteHttpElegan.inicializar(this)
        MonitorErrores.inicializar(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ClienteHttpElegan.mediaClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("elegan_image_cache"))
                    .maxSizeBytes(150L * 1024L * 1024L)
                    .build()
            }
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .crossfade(120)
            .build()
    }
}

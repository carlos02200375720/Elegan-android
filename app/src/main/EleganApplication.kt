package com.example

import android.app.Application
import android.content.ComponentCallbacks2
import android.util.Log
import androidx.startup.AppInitializer
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.data.di.ContenedorDependenciasApp
import com.example.data.di.ContenedorDependenciasElegan
import com.example.data.di.ProveedorContenedorElegan
import com.example.data.media.ConfiguracionCacheImagenesCoil
import com.example.data.work.FabricaWorkersElegan
import com.example.startup.ClienteHttpInitializer

class EleganApplication : Application(), ImageLoaderFactory, ProveedorContenedorElegan, Configuration.Provider {
    @Volatile
    private var activeImageLoader: ImageLoader? = null

    override val contenedor: ContenedorDependenciasElegan by lazy {
        ContenedorDependenciasApp(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .setWorkerFactory(FabricaWorkersElegan())
            .build()

    override fun onCreate() {
        super.onCreate()
        // ClienteHttpInitializer se autodescubre vía androidx.startup.InitializationProvider;
        // Sentry, Room y WorkManager se inicializan de forma diferida tras el primer frame en MainActivity.
        AppInitializer.getInstance(this).initializeComponent(ClienteHttpInitializer::class.java)
    }

    override fun newImageLoader(): ImageLoader {
        return ConfiguracionCacheImagenesCoil.crearImageLoader(this).also { loader ->
            activeImageLoader = loader
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        ConfiguracionCacheImagenesCoil.recortarMemoria(activeImageLoader, level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ConfiguracionCacheImagenesCoil.recortarMemoria(
            activeImageLoader,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        )
    }
}

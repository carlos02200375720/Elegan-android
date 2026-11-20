package com.example

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.ui.theme.EleganTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE)
        )
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        configureCoilImageLoader()

        setContent {
            EleganTheme {
                MainApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun configureCoilImageLoader() {
        if (isCoilConfigured) return
        synchronized(this) {
            if (isCoilConfigured) return
            val appCtx = applicationContext
            Coil.setImageLoader(
                ImageLoader.Builder(appCtx)
                    .memoryCache {
                        MemoryCache.Builder(appCtx)
                            .maxSizePercent(0.30)
                            .build()
                    }
                    .diskCache {
                        DiskCache.Builder()
                            .directory(appCtx.cacheDir.resolve("elegan_image_cache"))
                            .maxSizePercent(0.05)
                            .build()
                    }
                    .respectCacheHeaders(false)
                    .allowRgb565(true)
                    .crossfade(false)
                    .build()
            )
            isCoilConfigured = true
        }
    }

    companion object {
        @Volatile
        private var isCoilConfigured = false
    }
}

package com.example

import android.app.Application
import com.example.data.observability.MonitorErrores

class EleganApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MonitorErrores.inicializar(this)
    }
}

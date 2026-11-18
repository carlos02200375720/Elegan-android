package com.example.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal data class CompartidoConfirmadoEvent(
    val requestId: String,
    val productId: String
)

internal object DespachadorCompartidoPublicacion {
    private val _confirmaciones = MutableSharedFlow<CompartidoConfirmadoEvent>(extraBufferCapacity = 32)
    val confirmaciones: SharedFlow<CompartidoConfirmadoEvent> = _confirmaciones.asSharedFlow()

    fun notificarCompartidoConfirmado(requestId: String, productId: String) {
        if (requestId.isBlank() || productId.isBlank()) return
        _confirmaciones.tryEmit(CompartidoConfirmadoEvent(requestId = requestId, productId = productId))
    }
}

class ReceptorCompartidoPublicacion : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val productId = intent?.getStringExtra(EXTRA_PRODUCT_ID).orEmpty()
        val requestId = intent?.getStringExtra(EXTRA_SHARE_REQUEST_ID).orEmpty()
        DespachadorCompartidoPublicacion.notificarCompartidoConfirmado(
            requestId = requestId,
            productId = productId
        )
    }

    internal companion object {
        const val EXTRA_PRODUCT_ID = "extra_elegan_share_product_id"
        const val EXTRA_SHARE_REQUEST_ID = "extra_elegan_share_request_id"
    }
}

internal fun lanzarSelectorCompartirPublicacion(
    context: Context,
    productId: String,
    productName: String,
    productPrice: Double
): String {
    val requestId = UUID.randomUUID().toString()
    val appContext = context.applicationContext
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "ELEGAN: Descubre $productName por $${String.format("%.2f", productPrice)} USD. Pieza exclusiva de la boutique."
        )
    }
    val callbackIntent = Intent(appContext, ReceptorCompartidoPublicacion::class.java).apply {
        putExtra(ReceptorCompartidoPublicacion.EXTRA_PRODUCT_ID, productId)
        putExtra(ReceptorCompartidoPublicacion.EXTRA_SHARE_REQUEST_ID, requestId)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        appContext,
        requestId.hashCode(),
        callbackIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val chooserIntent = Intent.createChooser(
        sendIntent,
        "Compartir publicación",
        pendingIntent.intentSender
    )
    if (context !is Activity) {
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooserIntent)
    return requestId
}

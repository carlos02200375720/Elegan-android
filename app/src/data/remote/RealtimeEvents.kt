package com.example.data.remote

import android.os.Build
import com.example.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RealtimeEvent(val collection: String, val operation: String)

object RealtimeEvents {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val mutableEvents = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events = mutableEvents.asSharedFlow()
    private var connectionJob: Job? = null

    @Synchronized
    fun start(token: String?) {
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return
        connectionJob?.cancel()
        connectionJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val base = BuildConfig.ELEGAN_API_BASE_URL.trimEnd('/')
                .replaceFirst(Regex("^https:", RegexOption.IGNORE_CASE), "wss:")
                .replaceFirst(Regex("^http:", RegexOption.IGNORE_CASE), "ws:")
            while (isActive) {
                val closed = CompletableDeferred<Unit>()
                val request = Request.Builder().url("$base/api/movil/events/ws").build()
                var socket: WebSocket? = null
                try {
                    socket = client.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            webSocket.send(JSONObject().put("token", token.orEmpty()).toString())
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            runCatching {
                                val json = JSONObject(text)
                                if (json.optString("type") == "data_changed") {
                                    mutableEvents.tryEmit(
                                        RealtimeEvent(
                                            collection = json.optString("collection"),
                                            operation = json.optString("operation")
                                        )
                                    )
                                }
                            }
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            if (!closed.isCompleted) closed.complete(Unit)
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            if (!closed.isCompleted) closed.complete(Unit)
                        }
                    })
                    closed.await()
                } catch (_: Exception) {
                    // La conexión se reintenta sin bloquear el hilo principal.
                } finally {
                    socket?.cancel()
                }
                if (isActive) delay(2000)
            }
        }
    }

    @Synchronized
    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
    }
}

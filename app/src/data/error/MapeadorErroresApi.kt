package com.example.data.error

import com.example.data.observability.MonitorErrores
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.EOFException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Mapeador global de excepciones hacia [ErrorApi] y ejecutor seguro [safeApiCall]
 * para estandarizar todas las llamadas de red y parsing en los repositorios.
 */
object MapeadorErroresApi {

    fun mapear(throwable: Throwable): ErrorApi {
        if (throwable is CancellationException) throw throwable

        return when (throwable) {
            is ApiResultException -> throwable.error
            is ApiParsingException -> ErrorApi.ParsingError(
                fieldOrContext = throwable.fieldOrContext,
                userMessage = throwable.message?.takeIf { it.isNotBlank() }
                    ?: ErrorApi.DEFAULT_PARSING_MESSAGE,
                cause = throwable
            )
            is JsonDataException,
            is JsonEncodingException,
            is SerializationException,
            is JSONException,
            is EOFException -> ErrorApi.ParsingError(
                fieldOrContext = throwable.javaClass.simpleName,
                userMessage = ErrorApi.DEFAULT_PARSING_MESSAGE,
                cause = throwable
            )
            is HttpException -> mapearHttpException(throwable)
            is SocketTimeoutException,
            is InterruptedIOException -> ErrorApi.NetworkError(
                userMessage = ErrorApi.DEFAULT_TIMEOUT_MESSAGE,
                isTimeout = true,
                cause = throwable
            )
            is IOException -> ErrorApi.NetworkError(
                userMessage = ErrorApi.DEFAULT_NETWORK_MESSAGE,
                isTimeout = false,
                cause = throwable
            )
            else -> ErrorApi.UnknownError(
                userMessage = throwable.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: ErrorApi.DEFAULT_UNKNOWN_MESSAGE,
                cause = throwable
            )
        }
    }

    private fun mapearHttpException(exception: HttpException): ErrorApi {
        val code = exception.code()
        val rawBody = runCatching { exception.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        val (backendCode, backendMessage) = extraerDetalleErrorHttp(rawBody)

        return when {
            code == 401 -> ErrorApi.UnauthorizedError(
                httpCode = code,
                userMessage = backendMessage?.takeIf { it.isNotBlank() && !esCodigoMaquina(it) }
                    ?: ErrorApi.DEFAULT_UNAUTHORIZED_MESSAGE,
                cause = exception
            )
            code >= 500 -> ErrorApi.ServerError(
                httpCode = code,
                backendCode = backendCode,
                backendMessage = backendMessage,
                userMessage = ErrorApi.DEFAULT_SERVER_MESSAGE,
                cause = exception
            )
            code in 400..499 -> {
                val friendly = when {
                    !backendMessage.isNullOrBlank() && !esCodigoMaquina(backendMessage) -> backendMessage
                    code == 403 -> "No tienes permisos para realizar esta acción."
                    code == 404 -> "El recurso solicitado no se encontró en el servidor."
                    code == 408 -> ErrorApi.DEFAULT_TIMEOUT_MESSAGE
                    code == 409 -> "Conflicto al actualizar los datos. Actualiza e inténtalo de nuevo."
                    code == 422 -> "Algunos datos enviados no son válidos. Verifícalos e inténtalo de nuevo."
                    code == 429 -> "Demasiadas solicitudes en poco tiempo. Espera unos segundos e inténtalo nuevamente."
                    else -> ErrorApi.DEFAULT_CLIENT_MESSAGE
                }
                ErrorApi.ClientError(
                    httpCode = code,
                    backendCode = backendCode,
                    backendMessage = backendMessage,
                    userMessage = friendly,
                    cause = exception
                )
            }
            else -> ErrorApi.UnknownError(
                userMessage = backendMessage?.takeIf { it.isNotBlank() } ?: ErrorApi.DEFAULT_UNKNOWN_MESSAGE,
                cause = exception
            )
        }
    }

    internal fun extraerDetalleErrorHttp(rawBody: String): Pair<String?, String?> {
        val clean = rawBody.trim()
        if (clean.isEmpty()) return null to null

        if (clean.startsWith("{")) {
            val parsed = runCatching { JSONObject(clean) }.getOrNull()
            if (parsed != null) {
                val detail = parsed.optString("detail").takeIf { it.isNotBlank() }
                val message = parsed.optString("message").takeIf { it.isNotBlank() }
                val errorField = parsed.optString("error").takeIf { it.isNotBlank() }
                val codeField = parsed.optString("code").takeIf { it.isNotBlank() }
                    ?: detail?.takeIf { esCodigoMaquina(it) }
                    ?: errorField?.takeIf { esCodigoMaquina(it) }
                val humanMessage = message ?: detail ?: errorField
                return codeField to humanMessage
            }
        }
        val trimmedText = clean.take(240)
        return (trimmedText.takeIf { esCodigoMaquina(it) }) to trimmedText
    }

    private fun esCodigoMaquina(value: String): Boolean =
        value.matches(Regex("^[A-Z0-9_]{3,64}$"))
}

/**
 * Ejecuta una operación de API en [dispatcher], clasificando cualquier fallo en [ErrorApi]
 * y registrando telemetría uniforme en [MonitorErrores].
 */
suspend fun <T> safeApiCall(
    tag: String,
    operation: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    extras: Map<String, String> = emptyMap(),
    logAsWarningOnNetwork: Boolean = true,
    block: suspend () -> T
): ResultWrapper<T> = withContext(dispatcher) {
    try {
        ResultWrapper.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        val mapped = MapeadorErroresApi.mapear(t)
        val enrichedExtras = buildMap {
            putAll(extras)
            put("operation", operation)
            put("error_category", mapped.errorCategory)
            when (mapped) {
                is ErrorApi.ServerError -> put("http_code", mapped.httpCode.toString())
                is ErrorApi.ClientError -> put("http_code", mapped.httpCode.toString())
                is ErrorApi.UnauthorizedError -> put("http_code", mapped.httpCode.toString())
                is ErrorApi.ParsingError -> mapped.fieldOrContext?.let { put("parse_context", it) }
                is ErrorApi.NetworkError -> put("timeout", mapped.isTimeout.toString())
                is ErrorApi.UnknownError -> Unit
            }
        }

        val summary = "Fallo en $operation (${mapped.errorCategory}): ${mapped.userMessage}"
        val shouldWarnOnly = logAsWarningOnNetwork && (
            mapped is ErrorApi.NetworkError ||
                mapped is ErrorApi.UnauthorizedError ||
                mapped is ErrorApi.ClientError
            )
        if (shouldWarnOnly) {
            MonitorErrores.registrarAdvertencia(
                tag = tag,
                mensaje = summary,
                error = t,
                extras = enrichedExtras
            )
        } else {
            MonitorErrores.registrarError(
                tag = tag,
                mensaje = summary,
                error = t,
                extras = enrichedExtras
            )
        }
        ResultWrapper.Error(mapped)
    }
}

package com.example.data.error

/**
 * Clasificación global y tipada de errores de red, servidor, cliente, autenticación y parsing
 * para estandarizar el manejo de fallos en todos los repositorios y ViewModels de Elegan.
 */
sealed interface ErrorApi {
    val userMessage: String
    val cause: Throwable?
    val errorCategory: String
    val isRetryable: Boolean

    data class NetworkError(
        override val userMessage: String = DEFAULT_NETWORK_MESSAGE,
        val isTimeout: Boolean = false,
        override val cause: Throwable? = null
    ) : ErrorApi {
        override val errorCategory: String = "network"
        override val isRetryable: Boolean = true
    }

    data class ServerError(
        val httpCode: Int,
        val backendCode: String? = null,
        val backendMessage: String? = null,
        override val userMessage: String = DEFAULT_SERVER_MESSAGE,
        override val cause: Throwable? = null
    ) : ErrorApi {
        override val errorCategory: String = "server"
        override val isRetryable: Boolean = true
    }

    data class ClientError(
        val httpCode: Int,
        val backendCode: String? = null,
        val backendMessage: String? = null,
        override val userMessage: String = DEFAULT_CLIENT_MESSAGE,
        override val cause: Throwable? = null
    ) : ErrorApi {
        override val errorCategory: String = "client"
        override val isRetryable: Boolean = httpCode == 408 || httpCode == 429
    }

    data class UnauthorizedError(
        val httpCode: Int = 401,
        override val userMessage: String = DEFAULT_UNAUTHORIZED_MESSAGE,
        override val cause: Throwable? = null
    ) : ErrorApi {
        override val errorCategory: String = "unauthorized"
        override val isRetryable: Boolean = false
    }

    data class ParsingError(
        val fieldOrContext: String? = null,
        override val userMessage: String = DEFAULT_PARSING_MESSAGE,
        override val cause: Throwable? = null
    ) : ErrorApi {
        override val errorCategory: String = "parsing"
        override val isRetryable: Boolean = false
    }

    data class UnknownError(
        override val userMessage: String = DEFAULT_UNKNOWN_MESSAGE,
        override val cause: Throwable? = null
    ) : ErrorApi {
        override val errorCategory: String = "unknown"
        override val isRetryable: Boolean = false
    }

    fun toException(): ApiResultException = ApiResultException(this)

    companion object {
        const val DEFAULT_NETWORK_MESSAGE =
            "Sin conexión a internet. Verifica tu red e inténtalo de nuevo."
        const val DEFAULT_TIMEOUT_MESSAGE =
            "La conexión tardó demasiado en responder. Inténtalo nuevamente."
        const val DEFAULT_SERVER_MESSAGE =
            "El servidor no está disponible en este momento. Inténtalo en unos minutos."
        const val DEFAULT_CLIENT_MESSAGE =
            "No se pudo procesar la solicitud. Revisa los datos e inténtalo nuevamente."
        const val DEFAULT_UNAUTHORIZED_MESSAGE =
            "Tu sesión expiró o necesitas iniciar sesión para continuar."
        const val DEFAULT_PARSING_MESSAGE =
            "Recibimos una respuesta inesperada del servidor. Inténtalo nuevamente."
        const val DEFAULT_UNKNOWN_MESSAGE =
            "Ocurrió un error inesperado. Inténtalo de nuevo."
    }
}

/**
 * Excepción tipada que envuelve un [ErrorApi] para preservar interoperabilidad con flujos
 * que esperan propagación por excepción sin perder la clasificación semántica del fallo.
 */
class ApiResultException(
    val error: ErrorApi
) : IllegalStateException(error.userMessage, error.cause)

/**
 * Excepción dedicada para señalar errores de contrato/parsing en payloads JSON del backend
 * dentro de bloques `safeApiCall`.
 */
class ApiParsingException(
    message: String,
    val fieldOrContext: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Envoltorio estándar para resultados de la capa de datos (API / Repositorios).
 */
sealed class ResultWrapper<out T> {
    data class Success<out T>(val data: T) : ResultWrapper<T>()
    data class Error(val error: ErrorApi) : ResultWrapper<Nothing>()
    data object Loading : ResultWrapper<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data

    fun errorOrNull(): ErrorApi? = (this as? Error)?.error

    inline fun <R> map(transform: (T) -> R): ResultWrapper<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): ResultWrapper<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (ErrorApi) -> Unit): ResultWrapper<T> {
        if (this is Error) action(error)
        return this
    }

    inline fun getOrElse(fallback: (ErrorApi) -> @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> fallback(error)
        Loading -> fallback(ErrorApi.UnknownError("La operación aún está en curso."))
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw error.toException()
        Loading -> throw IllegalStateException("La operación aún está en curso.")
    }
}

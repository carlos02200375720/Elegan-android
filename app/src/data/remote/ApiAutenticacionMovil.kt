package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

internal data class RegistroRequest(
    val email: String,
    val password: String,
    val name: String
)

internal data class InicioSesionRequest(
    val email: String,
    val password: String
)

internal data class GoogleAuthRequest(
    val idToken: String
)

internal data class UsuarioRespuesta(
    val user: AuthUser
)

internal data class PerfilUsuarioRespuesta(
    val userId: String = "",
    val displayName: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val updatedAt: String? = null
)

internal data class PerfilRespuesta(
    val item: PerfilUsuarioRespuesta
)

internal data class PerfilActualizarRequest(
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val city: String
)

internal data class PerfilActualizadoRespuesta(
    val success: Boolean = false,
    val item: PerfilUsuarioRespuesta
)

internal interface EleganAuthApi {
    // 1. Autenticación (/api/movil/auth)
    @POST("api/movil/auth/register")
    suspend fun register(@Body request: RegistroRequest): AuthResponse

    @POST("api/movil/auth/login")
    suspend fun login(@Body request: InicioSesionRequest): AuthResponse

    @POST("api/movil/auth/google")
    suspend fun loginGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @GET("api/movil/auth/me")
    suspend fun me(@Header("Authorization") authorization: String): UsuarioRespuesta

    @POST("api/movil/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String)

    // 2. Productos (/api/movil/products)
    @GET("api/movil/products")
    suspend fun products(): Map<String, Any?>

    @GET("api/movil/products/{product_id}")
    suspend fun productById(@Path("product_id") productId: String): Map<String, Any?>

    // 3. Anuncios (/api/movil/announcements)
    @GET("api/movil/announcements")
    suspend fun announcements(): Map<String, Any?>

    // 4. Carrito (/api/movil/cart)
    @GET("api/movil/cart")
    suspend fun getCart(@Header("Authorization") authorization: String): Map<String, Any?>

    @POST("api/movil/cart")
    suspend fun addToCart(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @PATCH("api/movil/cart/{item_id}")
    suspend fun updateCartItem(
        @Header("Authorization") authorization: String,
        @Path("item_id") itemId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @DELETE("api/movil/cart/{item_id}")
    suspend fun removeCartItem(
        @Header("Authorization") authorization: String,
        @Path("item_id") itemId: String
    ): Map<String, Any?>

    @DELETE("api/movil/cart")
    suspend fun clearCart(@Header("Authorization") authorization: String): Map<String, Any?>

    // 5. Productos guardados (/api/movil/saved)
    @GET("api/movil/saved")
    suspend fun getSaved(@Header("Authorization") authorization: String): Map<String, Any?>

    @PUT("api/movil/saved/{product_id}")
    suspend fun setSaved(
        @Header("Authorization") authorization: String,
        @Path("product_id") productId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    // 6. Usuario (/api/movil/usuario)
    @GET("api/movil/usuario")
    suspend fun profile(@Header("Authorization") authorization: String): PerfilRespuesta

    @PUT("api/movil/usuario")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: PerfilActualizarRequest
    ): PerfilActualizadoRespuesta

    // 7. Órdenes (/api/movil/orders) — Nota: POST /api/movil/orders devuelve HTTP 410 (usar PayPal)
    @GET("api/movil/orders")
    suspend fun getOrders(@Header("Authorization") authorization: String): Map<String, Any?>

    // 8. Pagos PayPal (/api/movil/payments/paypal)
    @POST("api/movil/payments/paypal/orders")
    suspend fun createPayPalOrder(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @POST("api/movil/payments/paypal/orders/{paypal_order_id}/capture")
    suspend fun capturePayPalOrder(
        @Header("Authorization") authorization: String,
        @Path("paypal_order_id") paypalOrderId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): Map<String, Any?>

    // 9. Mensajes (/api/movil/messages)
    @GET("api/movil/messages")
    suspend fun getMessages(@Header("Authorization") authorization: String): Map<String, Any?>

    @POST("api/movil/messages")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    // 10. Multimedia (/api/movil/media) — Requiere rol admin o superadmin
    @Multipart
    @POST("api/movil/media")
    suspend fun uploadMedia(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part
    ): Map<String, Any?>
}

internal object ApiAutenticacionMovil {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    val service: EleganAuthApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.ELEGAN_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }
}

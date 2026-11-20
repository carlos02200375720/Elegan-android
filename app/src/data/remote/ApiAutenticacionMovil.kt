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

    @DELETE("api/movil/account")
    suspend fun deleteAccount(
        @Header("Authorization") authorization: String
    ): Map<String, @JvmSuppressWildcards Any?>

    @GET("api/movil/products")
    suspend fun products(): Map<String, Any?>

    @GET("api/movil/products/{product_id}")
    suspend fun productById(@Path("product_id") productId: String): Map<String, Any?>

    @GET("api/movil/categories")
    suspend fun categories(): Map<String, Any?>

    @GET("api/movil/announcements")
    suspend fun announcements(): Map<String, Any?>

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

    @GET("api/movil/saved")
    suspend fun getSaved(@Header("Authorization") authorization: String): Map<String, Any?>

    @PUT("api/movil/saved/{product_id}")
    suspend fun setSaved(
        @Header("Authorization") authorization: String,
        @Path("product_id") productId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @GET("api/movil/saved/counts")
    suspend fun getSavedCounts(
        @Header("Authorization") authorization: String?,
        @retrofit2.http.Query("productIds") productIds: List<String>
    ): Map<String, Any?>

    @GET("api/movil/likes")
    suspend fun getLikes(@Header("Authorization") authorization: String): Map<String, Any?>

    @PUT("api/movil/likes/{product_id}")
    suspend fun setLike(
        @Header("Authorization") authorization: String,
        @Path("product_id") productId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @GET("api/movil/likes/counts")
    suspend fun getLikeCounts(
        @Header("Authorization") authorization: String?,
        @retrofit2.http.Query("productIds") productIds: List<String>
    ): Map<String, Any?>

    @POST("api/movil/shares/{product_id}")
    suspend fun recordShare(
        @Header("Authorization") authorization: String?,
        @Path("product_id") productId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?> = emptyMap()
    ): Map<String, Any?>

    @GET("api/movil/shares/counts")
    suspend fun getShareCounts(
        @Header("Authorization") authorization: String?,
        @retrofit2.http.Query("productIds") productIds: List<String>
    ): Map<String, Any?>

    @GET("api/movil/legal-documents")
    suspend fun getLegalDocuments(): Map<String, Any?>

    @POST("api/movil/comments/{comment_id}/report")
    suspend fun reportComment(
        @Header("Authorization") authorization: String,
        @Path("comment_id") commentId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @POST("api/movil/users/{user_id}/block")
    suspend fun blockUser(
        @Header("Authorization") authorization: String,
        @Path("user_id") userId: String
    ): Map<String, Any?>

    @GET("api/movil/comments/{product_id}")
    suspend fun getPublicationComments(
        @Header("Authorization") authorization: String,
        @Path("product_id") productId: String
    ): Map<String, Any?>

    @POST("api/movil/comments/{product_id}")
    suspend fun addPublicationComment(
        @Header("Authorization") authorization: String,
        @Path("product_id") productId: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @GET("api/movil/usuario")
    suspend fun profile(@Header("Authorization") authorization: String): PerfilRespuesta

    @PUT("api/movil/usuario")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: PerfilActualizarRequest
    ): PerfilActualizadoRespuesta

    @GET("api/movil/orders")
    suspend fun getOrders(@Header("Authorization") authorization: String): Map<String, Any?>

    @POST("api/movil/coupons/validate")
    suspend fun validateCoupon(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @GET("api/movil/payments/bank-accounts")
    suspend fun getBankAccounts(@Header("Authorization") authorization: String): Map<String, Any?>

    @POST("api/movil/payments/orders")
    suspend fun createPaymentOrder(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

    @Multipart
    @POST("api/movil/payments/orders/{order_id}/proof")
    suspend fun uploadPaymentProof(
        @Header("Authorization") authorization: String,
        @Path("order_id") orderId: String,
        @Part file: MultipartBody.Part
    ): Map<String, Any?>

    @GET("api/movil/messages")
    suspend fun getMessages(@Header("Authorization") authorization: String): Map<String, Any?>

    @POST("api/movil/messages")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any?>

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

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val service: EleganAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.ELEGAN_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }

    val uploadService: EleganAuthApi by lazy {
        val uploadClient = okHttpClient.newBuilder()
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.ELEGAN_API_BASE_URL)
            .client(uploadClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }
}

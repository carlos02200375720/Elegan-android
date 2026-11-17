package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.JsonObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

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
    // Catálogo público: el backend devuelve { items: [...] } con productos publicados.
    @GET("api/movil/products")
    suspend fun products(): JsonObject

    @POST("api/movil/auth/register")
    suspend fun register(@Body request: RegistroRequest): AuthResponse

    @POST("api/movil/auth/login")
    suspend fun login(@Body request: InicioSesionRequest): AuthResponse

    @POST("api/movil/auth/google")
    suspend fun loginGoogle(@Body request: GoogleAuthRequest): AuthResponse

    @GET("api/movil/auth/me")
    suspend fun me(@Header("Authorization") authorization: String): UsuarioRespuesta

    @GET("api/movil/usuario")
    suspend fun profile(@Header("Authorization") authorization: String): PerfilRespuesta

    @PUT("api/movil/usuario")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: PerfilActualizarRequest
    ): PerfilActualizadoRespuesta

    @POST("api/movil/auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String)
}

internal object ApiAutenticacionMovil {
    val service: EleganAuthApi by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.ELEGAN_API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EleganAuthApi::class.java)
    }
}

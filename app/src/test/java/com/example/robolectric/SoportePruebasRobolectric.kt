package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.SuccessResult
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.GoogleAuthRequest
import com.example.data.remote.InicioSesionRequest
import com.example.data.remote.MobileSession
import com.example.data.remote.PerfilActualizadoRespuesta
import com.example.data.remote.PerfilActualizarRequest
import com.example.data.remote.PerfilRespuesta
import com.example.data.remote.RegistroRequest
import com.example.data.remote.RestablecerContrasenaRequest
import com.example.data.remote.SolicitarRecuperacionRequest
import com.example.data.remote.UsuarioRespuesta
import okhttp3.MultipartBody

internal fun obtenerContextoPrueba(): Context =
    ApplicationProvider.getApplicationContext()

internal fun instalarImageLoaderOfflineParaPruebas(context: Context) {
    val placeholder = BitmapDrawable(
        context.resources,
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    )
    val loader = ImageLoader.Builder(context)
        .components {
            add(
                Interceptor { chain ->
                    SuccessResult(
                        drawable = placeholder,
                        request = chain.request,
                        dataSource = DataSource.MEMORY
                    )
                }
            )
        }
        .build()
    Coil.setImageLoader(loader)
}

internal fun restaurarImageLoaderPruebas() {
    Coil.reset()
}

internal fun limpiarPreferenciasPrueba(context: Context, vararg nombres: String) {
    nombres.forEach { nombre ->
        context.getSharedPreferences(nombre, Context.MODE_PRIVATE).edit().clear().commit()
    }
}

internal fun reiniciarSesionClienteDemo(context: Context) {
    MobileSession.clear(context)
    MobileSession.save(
        context,
        AuthResponse(
            token = "backend_test_token",
            user = AuthUser(
                id = "user-demo-001",
                email = "cliente.elegan@gmail.com",
                name = "Cliente Elegan",
                role = "customer",
                provider = "password"
            )
        )
    )
}

internal open class BaseFakeEleganAuthApi : EleganAuthApi {
    override suspend fun register(request: RegistroRequest): AuthResponse = throw UnsupportedOperationException()
    override suspend fun login(request: InicioSesionRequest): AuthResponse = throw UnsupportedOperationException()
    override suspend fun requestPasswordReset(request: SolicitarRecuperacionRequest): Map<String, Any?> = emptyMap()
    override suspend fun resetPassword(request: RestablecerContrasenaRequest): Map<String, Any?> = emptyMap()
    override suspend fun loginGoogle(request: GoogleAuthRequest): AuthResponse = throw UnsupportedOperationException()
    override suspend fun me(authorization: String): UsuarioRespuesta = throw UnsupportedOperationException()
    override suspend fun logout(authorization: String) = Unit
    override suspend fun deleteAccount(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun products(): Map<String, Any?> = emptyMap()
    override suspend fun productById(productId: String): Map<String, Any?> = emptyMap()
    override suspend fun recommendedProducts(productId: String, limit: Int): Map<String, Any?> = emptyMap()
    override suspend fun categories(): Map<String, Any?> = emptyMap()
    override suspend fun announcements(): Map<String, Any?> = emptyMap()
    override suspend fun getCart(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun addToCart(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun updateCartItem(authorization: String, itemId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun removeCartItem(authorization: String, itemId: String): Map<String, Any?> = emptyMap()
    override suspend fun clearCart(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun getSaved(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun setSaved(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getSavedCounts(authorization: String?, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun getLikes(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun setLike(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getLikeCounts(authorization: String?, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun recordShare(authorization: String?, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getShareCounts(authorization: String?, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun getLegalDocuments(): Map<String, Any?> = emptyMap()
    override suspend fun reportComment(authorization: String, commentId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun blockUser(authorization: String, userId: String): Map<String, Any?> = emptyMap()
    override suspend fun getPublicationComments(authorization: String, productId: String): Map<String, Any?> = emptyMap()
    override suspend fun addPublicationComment(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun profile(authorization: String): PerfilRespuesta = throw UnsupportedOperationException()
    override suspend fun updateProfile(authorization: String, request: PerfilActualizarRequest): PerfilActualizadoRespuesta = throw UnsupportedOperationException()
    override suspend fun getOrders(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun validateCoupon(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getCashOnDeliveryConfig(): Map<String, Any?> = mapOf("enabled" to true, "message" to "")
    override suspend fun getBankAccounts(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun createPaymentOrder(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun uploadPaymentProof(authorization: String, orderId: String, file: MultipartBody.Part): Map<String, Any?> = emptyMap()
    override suspend fun getMessages(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun sendMessage(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun uploadMedia(authorization: String, file: MultipartBody.Part): Map<String, Any?> = emptyMap()
    override suspend fun uploadSupportMedia(authorization: String, file: MultipartBody.Part): Map<String, Any?> = emptyMap()
}

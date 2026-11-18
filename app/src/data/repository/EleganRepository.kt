package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.AnnouncementJson
import com.example.data.model.CartItem
import com.example.data.model.CategoryItem
import com.example.data.model.Order
import com.example.data.model.PayPalOrderSession
import com.example.data.model.ProductJson
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class EleganRepository(context: Context) {
    private val appContext = context.applicationContext

    private val catalogoRepo = RepositorioCatalogo()
    private val categoriasRepo = RepositorioCategorias()
    private val anunciosRepo = RepositorioAnuncios()
    private val mediaRepo = RepositorioMedia(appContext)
    private val carritoRepo = RepositorioCarrito(appContext)
    private val estadisticasRepo = RepositorioEstadisticasPublicaciones()
    private val guardadosYLikesRepo = RepositorioGuardadosYLikes(appContext, estadisticasRepo)
    private val comentariosRepo = RepositorioComentarios(
        appContext = appContext,
        statsRepo = estadisticasRepo
    )
    private val pedidosRepo = RepositorioPedidos(appContext, carritoRepo)
    private val mensajesRepo = RepositorioMensajesSoporte(appContext)
    private val perfilYCuentaRepo = RepositorioPerfilYCuenta(
        appContext = appContext,
        carritoRepo = carritoRepo,
        guardadosYLikesRepo = guardadosYLikesRepo,
        pedidosRepo = pedidosRepo,
        mensajesRepo = mensajesRepo
    )

    fun observeProducts(): Flow<List<ProductJson>> = catalogoRepo.observeProducts()

    fun observeProductsLoading(): Flow<Boolean> = catalogoRepo.observeLoading()

    fun observeProductsError(): Flow<String?> = catalogoRepo.observeError()

    suspend fun refreshProducts(): List<ProductJson> = catalogoRepo.refreshProducts()

    /** Descarga el catálogo publicado desde el backend; conserva cada producto en formato JSON crudo. */
    suspend fun fetchPublishedProducts(): List<ProductJson> = catalogoRepo.fetchProductsFromBackend()

    suspend fun getProductById(productId: String): ProductJson? = catalogoRepo.getProductById(productId)

    fun observeAnnouncements(): Flow<List<AnnouncementJson>> = anunciosRepo.observeAnnouncements()

    suspend fun refreshAnnouncements(): List<AnnouncementJson> = anunciosRepo.refreshAnnouncements()

    fun observeCategories(): Flow<List<CategoryItem>> = categoriasRepo.observeCategories()

    suspend fun loadCategoriesOnce(): List<CategoryItem> = categoriasRepo.loadCategoriesOnce()

    suspend fun refreshCategories(): List<CategoryItem> = categoriasRepo.refreshCategories()

    fun observeCart(): Flow<List<CartItem>> = carritoRepo.observeCart()

    suspend fun refreshCart(): List<CartItem> = carritoRepo.refreshCart()

    suspend fun addToCart(
        product: ProductJson,
        selectedSize: String,
        quantity: Long = 1L,
        selectedColor: String = ""
    ) = carritoRepo.addToCart(product, selectedSize, quantity, selectedColor)

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) =
        carritoRepo.updateCartQuantity(cartItemId, newQuantity)

    suspend fun removeFromCart(cartItemId: String) = carritoRepo.removeFromCart(cartItemId)

    suspend fun clearCart() = carritoRepo.clearCart()

    fun observeSavedProductIds(): Flow<Set<String>> = guardadosYLikesRepo.observeSavedProductIds()

    suspend fun refreshSavedProducts(): Set<String> = guardadosYLikesRepo.refreshSavedProducts()

    suspend fun refreshPublicationLikes(): Set<String> =
        guardadosYLikesRepo.refreshPublicationLikes()

    suspend fun refreshPublicationLikeCounts(productIds: Collection<String>) =
        guardadosYLikesRepo.refreshPublicationLikeCounts(productIds)

    suspend fun setProductSaved(productId: String, isSaved: Boolean) =
        guardadosYLikesRepo.setProductSaved(productId, isSaved)

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> =
        guardadosYLikesRepo.observePublicationStats()

    fun observeLikedPublicationIds(): Flow<Set<String>> =
        guardadosYLikesRepo.observeLikedPublicationIds()

    suspend fun togglePublicationLike(productId: String): Boolean =
        guardadosYLikesRepo.togglePublicationLike(productId)

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> =
        comentariosRepo.observePublicationComments(productId)

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) =
        comentariosRepo.addPublicationComment(productId, displayName, text)

    suspend fun recordPublicationShare(productId: String) =
        guardadosYLikesRepo.recordPublicationShare(productId)

    fun observeOrders(): Flow<List<Order>> = pedidosRepo.observeOrders()

    suspend fun refreshOrders(): List<Order> = pedidosRepo.refreshOrders()

    suspend fun createPayPalOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        cartItems: List<CartItem>
    ): PayPalOrderSession = pedidosRepo.createPayPalOrder(
        customerName = customerName,
        email = email,
        phone = phone,
        address = address,
        city = city,
        total = total,
        itemCount = itemCount,
        itemsSummary = itemsSummary,
        cartItems = cartItems
    )

    suspend fun capturePayPalOrder(
        paypalOrderId: String,
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String
    ): String = pedidosRepo.capturePayPalOrder(
        paypalOrderId = paypalOrderId,
        customerName = customerName,
        email = email,
        phone = phone,
        address = address,
        city = city,
        total = total,
        itemCount = itemCount,
        itemsSummary = itemsSummary
    )

    suspend fun createOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        paymentMethod: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        paymentProofUri: String = ""
    ): String = pedidosRepo.createOrder(
        customerName = customerName,
        email = email,
        phone = phone,
        address = address,
        city = city,
        paymentMethod = paymentMethod,
        total = total,
        itemCount = itemCount,
        itemsSummary = itemsSummary,
        paymentProofUri = paymentProofUri
    )

    fun observeMessages(): Flow<List<SupportMessage>> = mensajesRepo.observeMessages()

    suspend fun refreshMessages(): List<SupportMessage> = mensajesRepo.refreshMessages()

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        scope: CoroutineScope
    ) = mensajesRepo.sendSupportMessage(text, mediaUri, mediaType, scope)

    suspend fun uploadMedia(mediaUri: Uri): String = mediaRepo.uploadMedia(mediaUri)

    fun observeUserProfile(): Flow<UserProfile?> = perfilYCuentaRepo.observeUserProfile()

    suspend fun saveUserProfile(name: String, email: String, phone: String, address: String, city: String) =
        perfilYCuentaRepo.saveUserProfile(name, email, phone, address, city)

    suspend fun deleteUserAccountData() = perfilYCuentaRepo.deleteUserAccountData()
}

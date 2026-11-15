package com.example.data.repository

import android.content.Context
import com.example.data.model.Announcement
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class EleganRepository(context: Context) {
    private val appContext = context.applicationContext

    private val catalogoRepo = RepositorioCatalogo(appContext)
    private val carritoRepo = RepositorioCarrito(appContext)
    private val estadisticasRepo = RepositorioEstadisticasPublicaciones(appContext)
    private val guardadosYLikesRepo = RepositorioGuardadosYLikes(appContext, estadisticasRepo)
    private val comentariosRepo = RepositorioComentarios(
        appContext = appContext,
        statsRepo = estadisticasRepo,
        ensureStatsLoaded = { guardadosYLikesRepo.ensureStatsLoaded() }
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

    fun observeProducts(): Flow<List<Product>> = catalogoRepo.observeProducts()

    suspend fun seedProductsIfEmpty() {
        // El catálogo se alimenta directamente desde el almacenamiento local.
    }

    suspend fun seedAnnouncementsIfEmpty() {
        // Los anuncios se alimentan directamente desde el almacenamiento local.
    }

    suspend fun getProductById(productId: String): Product? = catalogoRepo.getProductById(productId)

    suspend fun addProduct(product: Product): String = catalogoRepo.addProduct(product)

    suspend fun addAnnouncement(announcement: Announcement): String =
        catalogoRepo.addAnnouncement(announcement)

    fun observeAnnouncements(): Flow<List<Announcement>> = catalogoRepo.observeAnnouncements()

    suspend fun deleteProduct(productId: String) = catalogoRepo.deleteProduct(productId)

    suspend fun toggleProductStock(productId: String, inStock: Boolean) =
        catalogoRepo.toggleProductStock(productId, inStock)

    suspend fun deleteAnnouncement(announcementId: String) =
        catalogoRepo.deleteAnnouncement(announcementId)

    suspend fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) =
        catalogoRepo.toggleAnnouncementActive(announcementId, isActive)

    fun getInitialSampleAnnouncements(): List<Announcement> = anunciosInicialesTienda()

    fun getInitialSampleProducts(): List<Product> = todosProductosIniciales()

    fun observeCart(): Flow<List<CartItem>> = carritoRepo.observeCart()

    suspend fun addToCart(
        product: Product,
        selectedSize: String,
        quantity: Long = 1L,
        selectedColor: String = ""
    ) = carritoRepo.addToCart(product, selectedSize, quantity, selectedColor)

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) =
        carritoRepo.updateCartQuantity(cartItemId, newQuantity)

    suspend fun removeFromCart(cartItemId: String) = carritoRepo.removeFromCart(cartItemId)

    suspend fun clearCart() = carritoRepo.clearCart()

    fun observeSavedProductIds(): Flow<Set<String>> = guardadosYLikesRepo.observeSavedProductIds()

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

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        scope: CoroutineScope
    ) = mensajesRepo.sendSupportMessage(text, mediaUri, mediaType, scope)

    fun observeUserProfile(): Flow<UserProfile?> = perfilYCuentaRepo.observeUserProfile()

    suspend fun saveUserProfile(name: String, email: String, phone: String, address: String, city: String) =
        perfilYCuentaRepo.saveUserProfile(name, email, phone, address, city)

    suspend fun deleteUserAccountData() = perfilYCuentaRepo.deleteUserAccountData()
}

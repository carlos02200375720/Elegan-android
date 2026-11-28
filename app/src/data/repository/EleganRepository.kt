package com.example.data.repository

import android.content.Context
import android.net.Uri
import androidx.paging.PagingData
import com.example.data.error.ErrorApi
import com.example.data.error.ResultWrapper
import com.example.data.local.EleganDatabase
import com.example.data.model.Announcement
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.CashOnDeliveryConfig
import com.example.data.model.CategoryItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession
import com.example.data.work.ProgramadorSincronizacionProductos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

class EleganRepository internal constructor(
    private val appContext: Context,
    private val catalogoRepo: RepositorioCatalogo,
    private val categoriasRepo: RepositorioCategorias,
    private val anunciosRepo: RepositorioAnuncios,
    private val mediaRepo: RepositorioMedia,
    private val carritoRepo: RepositorioCarrito,
    private val estadisticasRepo: RepositorioEstadisticasPublicaciones,
    private val guardadosYLikesRepo: RepositorioGuardadosYLikes,
    private val comentariosRepo: RepositorioComentarios,
    private val pedidosRepo: RepositorioPedidos,
    private val mensajesRepo: RepositorioMensajesSoporte,
    private val perfilYCuentaRepo: RepositorioPerfilYCuenta
) {
    constructor(context: Context) : this(
        appContext = context.applicationContext,
        catalogoRepo = RepositorioCatalogo(
            databaseProvider = { EleganDatabase.getInstance(context.applicationContext) }
        ),
        categoriasRepo = RepositorioCategorias(),
        anunciosRepo = RepositorioAnuncios(),
        mediaRepo = RepositorioMedia(context.applicationContext),
        carritoRepo = RepositorioCarrito(context.applicationContext),
        estadisticasRepo = RepositorioEstadisticasPublicaciones()
    )

    private constructor(
        appContext: Context,
        catalogoRepo: RepositorioCatalogo,
        categoriasRepo: RepositorioCategorias,
        anunciosRepo: RepositorioAnuncios,
        mediaRepo: RepositorioMedia,
        carritoRepo: RepositorioCarrito,
        estadisticasRepo: RepositorioEstadisticasPublicaciones,
        guardadosYLikesRepo: RepositorioGuardadosYLikes = RepositorioGuardadosYLikes(appContext, estadisticasRepo),
        comentariosRepo: RepositorioComentarios = RepositorioComentarios(appContext = appContext, statsRepo = estadisticasRepo),
        pedidosRepo: RepositorioPedidos = RepositorioPedidos(appContext, carritoRepo),
        mensajesRepo: RepositorioMensajesSoporte = RepositorioMensajesSoporte(appContext)
    ) : this(
        appContext = appContext,
        catalogoRepo = catalogoRepo,
        categoriasRepo = categoriasRepo,
        anunciosRepo = anunciosRepo,
        mediaRepo = mediaRepo,
        carritoRepo = carritoRepo,
        estadisticasRepo = estadisticasRepo,
        guardadosYLikesRepo = guardadosYLikesRepo,
        comentariosRepo = comentariosRepo,
        pedidosRepo = pedidosRepo,
        mensajesRepo = mensajesRepo,
        perfilYCuentaRepo = RepositorioPerfilYCuenta(
            appContext = appContext,
            carritoRepo = carritoRepo,
            guardadosYLikesRepo = guardadosYLikesRepo,
            pedidosRepo = pedidosRepo,
            mensajesRepo = mensajesRepo
        )
    )

    fun observeProducts(): Flow<List<Product>> = catalogoRepo.observeProducts()

    fun observePagedProducts(
        category: String? = null,
        query: String? = null,
        inStockOnly: Boolean = true,
        pageSize: Int = ProductosPagingSource.DEFAULT_PAGE_SIZE,
        prefetchDistance: Int = ProductosPagingSource.DEFAULT_PREFETCH_DISTANCE,
        maxSize: Int = ProductosPagingSource.DEFAULT_MAX_MEMORY_ITEMS
    ): Flow<PagingData<Product>> = catalogoRepo.observePagedProducts(
        category = category,
        query = query,
        inStockOnly = inStockOnly,
        pageSize = pageSize,
        prefetchDistance = prefetchDistance,
        maxSize = maxSize
    )

    fun observeProductsLoading(): Flow<Boolean> = catalogoRepo.observeLoading()

    fun observeProductsApiError(): Flow<ErrorApi?> = catalogoRepo.observeApiError()

    fun observeProductsError(): Flow<String?> = catalogoRepo.observeError()

    suspend fun refreshProductsResult(): ResultWrapper<List<Product>> = catalogoRepo.refreshProductsResult()

    suspend fun refreshProducts(): List<Product> = catalogoRepo.refreshProducts()

    fun scheduleBackgroundCatalogSync(force: Boolean = false): Boolean =
        ProgramadorSincronizacionProductos.solicitarSincronizacionInmediata(
            context = appContext,
            forceReplace = force
        )

    fun observeBackgroundCatalogSync(): Flow<Boolean> =
        ProgramadorSincronizacionProductos.observarSincronizacionActiva(appContext)

    /** Descarga el catálogo publicado desde el backend como modelos inmutables [Product]. */
    suspend fun fetchPublishedProducts(): List<Product> = catalogoRepo.fetchProductsFromBackend()

    suspend fun getProductByIdResult(productId: String): ResultWrapper<Product> =
        catalogoRepo.getProductByIdResult(productId)

    suspend fun getProductById(productId: String): Product? = catalogoRepo.getProductById(productId)

    suspend fun getRecommendedProductsResult(productId: String, limit: Int = 12): ResultWrapper<List<Product>> =
        catalogoRepo.getRecommendedProductsResult(productId, limit)

    suspend fun getRecommendedProducts(productId: String, limit: Int = 12): List<Product> =
        catalogoRepo.getRecommendedProducts(productId, limit)

    fun observeAnnouncements(): Flow<List<Announcement>> = anunciosRepo.observeAnnouncements()

    suspend fun refreshAnnouncementsResult(): ResultWrapper<List<Announcement>> =
        anunciosRepo.refreshAnnouncementsResult()

    suspend fun refreshAnnouncements(): List<Announcement> = anunciosRepo.refreshAnnouncements()

    fun observeCategories(): Flow<List<CategoryItem>> = categoriasRepo.observeCategories()

    suspend fun fetchCategoriesResult(force: Boolean = true): ResultWrapper<List<CategoryItem>> =
        categoriasRepo.fetchCategoriesResult(force)

    suspend fun loadCategoriesOnce(): List<CategoryItem> = categoriasRepo.loadCategoriesOnce()

    suspend fun refreshCategories(): List<CategoryItem> = categoriasRepo.refreshCategories()

    fun observeCart(): Flow<List<CartItem>> = carritoRepo.observeCart()

    fun observeCartApiError(): Flow<ErrorApi?> = carritoRepo.observeCartApiError()

    fun observeCartError(): Flow<String?> = carritoRepo.observeCartError()

    fun clearCartError() = carritoRepo.clearCartError()

    suspend fun refreshCartResult(): ResultWrapper<List<CartItem>> = carritoRepo.refreshCartResult()

    suspend fun refreshCart(): List<CartItem> = carritoRepo.refreshCart()

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

    suspend fun refreshSavedProducts(): Set<String> = guardadosYLikesRepo.refreshSavedProducts()

    suspend fun refreshPublicationLikes(): Set<String> =
        guardadosYLikesRepo.refreshPublicationLikes()

    suspend fun refreshPublicationLikeCounts(productIds: Collection<String>) =
        guardadosYLikesRepo.refreshPublicationLikeCounts(productIds)

    suspend fun refreshPublicationCounts(productIds: Collection<String>) =
        guardadosYLikesRepo.refreshPublicationCounts(productIds)

    fun syncPublicationCountsFromProducts(products: List<Product>) =
        guardadosYLikesRepo.syncCountsFromProducts(products)

    suspend fun setProductSaved(productId: String, isSaved: Boolean): Boolean =
        guardadosYLikesRepo.setProductSaved(productId, isSaved)

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> =
        guardadosYLikesRepo.observePublicationStats()

    fun observeLikedPublicationIds(): Flow<Set<String>> =
        guardadosYLikesRepo.observeLikedPublicationIds()

    suspend fun togglePublicationLike(productId: String): Boolean =
        guardadosYLikesRepo.togglePublicationLike(productId)

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> =
        comentariosRepo.observePublicationComments(productId)

    suspend fun refreshPublicationComments(productId: String): List<PublicationComment> =
        comentariosRepo.refreshPublicationComments(productId)

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) =
        comentariosRepo.addPublicationComment(productId, displayName, text)

    suspend fun reportPublicationComment(
        commentId: String,
        reason: String = "Contenido inapropiado"
    ) = comentariosRepo.reportComment(commentId, reason)

    suspend fun blockUser(userId: String) = comentariosRepo.blockUser(userId)

    suspend fun recordPublicationShare(productId: String): Long =
        guardadosYLikesRepo.recordPublicationShare(productId)

    suspend fun flushPendingVisitorShares(): Int =
        guardadosYLikesRepo.flushPendingVisitorShares()

    fun startPendingShareSync(scope: CoroutineScope) =
        guardadosYLikesRepo.startPendingShareSync(scope)

    fun observeOrders(): Flow<List<Order>> = pedidosRepo.observeOrders()

    fun observeOrdersApiError(): Flow<ErrorApi?> = pedidosRepo.observeOrdersApiError()

    suspend fun refreshOrdersResult(): ResultWrapper<List<Order>> = pedidosRepo.refreshOrdersResult()

    suspend fun refreshOrders(): List<Order> = pedidosRepo.refreshOrders()

    suspend fun validateCoupon(code: String, selectedCartItemIds: List<String>): Map<String, Any?> =
        pedidosRepo.validateCoupon(code, selectedCartItemIds)

    suspend fun getCashOnDeliveryConfig(): CashOnDeliveryConfig = pedidosRepo.getCashOnDeliveryConfig()

    suspend fun getBankAccounts(): List<BankAccountInfo> = pedidosRepo.getBankAccounts()

    suspend fun createPaymentOrder(
        paymentMethod: String, bankAccountId: String, cashOnDeliveryAccepted: Boolean,
        customerName: String, email: String, phone: String, address: String, city: String,
        couponCode: String, selectedCartItemIds: List<String>
    ): String = pedidosRepo.createPaymentOrder(
        paymentMethod, bankAccountId, cashOnDeliveryAccepted,
        customerName, email, phone, address, city, couponCode, selectedCartItemIds
    )

    suspend fun uploadPaymentProof(orderId: String, mediaUri: Uri): String =
        mediaRepo.uploadPaymentProof(orderId, mediaUri)

    suspend fun saveConfirmedPaymentProofPreview(mediaUri: Uri): String? =
        mediaRepo.saveConfirmedPaymentProofPreview(mediaUri)

    fun clearConfirmedPaymentProofPreview() =
        mediaRepo.clearConfirmedPaymentProofPreview()

    fun observeMessages(): Flow<List<SupportMessage>> = mensajesRepo.observeMessages()

    suspend fun refreshMessages(): List<SupportMessage> = mensajesRepo.refreshMessages()

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        scope: CoroutineScope
    ) = mensajesRepo.sendSupportMessage(text, mediaUri, mediaType, scope)

    suspend fun uploadMedia(mediaUri: Uri): String = mediaRepo.uploadMedia(mediaUri)

    suspend fun uploadSupportAttachment(mediaUri: Uri): String =
        mediaRepo.uploadSupportAttachment(mediaUri)

    fun observeUserProfile(): Flow<UserProfile?> = perfilYCuentaRepo.observeUserProfile()

    suspend fun refreshUserProfile() = perfilYCuentaRepo.refreshUserProfile()

    suspend fun saveUserProfile(name: String, email: String, phone: String, address: String, city: String) =
        perfilYCuentaRepo.saveUserProfile(name, email, phone, address, city)

    fun currentSessionUser(): AuthUser? = MobileSession.user(appContext)

    fun currentSessionUserId(): String? = MobileSession.userId(appContext)

    suspend fun clearUserSessionData() = perfilYCuentaRepo.clearUserSessionData()

    suspend fun deleteUserAccountData() = perfilYCuentaRepo.deleteUserAccountData()
}

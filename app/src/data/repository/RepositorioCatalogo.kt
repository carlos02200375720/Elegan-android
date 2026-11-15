package com.example.data.repository

import android.content.Context
import com.example.data.model.Announcement
import com.example.data.model.LocalBoutiqueMedia
import com.example.data.model.Product
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

internal class RepositorioCatalogo(private val appContext: Context) {

    private val publicationStatePrefs by lazy {
        appContext.getSharedPreferences("elegan_publication_states", Context.MODE_PRIVATE)
    }
    private val catalogStorePrefs by lazy {
        appContext.getSharedPreferences("elegan_catalog_store", Context.MODE_PRIVATE)
    }

    private val productStockOverridesFlow by lazy {
        MutableStateFlow(loadBooleanOverrideMap(publicationStatePrefs, "product_stock_overrides"))
    }
    private val announcementActiveOverridesFlow by lazy {
        MutableStateFlow(loadBooleanOverrideMap(publicationStatePrefs, "announcement_active_overrides"))
    }
    private val customProductsFlow by lazy {
        MutableStateFlow(leerProductosPersonalizados(catalogStorePrefs))
    }
    private val deletedProductIdsFlow by lazy {
        MutableStateFlow(loadPersistedIdSet(catalogStorePrefs, "deleted_product_ids"))
    }
    private val customAnnouncementsFlow by lazy {
        MutableStateFlow(leerAnunciosPersonalizados(catalogStorePrefs))
    }
    private val deletedAnnouncementIdsFlow by lazy {
        MutableStateFlow(loadPersistedIdSet(catalogStorePrefs, "deleted_announcement_ids"))
    }

    private fun mergedProducts(
        custom: List<Product>,
        deletedIds: Set<String>,
        overrides: Map<String, Boolean>
    ): List<Product> =
        (custom + todosProductosIniciales())
            .distinctBy { it.id }
            .filter { it.id !in deletedIds }
            .map { p -> overrides[p.id]?.let { inStock -> p.copy(inStock = inStock) } ?: p }

    fun observeProducts(): Flow<List<Product>> =
        combine(
            customProductsFlow,
            deletedProductIdsFlow,
            productStockOverridesFlow
        ) { custom, deletedIds, overrides ->
            mergedProducts(custom, deletedIds, overrides)
        }.flowOn(Dispatchers.IO)

    fun observeAnnouncements(): Flow<List<Announcement>> =
        combine(
            customAnnouncementsFlow,
            deletedAnnouncementIdsFlow,
            announcementActiveOverridesFlow
        ) { custom, deletedIds, overrides ->
            (custom + anunciosInicialesTienda())
                .distinctBy { it.id }
                .filter { it.id !in deletedIds }
                .map { ann -> overrides[ann.id]?.let { active -> ann.copy(isActive = active) } ?: ann }
        }.flowOn(Dispatchers.IO)

    fun getProductById(productId: String): Product? =
        mergedProducts(
            customProductsFlow.value,
            deletedProductIdsFlow.value,
            productStockOverridesFlow.value
        ).find { it.id == productId }

    suspend fun addProduct(product: Product): String {
        val productId = product.id.ifBlank { "prod_${UUID.randomUUID()}" }
        val category = product.category.ifBlank { "camisa" }
        val fallback = LocalBoutiqueMedia.fallbackImageForCategory(category)
        val sourceVideo = product.videoUrl.takeIf { product.isVideo }.orEmpty()
        val coverSource = if (product.isVideo) {
            if (product.imageUrl.isNotBlank() && product.imageUrl != sourceVideo) product.imageUrl else fallback
        } else {
            product.imageUrl.ifBlank { product.images.firstOrNull() ?: fallback }
        }

        val savedVideoUrl = if (product.isVideo && sourceVideo.isNotBlank()) {
            persistirArchivoMultimediaLocal(appContext, sourceVideo, productId, "video")
        } else ""
        val savedCoverUrl = persistirArchivoMultimediaLocal(appContext, coverSource, productId, "cover")
            .ifBlank { fallback }
        val gallery = (product.images + listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() }))
            .distinct()
            .filterNot { it == coverSource || it == sourceVideo }
            .mapIndexed { idx, uri -> persistirArchivoMultimediaLocal(appContext, uri, productId, "gallery_$idx") }
            .filter { it.isNotBlank() }

        val finalProduct = product.copy(
            id = productId,
            category = category,
            imageUrl = savedCoverUrl,
            images = (listOf(savedCoverUrl) + gallery).distinct(),
            videoUrl = savedVideoUrl,
            isVideo = product.isVideo || savedVideoUrl.isNotBlank(),
            createdAt = product.createdAt ?: nowIsoUtc()
        )

        withContext(Dispatchers.IO) {
            val updated = listOf(finalProduct) + customProductsFlow.value.filterNot { it.id == productId }
            customProductsFlow.value = updated
            guardarProductosPersonalizados(catalogStorePrefs, updated)
            if (productId in deletedProductIdsFlow.value) {
                val updatedDeleted = deletedProductIdsFlow.value - productId
                deletedProductIdsFlow.value = updatedDeleted
                savePersistedIdSet(catalogStorePrefs, "deleted_product_ids", updatedDeleted)
            }
        }
        return productId
    }

    suspend fun addAnnouncement(announcement: Announcement): String {
        val id = announcement.id.ifBlank { "ann_${UUID.randomUUID()}" }
        val image = if (announcement.imageUrl.isNotBlank()) {
            persistirArchivoMultimediaLocal(appContext, announcement.imageUrl, id, "announcement_img")
        } else {
            LocalBoutiqueMedia.fallbackImageForCategory(announcement.targetCategory)
        }
        val video = if (announcement.videoUrl.isNotBlank()) {
            persistirArchivoMultimediaLocal(appContext, announcement.videoUrl, id, "announcement_video")
        } else ""

        val finalAnnouncement = announcement.copy(
            id = id,
            imageUrl = image,
            videoUrl = video,
            isVideo = announcement.hasVideo || video.isNotBlank(),
            createdAt = announcement.createdAt ?: nowIsoUtc()
        )

        withContext(Dispatchers.IO) {
            val updated = listOf(finalAnnouncement) + customAnnouncementsFlow.value.filterNot { it.id == id }
            customAnnouncementsFlow.value = updated
            guardarAnunciosPersonalizados(catalogStorePrefs, updated)
            if (id in deletedAnnouncementIdsFlow.value) {
                val updatedDeleted = deletedAnnouncementIdsFlow.value - id
                deletedAnnouncementIdsFlow.value = updatedDeleted
                savePersistedIdSet(catalogStorePrefs, "deleted_announcement_ids", updatedDeleted)
            }
        }
        return id
    }

    suspend fun deleteProduct(productId: String) {
        withContext(Dispatchers.IO) {
            val updatedCustom = customProductsFlow.value.filterNot { it.id == productId }
            customProductsFlow.value = updatedCustom
            guardarProductosPersonalizados(catalogStorePrefs, updatedCustom)

            val updatedDeleted = deletedProductIdsFlow.value + productId
            deletedProductIdsFlow.value = updatedDeleted
            savePersistedIdSet(catalogStorePrefs, "deleted_product_ids", updatedDeleted)
        }
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        val updated = productStockOverridesFlow.value + (productId to inStock)
        productStockOverridesFlow.value = updated
        withContext(Dispatchers.IO) {
            saveBooleanOverrideMap(publicationStatePrefs, "product_stock_overrides", updated)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        withContext(Dispatchers.IO) {
            val updatedCustom = customAnnouncementsFlow.value.filterNot { it.id == announcementId }
            customAnnouncementsFlow.value = updatedCustom
            guardarAnunciosPersonalizados(catalogStorePrefs, updatedCustom)

            val updatedDeleted = deletedAnnouncementIdsFlow.value + announcementId
            deletedAnnouncementIdsFlow.value = updatedDeleted
            savePersistedIdSet(catalogStorePrefs, "deleted_announcement_ids", updatedDeleted)
        }
    }

    suspend fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        val updated = announcementActiveOverridesFlow.value + (announcementId to isActive)
        announcementActiveOverridesFlow.value = updated
        withContext(Dispatchers.IO) {
            saveBooleanOverrideMap(publicationStatePrefs, "announcement_active_overrides", updated)
        }
    }
}

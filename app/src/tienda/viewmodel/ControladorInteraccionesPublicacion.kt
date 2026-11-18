package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.ProductJson
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.productId
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ControladorInteraccionesPublicacion(
    private val repository: EleganRepository,
    products: StateFlow<List<ProductJson>>,
    private val scope: CoroutineScope
) {
    val savedProductIds: StateFlow<Set<String>> = repository.observeSavedProductIds()
        .catch { error ->
            Log.e("EleganViewModel", "No se pudieron cargar las publicaciones guardadas.", error)
            emit(emptySet())
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptySet())

    val savedProducts: StateFlow<List<ProductJson>> = combine(
        products,
        savedProductIds
    ) { allProducts, savedIds ->
        allProducts.filter { it.productId() in savedIds }
    }.stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    val likedPublicationIds: StateFlow<Set<String>> = repository.observeLikedPublicationIds()
        .catch { error ->
            Log.e("EleganViewModel", "No se pudieron cargar los likes de publicaciones.", error)
            emit(emptySet())
        }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptySet())

    val publicationStats: StateFlow<Map<String, PublicationStats>> = combine(
        repository.observePublicationStats(),
        savedProductIds,
        likedPublicationIds
    ) { statsMap, savedIds, likedIds ->
        if (savedIds.isEmpty() && likedIds.isEmpty()) {
            statsMap
        } else {
            val adjusted = statsMap.toMutableMap()
            savedIds.forEach { productId ->
                val existing = adjusted[productId] ?: PublicationStats(id = productId)
                if (existing.savesCount < 1L) adjusted[productId] = existing.copy(savesCount = 1L)
            }
            likedIds.forEach { productId ->
                val existing = adjusted[productId] ?: PublicationStats(id = productId)
                if (existing.likesCount < 1L) adjusted[productId] = existing.copy(likesCount = 1L)
            }
            adjusted
        }
    }.catch { error ->
        Log.e("EleganViewModel", "No se pudieron cargar las estadísticas de publicaciones.", error)
        emit(emptyMap())
    }.stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyMap())

    private val currentProductIds = MutableStateFlow<List<String>>(emptyList())

    init {
        // El flujo de productos cambia al cargar/actualizar el catálogo, no al desplazar el feed.
        scope.launch {
            products
                .collect { list ->
                    repository.syncPublicationCountsFromProducts(list)
                }
        }
        scope.launch {
            products
                .map { list -> list.map { it.productId() }.filter { it.isNotBlank() }.distinct() }
                .distinctUntilChanged()
                .collect { ids ->
                    currentProductIds.value = ids
                    if (ids.isNotEmpty()) repository.refreshPublicationCounts(ids)
                }
        }
    }

    private val _commentsProductId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val publicationComments: StateFlow<List<PublicationComment>> = _commentsProductId
        .flatMapLatest { productId ->
            if (productId == null) flowOf(emptyList())
            else repository.observePublicationComments(productId)
        }
        .catch { error ->
            Log.e("EleganViewModel", "No se pudieron cargar los comentarios de la publicación.", error)
            emit(emptyList())
        }
        .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    fun refreshSavedProducts() {
        scope.launch {
            try {
                repository.refreshSavedProducts()
                repository.refreshPublicationLikes()
                val ids = currentProductIds.value
                if (ids.isNotEmpty()) {
                    repository.refreshPublicationCounts(ids)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudieron sincronizar los guardados.", e)
            }
        }
    }

    /**
     * Carga conteos en una sola petición para el conjunto de publicaciones indicado.
     * No se ejecuta desde la composición ni durante el desplazamiento del feed.
     */
    fun refreshPublicationLikeCounts(productIds: Collection<String>) {
        if (productIds.isEmpty()) return
        scope.launch {
            try {
                repository.refreshPublicationCounts(productIds)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("EleganViewModel", "No se pudieron sincronizar los conteos de interacciones.", e)
            }
        }
    }

    fun toggleSaveProduct(productId: String, onResult: (Result<Boolean>) -> Unit) {
        val shouldSave = productId !in savedProductIds.value
        scope.launch {
            try {
                val confirmedSaved = repository.setProductSaved(productId, shouldSave)
                onResult(Result.success(confirmedSaved))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo actualizar la publicación guardada.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun togglePublicationLike(productId: String, onResult: (Result<Boolean>) -> Unit) {
        scope.launch {
            try {
                onResult(Result.success(repository.togglePublicationLike(productId)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo actualizar el like de la publicación.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun openPublicationComments(productId: String?) {
        _commentsProductId.value = productId
    }

    fun addPublicationComment(
        productId: String,
        displayName: String,
        text: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        scope.launch {
            try {
                repository.addPublicationComment(productId, displayName, text)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo publicar el comentario.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun recordPublicationShare(productId: String, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            try {
                repository.recordPublicationShare(productId)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo registrar el compartido de la publicación.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun setProductSaved(productId: String, isSaved: Boolean, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            try {
                repository.setProductSaved(productId, isSaved)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo actualizar la publicación guardada.", e)
                onResult(Result.failure(e))
            }
        }
    }
}

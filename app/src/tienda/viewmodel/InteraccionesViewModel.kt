package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.productId
import com.example.data.observability.MonitorErrores
import com.example.data.repository.EleganRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel dedicado a las interacciones sociales de publicaciones:
 * likes, guardados, compartidos, conteos visibles bajo demanda, comentarios, reportes y bloqueos.
 * Se alimenta directamente de la fuente única de verdad en [EleganRepository].
 */
class InteraccionesViewModel(
    private val repository: EleganRepository,
    autoSync: Boolean = true,
    private val countsTtlMs: Long = 30_000L,
    private val maxVisibleCountsWindow: Int = 24,
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val observedProducts = repository.observeProducts()

    val savedProductIds: StateFlow<Set<String>> = repository.observeSavedProductIds()
        .catch { error ->
            MonitorErrores.registrarError("EleganInteracciones", "No se pudieron cargar las publicaciones guardadas.", error)
            emit(emptySet())
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptySet())

    val savedProducts: StateFlow<List<Product>> = combine(
        observedProducts,
        savedProductIds
    ) { allProducts, savedIds ->
        if (savedIds.isEmpty()) emptyList() else allProducts.filter { it.productId() in savedIds }
    }
        .flowOn(computationDispatcher)
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    val likedPublicationIds: StateFlow<Set<String>> = repository.observeLikedPublicationIds()
        .catch { error ->
            MonitorErrores.registrarError("EleganInteracciones", "No se pudieron cargar los likes de publicaciones.", error)
            emit(emptySet())
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = emptySet())

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
    }
        .flowOn(computationDispatcher)
        .catch { error ->
            MonitorErrores.registrarError("EleganInteracciones", "No se pudieron cargar las estadísticas de publicaciones.", error)
            emit(emptyMap())
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyMap())

    private val visibleProductIds = MutableStateFlow<List<String>>(emptyList())
    private val countsFetchedAtMs = ConcurrentHashMap<String, Long>()
    private val _commentsProductId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val publicationComments: StateFlow<List<PublicationComment>> = _commentsProductId
        .flatMapLatest { productId ->
            if (productId == null) flowOf(emptyList())
            else repository.observePublicationComments(productId)
        }
        .catch { error ->
            MonitorErrores.registrarError("EleganInteracciones", "No se pudieron cargar los comentarios de la publicación.", error)
            emit(emptyList())
        }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    init {
        if (autoSync) {
            repository.startPendingShareSync(viewModelScope)
            viewModelScope.launch {
                observedProducts.collect { list ->
                    withContext(computationDispatcher) {
                        repository.syncPublicationCountsFromProducts(list)
                    }
                }
            }
            refreshInteractions()
        }
    }

    /**
     * Registra la ventana actual de publicaciones visibles en el feed y consulta conteos remotos
     * únicamente para los IDs vencidos según [countsTtlMs], evitando descargas masivas del catálogo.
     */
    fun onVisibleProductsChanged(productIds: List<String>) {
        val normalized = productIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(maxVisibleCountsWindow.coerceAtLeast(1))
            .toList()
        visibleProductIds.value = normalized
        if (normalized.isEmpty()) return

        val now = nowProvider()
        val staleIds = normalized.filter { id ->
            val lastFetched = countsFetchedAtMs[id]
            lastFetched == null || (now - lastFetched) >= countsTtlMs
        }
        if (staleIds.isEmpty()) return

        staleIds.forEach { countsFetchedAtMs[it] = now }
        viewModelScope.launch {
            try {
                repository.refreshPublicationCounts(staleIds)
            } catch (e: CancellationException) {
                staleIds.forEach { countsFetchedAtMs.remove(it, now) }
                throw e
            } catch (e: Exception) {
                staleIds.forEach { countsFetchedAtMs.remove(it, now) }
                MonitorErrores.registrarAdvertencia(
                    "EleganInteracciones",
                    "No se pudieron sincronizar los conteos de las publicaciones visibles.",
                    e
                )
            }
        }
    }

    fun refreshInteractions(forceVisibleCounts: Boolean = false) {
        viewModelScope.launch {
            try {
                repository.refreshSavedProducts()
                repository.refreshPublicationLikes()
                val visibleIds = visibleProductIds.value
                if (visibleIds.isNotEmpty()) {
                    val now = nowProvider()
                    val targetIds = if (forceVisibleCounts) {
                        visibleIds
                    } else {
                        visibleIds.filter { id ->
                            val lastFetched = countsFetchedAtMs[id]
                            lastFetched == null || (now - lastFetched) >= countsTtlMs
                        }
                    }
                    if (targetIds.isNotEmpty()) {
                        targetIds.forEach { countsFetchedAtMs[it] = now }
                        repository.refreshPublicationCounts(targetIds)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganInteracciones", "No se pudieron sincronizar los guardados.", e)
            }
        }
    }

    fun refreshPublicationLikeCounts(productIds: Collection<String>) {
        val normalized = productIds
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(maxVisibleCountsWindow.coerceAtLeast(1))
            .toList()
        if (normalized.isEmpty()) return
        val now = nowProvider()
        normalized.forEach { countsFetchedAtMs[it] = now }
        viewModelScope.launch {
            try {
                repository.refreshPublicationCounts(normalized)
            } catch (e: CancellationException) {
                normalized.forEach { countsFetchedAtMs.remove(it, now) }
                throw e
            } catch (e: Exception) {
                normalized.forEach { countsFetchedAtMs.remove(it, now) }
                MonitorErrores.registrarAdvertencia(
                    "EleganInteracciones",
                    "No se pudieron sincronizar los conteos de interacciones.",
                    e
                )
            }
        }
    }

    fun toggleSaveProduct(productId: String, onResult: (Result<Boolean>) -> Unit = {}) {
        val shouldSave = productId !in savedProductIds.value
        viewModelScope.launch {
            try {
                val confirmedSaved = repository.setProductSaved(productId, shouldSave)
                onResult(Result.success(confirmedSaved))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganInteracciones", "No se pudo actualizar la publicación guardada.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun setProductSaved(productId: String, isSaved: Boolean, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.setProductSaved(productId, isSaved)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganInteracciones", "No se pudo actualizar la publicación guardada.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun togglePublicationLike(productId: String, onResult: (Result<Boolean>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                onResult(Result.success(repository.togglePublicationLike(productId)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganInteracciones", "No se pudo actualizar el like de la publicación.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun openPublicationComments(productId: String?) {
        _commentsProductId.value = productId
    }

    fun refreshCurrentPublicationComments() {
        val productId = _commentsProductId.value ?: return
        viewModelScope.launch {
            try {
                repository.refreshPublicationComments(productId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarAdvertencia(
                    "EleganComments",
                    "No se pudieron actualizar los comentarios en tiempo real.",
                    e
                )
            }
        }
    }

    fun addPublicationComment(
        productId: String,
        displayName: String,
        text: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.addPublicationComment(productId, displayName, text)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganInteracciones", "No se pudo publicar el comentario.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun reportPublicationComment(
        commentId: String,
        reason: String = "Contenido inapropiado",
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.reportPublicationComment(commentId, reason)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarAdvertencia(
                    "EleganComments",
                    "No se pudo enviar el reporte del comentario.",
                    e
                )
                onResult(Result.failure(e))
            }
        }
    }

    fun blockUser(
        userId: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.blockUser(userId)
                refreshCurrentPublicationComments()
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarAdvertencia(
                    "EleganComments",
                    "No se pudo bloquear al usuario.",
                    e
                )
                onResult(Result.failure(e))
            }
        }
    }

    fun recordPublicationShare(productId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.recordPublicationShare(productId)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError(
                    "EleganInteracciones",
                    "No se pudo registrar el compartido de la publicación.",
                    e
                )
                onResult(Result.failure(e))
            }
        }
    }
}

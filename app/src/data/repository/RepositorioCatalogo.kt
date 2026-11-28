package com.example.data.repository

import com.example.data.model.Product
import com.example.data.model.toProduct
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioCatalogo {

    private val fetchMutex = Mutex()
    private val productsFlow = MutableStateFlow<List<Product>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    fun observeProducts(): Flow<List<Product>> = productsFlow.asStateFlow()

    fun observeLoading(): Flow<Boolean> = isLoadingFlow.asStateFlow()

    fun observeError(): Flow<String?> = errorFlow.asStateFlow()

    suspend fun refreshProducts(): List<Product> = fetchMutex.withLock {
        isLoadingFlow.value = true
        errorFlow.value = null
        try {
            val remoteProducts = fetchProductsFromBackend()
            productsFlow.value = remoteProducts
            remoteProducts
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarError("EleganCatalog", "Error al obtener productos desde el backend.", e)
            errorFlow.value = e.localizedMessage ?: "No se pudo cargar el catálogo desde el servidor."
            emptyList()
        } finally {
            isLoadingFlow.value = false
        }
    }

    suspend fun fetchProductsFromBackend(): List<Product> = withContext(Dispatchers.IO) {
        val response = ApiAutenticacionMovil.service.products()
        val items = extractJsonItems(response)
        productsFlow.value = items
        items
    }

    suspend fun getRecommendedProducts(productId: String, limit: Int = 12): List<Product> =
        withContext(Dispatchers.IO) {
            if (productId.isBlank()) return@withContext emptyList()
            val response = ApiAutenticacionMovil.service.recommendedProducts(productId, limit)
            extractJsonItems(response)
        }

    suspend fun getProductById(productId: String): Product? = withContext(Dispatchers.IO) {
        if (productId.isBlank()) return@withContext null
        try {
            val response = ApiAutenticacionMovil.service.productById(productId)
            val candidate = (response["item"] as? Map<*, *>)
                ?: (response["product"] as? Map<*, *>)
                ?: (response["data"] as? Map<*, *>)
                ?: response
            val parsed = candidate.toProduct()
            parsed.takeIf { it.id.isNotBlank() && it.name.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("EleganCatalog", "No se pudo consultar el producto $productId desde el backend.", e)
            productsFlow.value.find { it.id == productId }
        }
    }
}

internal fun extractJsonItems(response: Map<String, Any?>): List<Product> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["products"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val parsed = map.toProduct()
        parsed.takeIf { it.id.isNotBlank() && it.name.isNotBlank() }
    }
}

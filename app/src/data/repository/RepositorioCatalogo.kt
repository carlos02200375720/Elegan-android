package com.example.data.repository

import android.util.Log
import com.example.data.model.ProductJson
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.data.remote.ApiAutenticacionMovil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioCatalogo {

    private val fetchMutex = Mutex()
    private val productsFlow = MutableStateFlow<List<ProductJson>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    fun observeProducts(): Flow<List<ProductJson>> = productsFlow.asStateFlow()

    fun observeLoading(): Flow<Boolean> = isLoadingFlow.asStateFlow()

    fun observeError(): Flow<String?> = errorFlow.asStateFlow()

    suspend fun refreshProducts(): List<ProductJson> = fetchMutex.withLock {
        isLoadingFlow.value = true
        errorFlow.value = null
        try {
            val remoteProducts = fetchProductsFromBackend()
            productsFlow.value = remoteProducts
            remoteProducts
        } catch (e: Exception) {
            Log.e("EleganCatalog", "Error al obtener productos desde el backend.", e)
            errorFlow.value = e.localizedMessage ?: "No se pudo cargar el catálogo desde el servidor."
            emptyList()
        } finally {
            isLoadingFlow.value = false
        }
    }

    suspend fun fetchProductsFromBackend(): List<ProductJson> = withContext(Dispatchers.IO) {
        val response = ApiAutenticacionMovil.service.products()
        val items = extractJsonItems(response)
        productsFlow.value = items
        items
    }

    suspend fun getProductById(productId: String): ProductJson? {
        if (productId.isBlank()) return null
        val remoteProducts = fetchProductsFromBackend()
        return remoteProducts.find { it.productId() == productId }
    }
}

internal fun extractJsonItems(response: Map<String, Any?>): List<ProductJson> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["products"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val typed: ProductJson = map.entries
            .mapNotNull { (k, v) -> k?.toString()?.let { it to v } }
            .toMap()
        typed.takeIf { it.productId().isNotBlank() && it.productName().isNotBlank() }
    }
}

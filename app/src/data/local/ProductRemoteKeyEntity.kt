package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estado de paginación remota por consulta (categoría + búsqueda + stock) para Paging 3 RemoteMediator.
 */
@Entity(tableName = "product_remote_keys")
internal data class ProductRemoteKeyEntity(
    @PrimaryKey
    val queryKey: String,
    val nextPage: Int?,
    val lastUpdatedMs: Long,
    val serverPaginated: Boolean
)

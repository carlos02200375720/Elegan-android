package com.example.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ProductDao {

    @Query("SELECT * FROM cached_products ORDER BY sortOrder ASC, id ASC LIMIT :limit")
    fun observeAllProducts(limit: Int = DEFAULT_OBSERVE_LIMIT): Flow<List<ProductEntity>>

    @Query(
        """
        SELECT * FROM cached_products
        WHERE (:inStockOnly = 0 OR inStock = 1)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (
              :queryPattern IS NULL
              OR LOWER(name) LIKE :queryPattern
              OR LOWER(description) LIKE :queryPattern
              OR LOWER(categoryName) LIKE :queryPattern
          )
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun pagingSource(
        categoryId: String?,
        queryPattern: String?,
        inStockOnly: Boolean
    ): PagingSource<Int, ProductEntity>

    @Query(
        """
        SELECT COUNT(*) FROM cached_products
        WHERE (:inStockOnly = 0 OR inStock = 1)
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (
              :queryPattern IS NULL
              OR LOWER(name) LIKE :queryPattern
              OR LOWER(description) LIKE :queryPattern
              OR LOWER(categoryName) LIKE :queryPattern
          )
        """
    )
    suspend fun countMatching(
        categoryId: String?,
        queryPattern: String?,
        inStockOnly: Boolean
    ): Int

    @Query("SELECT * FROM cached_products ORDER BY sortOrder ASC, id ASC LIMIT :limit")
    suspend fun getAllProducts(limit: Int = DEFAULT_OBSERVE_LIMIT): List<ProductEntity>

    @Query("SELECT * FROM cached_products WHERE id = :productId LIMIT 1")
    suspend fun getById(productId: String): ProductEntity?

    @Query("SELECT * FROM cached_products WHERE LOWER(id) = LOWER(:productId) LIMIT 1")
    suspend fun getByIdIgnoreCase(productId: String): ProductEntity?

    @Query("SELECT sortOrder FROM cached_products WHERE id = :productId LIMIT 1")
    suspend fun getSortOrder(productId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Query("DELETE FROM cached_products")
    suspend fun deleteAll()

    companion object {
        const val DEFAULT_OBSERVE_LIMIT = 250
    }
}

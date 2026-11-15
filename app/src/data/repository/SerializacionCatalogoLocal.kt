package com.example.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.Announcement
import com.example.data.model.Product
import org.json.JSONArray
import org.json.JSONObject

internal fun leerProductosPersonalizados(prefs: SharedPreferences): List<Product> {
    val raw = prefs.getString("custom_products", null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    Product(
                        id = obj.optString("id", ""),
                        name = obj.optString("name", ""),
                        description = obj.optString("description", ""),
                        category = obj.optString("category", "todo"),
                        price = obj.optDouble("price", 0.0),
                        imageUrl = obj.optString("imageUrl", ""),
                        images = obj.optJSONArray("images").toStringList(),
                        colors = obj.optJSONArray("colors").toStringList(),
                        rating = obj.optDouble("rating", 5.0),
                        reviewCount = obj.optLong("reviewCount", 0L),
                        sizes = obj.optJSONArray("sizes").toStringList(),
                        inStock = obj.optBoolean("inStock", true),
                        isFeatured = obj.optBoolean("isFeatured", false),
                        mediaFormat = obj.optString("mediaFormat", "1:1"),
                        videoUrl = obj.optString("videoUrl", ""),
                        isVideo = obj.optBoolean("isVideo", false),
                        createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudieron leer los productos publicados localmente: ${e.message}")
        emptyList()
    }
}

internal fun guardarProductosPersonalizados(prefs: SharedPreferences, items: List<Product>) {
    try {
        val array = JSONArray()
        items.forEach { p ->
            array.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("description", p.description)
                    put("category", p.category)
                    put("price", p.price)
                    put("imageUrl", p.imageUrl)
                    put("images", JSONArray(p.images))
                    put("colors", JSONArray(p.colors))
                    put("rating", p.rating)
                    put("reviewCount", p.reviewCount)
                    put("sizes", JSONArray(p.sizes))
                    put("inStock", p.inStock)
                    put("isFeatured", p.isFeatured)
                    put("mediaFormat", p.mediaFormat)
                    put("videoUrl", p.videoUrl)
                    put("isVideo", p.isVideo)
                    put("createdAt", p.createdAt.orEmpty())
                }
            )
        }
        prefs.edit().putString("custom_products", array.toString()).commit()
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudieron guardar los productos publicados localmente: ${e.message}")
    }
}

internal fun leerAnunciosPersonalizados(prefs: SharedPreferences): List<Announcement> {
    val raw = prefs.getString("custom_announcements", null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    Announcement(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        message = obj.optString("message", ""),
                        type = obj.optString("type", "banner"),
                        imageUrl = obj.optString("imageUrl", ""),
                        discountCode = obj.optString("discountCode", ""),
                        discountPercent = obj.optDouble("discountPercent", 0.0),
                        ctaText = obj.optString("ctaText", "Ver Más"),
                        targetCategory = obj.optString("targetCategory", "todo"),
                        isActive = obj.optBoolean("isActive", true),
                        videoUrl = obj.optString("videoUrl", ""),
                        isVideo = obj.optBoolean("isVideo", false),
                        createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudieron leer los anuncios publicados localmente: ${e.message}")
        emptyList()
    }
}

internal fun guardarAnunciosPersonalizados(prefs: SharedPreferences, items: List<Announcement>) {
    try {
        val array = JSONArray()
        items.forEach { ann ->
            array.put(
                JSONObject().apply {
                    put("id", ann.id)
                    put("title", ann.title)
                    put("message", ann.message)
                    put("type", ann.type)
                    put("imageUrl", ann.imageUrl)
                    put("discountCode", ann.discountCode)
                    put("discountPercent", ann.discountPercent)
                    put("ctaText", ann.ctaText)
                    put("targetCategory", ann.targetCategory)
                    put("isActive", ann.isActive)
                    put("videoUrl", ann.videoUrl)
                    put("isVideo", ann.isVideo)
                    put("createdAt", ann.createdAt.orEmpty())
                }
            )
        }
        prefs.edit().putString("custom_announcements", array.toString()).commit()
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudieron guardar los anuncios publicados localmente: ${e.message}")
    }
}

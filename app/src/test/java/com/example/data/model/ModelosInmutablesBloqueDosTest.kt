package com.example.data.model

import com.example.data.repository.RepositorioEstadisticasPublicaciones
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelosInmutablesBloqueDosTest {

    @Test
    fun `toProduct parses full backend payload into immutable Product and supports copy`() {
        val raw = mapOf(
            "id" to "prod_100",
            "name" to "Traje Sartorial Negro",
            "description" to "Corte italiano en lana virgen",
            "categoryId" to "cat_trajes",
            "categoryName" to "Trajes",
            "price" to 12500,
            "imageUrl" to "https://cdn.elegan.com/traje-1.jpg",
            "images" to listOf(
                "https://cdn.elegan.com/traje-2.jpg",
                "https://cdn.elegan.com/pasarela.mp4"
            ),
            "sizes" to listOf("48R", "50R"),
            "colors" to listOf(mapOf("name" to "Negro"), "Azul Marino"),
            "inStock" to true,
            "allowCashOnDelivery" to false,
            "mediaFormat" to "3:4",
            "shippingMethods" to listOf(
                mapOf(
                    "id" to "ship_express",
                    "name" to "Envío Express",
                    "price" to 350,
                    "deliveryTime" to "24 horas",
                    "origin" to "Santo Domingo"
                ),
                mapOf(
                    "id" to "ship_invalid_decimal",
                    "name" to "Inválido",
                    "price" to 12.5
                )
            ),
            "likesCount" to 18,
            "commentsCount" to 4,
            "savesCount" to 9,
            "sharesCount" to 2
        )

        val product = raw.toProduct()

        assertEquals("prod_100", product.id)
        assertEquals("Traje Sartorial Negro", product.name)
        assertEquals("cat_trajes", product.categoryId)
        assertEquals("Trajes", product.categoryName)
        assertEquals(12500.0, product.price, 0.0)
        assertEquals("https://cdn.elegan.com/traje-1.jpg", product.imageUrl)
        assertTrue(product.hasVideo)
        assertEquals(listOf("https://cdn.elegan.com/pasarela.mp4"), product.galleryVideoUrls)
        assertEquals(listOf("48R", "50R"), product.sizes)
        assertEquals(listOf("Negro", "Azul Marino"), product.colors)
        assertFalse(product.allowCashOnDelivery)
        assertEquals("3:4", product.mediaFormat)
        assertEquals(1, product.shippingMethods.size)
        assertEquals("ship_express", product.shippingMethods.single().id)
        assertEquals(350.0, product.shippingMethods.single().price, 0.0)
        assertEquals(18L, product.likesCount)
        assertEquals(4L, product.commentsCount)
        assertEquals(9L, product.savesCount)
        assertEquals(2L, product.sharesCount)

        val withShipping = product.copy(selectedShippingId = "ship_express")
        assertEquals("", product.selectedShippingId)
        assertEquals("ship_express", withShipping.selectedShippingId)
    }

    @Test
    fun `partial product payload leaves omitted counters null and preserves existing stats`() = runBlocking {
        val statsRepo = RepositorioEstadisticasPublicaciones()
        statsRepo.updateLikesCount("prod_partial", 25L)
        statsRepo.updateSavesCount("prod_partial", 11L)
        statsRepo.updateSharesCount("prod_partial", 6L)

        val partialProduct = mapOf(
            "id" to "prod_partial",
            "name" to "Camisa Blanca",
            "commentsCount" to 3
        ).toProduct()

        assertNull(partialProduct.likesCount)
        assertNull(partialProduct.savesCount)
        assertNull(partialProduct.sharesCount)
        assertEquals(3L, partialProduct.commentsCount)

        statsRepo.syncCountsFromProducts(listOf(partialProduct))

        val stats = statsRepo.statsFlow.first()["prod_partial"]
        assertEquals(25L, stats?.likesCount)
        assertEquals(11L, stats?.savesCount)
        assertEquals(6L, stats?.sharesCount)
        assertEquals(3L, stats?.commentsCount)
    }

    @Test
    fun `toAnnouncement and toCashOnDeliveryConfig parse typed destinations and defaults`() {
        val productAnnouncement = mapOf(
            "id" to "ann_p",
            "title" to "Destacado",
            "message" to "Edición limitada",
            "destinationType" to "product",
            "destinationId" to "prod_77",
            "badge" to "VIP",
            "active" to true
        ).toAnnouncement()

        assertEquals("ann_p", productAnnouncement.id)
        assertEquals(AnnouncementDestination.Product("prod_77"), productAnnouncement.destination)
        assertEquals("", productAnnouncement.announcementCategory())
        assertTrue(productAnnouncement.active)

        val fallbackAnnouncement = mapOf(
            "id" to "ann_none",
            "title" to "Aviso general"
        ).toAnnouncement()
        assertEquals(AnnouncementDestination.None, fallbackAnnouncement.destination)

        val codEnabled = mapOf(
            "enabled" to true,
            "message" to "Disponible en Santo Domingo"
        ).toCashOnDeliveryConfig()
        assertTrue(codEnabled.enabled)
        assertEquals("Disponible en Santo Domingo", codEnabled.message)

        val codEmpty = emptyMap<String, Any?>().toCashOnDeliveryConfig()
        assertFalse(codEmpty.enabled)
        assertEquals("", codEmpty.message)
    }
}

package com.example.data.repository

import com.example.data.model.Announcement
import com.example.data.model.LocalBoutiqueMedia
import com.example.data.model.Product

internal fun todosProductosIniciales(): List<Product> =
    productosInicialesPrendas() + productosInicialesCalzadoYPerfumes()

private fun productosInicialesCalzadoYPerfumes(): List<Product> = listOf(
    Product(
        id = "prod_zapato_1",
        name = "Zapatos Oxford Cuero Glaseado Cap-Toe",
        description = "Construcción Goodyear welted con suela de cuero apomazado. Piel de becerro de grano fino con acabado espejo en la puntera.",
        category = "zapato",
        price = 240.00,
        imageUrl = LocalBoutiqueMedia.zapatoCover,
        rating = 5.0,
        reviewCount = 280,
        sizes = listOf("40", "41", "42", "43", "44", "45"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_zapato_2",
        name = "Zapatos Derby Brogue Borgoña Antik",
        description = "Perforado brogue wingtip artesanal en tono burdeos envejecido. Plantilla con amortiguación de corcho natural transpirable.",
        category = "zapato",
        price = 220.00,
        imageUrl = LocalBoutiqueMedia.zapatoCover,
        rating = 4.9,
        reviewCount = 175,
        sizes = listOf("41", "42", "43", "44"),
        inStock = true,
        isFeatured = false
    ),
    Product(
        id = "prod_interiore_1",
        name = "Pack 3 Bóxers Modal Seda Elegan",
        description = "Micro-modal ultra suave con pretina elástica forrada en algodón que no marca ni oprime. Máxima frescura y elegancia interior.",
        category = "interiore",
        price = 45.00,
        imageUrl = LocalBoutiqueMedia.camisaCover,
        rating = 4.8,
        reviewCount = 150,
        sizes = listOf("S", "M", "L", "XL"),
        inStock = true,
        isFeatured = false
    ),
    Product(
        id = "prod_perfume_1",
        name = "Elegan Noir Eau de Parfum 100ml",
        description = "Notas de salida de bergamota y cardamomo, corazón de cedro del Atlas y fondo profundo de ámbar gris y cuero ahumado.",
        category = "perfume",
        price = 135.00,
        imageUrl = LocalBoutiqueMedia.perfumeCover,
        rating = 5.0,
        reviewCount = 410,
        sizes = listOf("100ml Spray"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_perfume_2",
        name = "Gentleman Reserve Extrait de Parfum",
        description = "Alta concentración con notas de tabaco rubio, vainilla bourbon y maderas nobles. Duración superior a 14 horas en piel.",
        category = "perfume",
        price = 165.00,
        imageUrl = LocalBoutiqueMedia.perfumeCover,
        rating = 4.9,
        reviewCount = 225,
        sizes = listOf("50ml Extrait"),
        inStock = true,
        isFeatured = true
    )
)

internal fun anunciosInicialesTienda(): List<Announcement> = listOf(
    Announcement(
        id = "ann_1",
        title = "Gran Venta Sartorial 2026",
        message = "Disfruta de hasta un 25% de descuento en trajes y camisas Oxford seleccionadas por tiempo limitado.",
        type = "descuento",
        imageUrl = LocalBoutiqueMedia.camisaCover,
        discountCode = "SARTORIAL25",
        discountPercent = 25.0,
        ctaText = "Ver Colección",
        targetCategory = "camisa",
        isActive = true
    ),
    Announcement(
        id = "ann_2",
        title = "Nueva Colección: Fragancias Noir",
        message = "Descubre nuestras esencias exclusivas destiladas para el caballero contemporáneo.",
        type = "banner",
        imageUrl = LocalBoutiqueMedia.perfumeCover,
        discountCode = "",
        discountPercent = 0.0,
        ctaText = "Explorar Perfumes",
        targetCategory = "perfume",
        isActive = true
    ),
    Announcement(
        id = "ann_3",
        title = "Artesanía & Calzado Goodyear",
        message = "Zapatos hechos a mano con pieles italianas de la más alta distinción.",
        type = "coleccion",
        imageUrl = LocalBoutiqueMedia.zapatoCover,
        discountCode = "",
        discountPercent = 0.0,
        ctaText = "Ver Zapatos",
        targetCategory = "zapato",
        isActive = true
    )
)

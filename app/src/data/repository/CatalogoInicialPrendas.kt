package com.example.data.repository

import com.example.data.model.LocalBoutiqueMedia
import com.example.data.model.Product

internal fun productosInicialesPrendas(): List<Product> = listOf(
    Product(
        id = "prod_gorra_1",
        name = "Boina Clásica Gatsby Peaky Tweed",
        description = "Confeccionada con lana británica de espiga en tono marrón carbón. Interior forrado en satén sedoso para una sensación inigualable.",
        category = "gorra",
        price = 55.00,
        imageUrl = LocalBoutiqueMedia.gorraCover,
        rating = 4.9,
        reviewCount = 142,
        sizes = listOf("S (56cm)", "M (58cm)", "L (60cm)", "XL (62cm)"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_gorra_2",
        name = "Gorra Newsboy Borgoña Signature",
        description = "Elegante gorra de 8 paneles en pana fina color vino intenso. Un homenaje al corte tradicional inglés con acabados a mano.",
        category = "gorra",
        price = 48.00,
        imageUrl = LocalBoutiqueMedia.gorraCover,
        rating = 4.8,
        reviewCount = 98,
        sizes = listOf("M (58cm)", "L (60cm)"),
        inStock = true,
        isFeatured = false
    ),
    Product(
        id = "prod_gafa_1",
        name = "Gafas de Sol Aviador Gold Master",
        description = "Montura de titanio bañada en oro de 18k con lentes polarizadas verde bosque degradadas. Protección UV400 completa con estuche de cuero.",
        category = "gafa",
        price = 145.00,
        imageUrl = LocalBoutiqueMedia.gafaCover,
        rating = 5.0,
        reviewCount = 210,
        sizes = listOf("Única (54-18-145)"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_gafa_2",
        name = "Gafas Clubmaster Carey Vintage",
        description = "Acetato italiano pulido a mano con detalles metálicos plateados. Diseño atemporal para caballeros distinguidos.",
        category = "gafa",
        price = 120.00,
        imageUrl = LocalBoutiqueMedia.gafaCover,
        rating = 4.7,
        reviewCount = 85,
        sizes = listOf("Única (51-20-145)"),
        inStock = true,
        isFeatured = false
    ),
    Product(
        id = "prod_camisa_1",
        name = "Camisa Oxford Blanca Algodón Egipcio",
        description = "Tejido dos cabos de algodón Giza 100%. Cuello cutaway reforzado y botones de nácar natural. Planchado antiarrugas permanente.",
        category = "camisa",
        price = 89.00,
        imageUrl = LocalBoutiqueMedia.camisaCover,
        rating = 4.9,
        reviewCount = 340,
        sizes = listOf("38 (S)", "40 (M)", "42 (L)", "44 (XL)", "46 (XXL)"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_camisa_2",
        name = "Blazer Sartorial Borgoña Imperial",
        description = "Chaqueta estructurada de sarga de lana peinada con solapa en punta de lanza y botones esmaltados. La insignia indiscutible de Elegan.",
        category = "camisa",
        price = 280.00,
        imageUrl = LocalBoutiqueMedia.camisaCover,
        rating = 5.0,
        reviewCount = 115,
        sizes = listOf("48R", "50R", "52R", "54R"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_correa_1",
        name = "Correa de Cuero Vacuno Hebilla Dorada",
        description = "Piel de curtición vegetal teñida en anilina color coñac intenso. Hebilla de latón macizo cepillado de alta durabilidad.",
        category = "correa",
        price = 65.00,
        imageUrl = LocalBoutiqueMedia.zapatoCover,
        rating = 4.8,
        reviewCount = 160,
        sizes = listOf("85cm", "90cm", "95cm", "100cm", "105cm"),
        inStock = true,
        isFeatured = false
    ),
    Product(
        id = "prod_correa_2",
        name = "Cinturón Reversible Negro & Borgoña",
        description = "Mecanismo giratorio suizo que permite alternar entre cuero negro caviar texturizado y borgoña liso satinado.",
        category = "correa",
        price = 75.00,
        imageUrl = LocalBoutiqueMedia.zapatoCover,
        rating = 4.9,
        reviewCount = 130,
        sizes = listOf("90cm", "95cm", "100cm", "105cm"),
        inStock = true,
        isFeatured = false
    ),
    Product(
        id = "prod_pantalone_1",
        name = "Pantalón de Vestir Plisado Gris Carbón",
        description = "Corte sartorial con doble pliegue frontal, ajustadores laterales sin necesidad de cinturón y caída recta impecable.",
        category = "pantalone",
        price = 110.00,
        imageUrl = LocalBoutiqueMedia.camisaCover,
        rating = 4.9,
        reviewCount = 192,
        sizes = listOf("30", "32", "34", "36", "38"),
        inStock = true,
        isFeatured = true
    ),
    Product(
        id = "prod_pantalone_2",
        name = "Pantalón Chino Premium Arena",
        description = "Algodón stretch de 280g con tacto melocotón. Ideal tanto para ocasiones de negocios informales como para fines de semana elegantes.",
        category = "pantalone",
        price = 85.00,
        imageUrl = LocalBoutiqueMedia.camisaCover,
        rating = 4.7,
        reviewCount = 94,
        sizes = listOf("30", "32", "34", "36"),
        inStock = true,
        isFeatured = false
    )
)

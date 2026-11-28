package com.example

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

/**
 * Rutas tipadas con Kotlinx Serialization para el grafo de Navigation Compose de Elegan.
 */
@Serializable
sealed interface RutaApp {
    @Serializable
    data object Inicio : RutaApp

    @Serializable
    data object Catalogo : RutaApp

    @Serializable
    data object Perfil : RutaApp

    @Serializable
    data object Categoria : RutaApp

    @Serializable
    data class Producto(val productId: String = "") : RutaApp

    @Serializable
    data object Carrito : RutaApp

    @Serializable
    data object Verificacion : RutaApp

    @Serializable
    data object Gracia : RutaApp

    @Serializable
    data object Chats : RutaApp
}

internal fun RutaApp.tieneBarraInferior(): Boolean =
    this == RutaApp.Inicio || this == RutaApp.Catalogo || this == RutaApp.Perfil

internal fun RutaApp.nombreAnalytics(): String = when (this) {
    RutaApp.Inicio -> "INICIO"
    RutaApp.Catalogo -> "CATALOGO"
    RutaApp.Perfil -> "PERFIL"
    RutaApp.Categoria -> "CATEGORIA"
    is RutaApp.Producto -> "PRODUCTO"
    RutaApp.Carrito -> "CARRITO"
    RutaApp.Verificacion -> "VERIFICASION"
    RutaApp.Gracia -> "GRACIA"
    RutaApp.Chats -> "CHATS"
}

internal fun NavDestination?.aRutaApp(fallback: RutaApp = RutaApp.Inicio): RutaApp = when {
    this == null -> fallback
    hasRoute<RutaApp.Inicio>() -> RutaApp.Inicio
    hasRoute<RutaApp.Catalogo>() -> RutaApp.Catalogo
    hasRoute<RutaApp.Perfil>() -> RutaApp.Perfil
    hasRoute<RutaApp.Categoria>() -> RutaApp.Categoria
    hasRoute<RutaApp.Producto>() -> RutaApp.Producto()
    hasRoute<RutaApp.Carrito>() -> RutaApp.Carrito
    hasRoute<RutaApp.Verificacion>() -> RutaApp.Verificacion
    hasRoute<RutaApp.Gracia>() -> RutaApp.Gracia
    hasRoute<RutaApp.Chats>() -> RutaApp.Chats
    else -> fallback
}

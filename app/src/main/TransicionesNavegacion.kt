package com.example

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private fun entradaDeslizada(desdeIzquierda: Boolean = false): EnterTransition =
    slideInHorizontally(
        initialOffsetX = { fullWidth -> if (desdeIzquierda) -fullWidth else fullWidth },
        animationSpec = tween(durationMillis = 320)
    )

private fun salidaDeslizada(haciaIzquierda: Boolean = false): ExitTransition =
    slideOutHorizontally(
        targetOffsetX = { fullWidth -> if (haciaIzquierda) -fullWidth else fullWidth },
        animationSpec = tween(durationMillis = 280)
    )

private fun entradaFondoSutil(): EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = 280), initialAlpha = 0.99f)

private fun salidaFondoSutil(): ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = 320), targetAlpha = 0.99f)

internal fun obtenerEnterTransition(
    origen: RutaApp,
    destino: RutaApp
): EnterTransition = when {
    destino == RutaApp.Gracia -> entradaDeslizada()
    origen == RutaApp.Gracia -> entradaFondoSutil()
    destino == RutaApp.Chats && origen != RutaApp.Carrito -> entradaDeslizada()
    origen == RutaApp.Chats && destino != RutaApp.Carrito -> entradaFondoSutil()
    destino == RutaApp.Categoria -> entradaDeslizada(desdeIzquierda = true)
    origen == RutaApp.Categoria -> entradaFondoSutil()
    destino == RutaApp.Verificacion -> entradaDeslizada()
    origen == RutaApp.Verificacion && destino == RutaApp.Carrito -> entradaFondoSutil()
    destino == RutaApp.Carrito -> entradaDeslizada()
    origen == RutaApp.Carrito && destino != RutaApp.Verificacion -> entradaFondoSutil()
    destino is RutaApp.Producto -> entradaDeslizada()
    origen is RutaApp.Producto && destino != RutaApp.Carrito -> entradaFondoSutil()
    destino == RutaApp.Perfil -> entradaDeslizada()
    origen == RutaApp.Perfil -> entradaFondoSutil()
    destino == RutaApp.Catalogo -> entradaDeslizada()
    origen == RutaApp.Catalogo -> entradaFondoSutil()
    else -> EnterTransition.None
}

internal fun obtenerExitTransition(
    origen: RutaApp,
    destino: RutaApp
): ExitTransition = when {
    destino == RutaApp.Gracia -> salidaFondoSutil()
    origen == RutaApp.Gracia -> salidaDeslizada()
    destino == RutaApp.Chats && origen != RutaApp.Carrito -> salidaFondoSutil()
    origen == RutaApp.Chats && destino != RutaApp.Carrito -> salidaDeslizada()
    destino == RutaApp.Categoria -> salidaFondoSutil()
    origen == RutaApp.Categoria -> salidaDeslizada(haciaIzquierda = true)
    destino == RutaApp.Verificacion -> salidaFondoSutil()
    origen == RutaApp.Verificacion && destino == RutaApp.Carrito -> salidaDeslizada()
    destino == RutaApp.Carrito -> salidaFondoSutil()
    origen == RutaApp.Carrito && destino != RutaApp.Verificacion -> salidaDeslizada()
    destino is RutaApp.Producto -> salidaFondoSutil()
    origen is RutaApp.Producto && destino != RutaApp.Carrito -> salidaDeslizada()
    destino == RutaApp.Perfil -> salidaFondoSutil()
    origen == RutaApp.Perfil -> salidaDeslizada()
    destino == RutaApp.Catalogo -> salidaFondoSutil()
    origen == RutaApp.Catalogo -> salidaDeslizada()
    else -> ExitTransition.None
}

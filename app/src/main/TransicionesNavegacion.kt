package com.example

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private fun transicionEntradaDeslizada(desdeIzquierda: Boolean = false): ContentTransform =
    ContentTransform(
        targetContentEnter = slideInHorizontally(
            initialOffsetX = { fullWidth -> if (desdeIzquierda) -fullWidth else fullWidth },
            animationSpec = tween(durationMillis = 320)
        ),
        initialContentExit = fadeOut(
            animationSpec = tween(durationMillis = 320),
            targetAlpha = 0.99f
        ),
        targetContentZIndex = 1f,
        sizeTransform = SizeTransform(clip = false)
    )

private fun transicionSalidaDeslizada(haciaIzquierda: Boolean = false): ContentTransform =
    ContentTransform(
        targetContentEnter = fadeIn(
            animationSpec = tween(durationMillis = 280),
            initialAlpha = 0.99f
        ),
        initialContentExit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> if (haciaIzquierda) -fullWidth else fullWidth },
            animationSpec = tween(durationMillis = 280)
        ),
        targetContentZIndex = -1f,
        sizeTransform = SizeTransform(clip = false)
    )

internal fun obtenerTransicionPantalla(
    initialState: Screen,
    targetState: Screen
): ContentTransform = when {
    targetState == Screen.GRACIA -> transicionEntradaDeslizada()
    initialState == Screen.GRACIA -> transicionSalidaDeslizada()
    targetState == Screen.CHATS && initialState != Screen.CARRITO -> transicionEntradaDeslizada()
    initialState == Screen.CHATS && targetState != Screen.CARRITO -> transicionSalidaDeslizada()
    targetState == Screen.CATEGORIA -> transicionEntradaDeslizada(desdeIzquierda = true)
    initialState == Screen.CATEGORIA -> transicionSalidaDeslizada(haciaIzquierda = true)
    targetState == Screen.VERIFICASION -> transicionEntradaDeslizada()
    initialState == Screen.VERIFICASION && targetState == Screen.CARRITO -> transicionSalidaDeslizada()
    targetState == Screen.CARRITO -> transicionEntradaDeslizada()
    initialState == Screen.CARRITO && targetState != Screen.VERIFICASION -> transicionSalidaDeslizada()
    targetState == Screen.PRODUCTO -> transicionEntradaDeslizada()
    initialState == Screen.PRODUCTO && targetState != Screen.CARRITO -> transicionSalidaDeslizada()
    targetState == Screen.PERFIL -> transicionEntradaDeslizada()
    initialState == Screen.PERFIL -> transicionSalidaDeslizada()
    targetState == Screen.CATALOGO -> transicionEntradaDeslizada()
    initialState == Screen.CATALOGO -> transicionSalidaDeslizada()
    else -> ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.None,
        sizeTransform = SizeTransform(clip = false)
    )
}

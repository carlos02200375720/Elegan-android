package com.example.ui

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun formatearMontoDecimal(monto: Double): String {
    val normalizado = if (!monto.isFinite() || monto == 0.0) 0.0 else monto
    val entero = BigDecimal.valueOf(normalizado)
        .setScale(0, RoundingMode.HALF_UP)
        .toBigInteger()
    val negativo = entero.signum() < 0
    val agrupado = agruparMilesConPunto(entero.abs().toString())
    return if (negativo) "-$agrupado" else agrupado
}

private fun agruparMilesConPunto(digitos: String): String {
    if (digitos.length <= 3) return digitos
    val sb = StringBuilder(digitos.length + (digitos.length / 3))
    val longitudPrimerGrupo = digitos.length % 3
    var indice = 0
    if (longitudPrimerGrupo > 0) {
        sb.append(digitos, 0, longitudPrimerGrupo)
        indice = longitudPrimerGrupo
    }
    while (indice < digitos.length) {
        if (sb.isNotEmpty()) sb.append('.')
        sb.append(digitos, indice, indice + 3)
        indice += 3
    }
    return sb.toString()
}

fun formatearMonedaRD(monto: Double): String = "RD$ ${formatearMontoDecimal(monto)}"

fun formatearPrecioRd(monto: Double): String = formatearMonedaRD(monto)

fun formatearDescuentoRd(monto: Double): String = "-RD$ ${formatearMontoDecimal(abs(monto))}"

fun formatearEnvioRd(monto: Double): String =
    if (!monto.isFinite() || monto <= 0.0) "GRATIS" else formatearMonedaRD(monto)

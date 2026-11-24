package com.example.ui

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import kotlin.math.abs

private val CIEN = BigInteger.valueOf(100L)

fun formatearMontoDecimal(monto: Double): String {
    val normalizado = if (!monto.isFinite() || monto == 0.0) 0.0 else monto
    val redondeado = BigDecimal.valueOf(normalizado).setScale(2, RoundingMode.HALF_UP)
    val esNegativo = redondeado.signum() < 0
    val centavosTotales = redondeado.abs().movePointRight(2).toBigInteger()
    val parteEntera = centavosTotales.divide(CIEN)
    val centavos = centavosTotales.remainder(CIEN).toInt()
    val enteroFormateado = agruparMilesConPunto(parteEntera.toString())
    val prefijoSigno = if (esNegativo) "-" else ""

    return if (centavos == 0) {
        "$prefijoSigno$enteroFormateado"
    } else {
        "$prefijoSigno$enteroFormateado,${centavos.toString().padStart(2, '0')}"
    }
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

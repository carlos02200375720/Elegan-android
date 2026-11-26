package com.example.ui

import kotlin.math.abs

fun formatearMontoDecimal(monto: Double): String {
    if (!monto.isFinite() || monto % 1.0 != 0.0 || abs(monto) >= Long.MAX_VALUE.toDouble()) return "—"
    val entero = monto.toLong()
    return agruparMilesConPunto(entero.toString())
}

private fun agruparMilesConPunto(digitos: String): String {
    val negativo = digitos.startsWith("-")
    val numero = if (negativo) digitos.drop(1) else digitos
    if (numero.length <= 3) return digitos
    val grupos = numero.reversed().chunked(3).joinToString(".").reversed()
    return if (negativo) "-$grupos" else grupos
}

fun formatearMonedaRD(monto: Double): String = "\$" + formatearMontoDecimal(monto)

fun formatearPrecioRd(monto: Double): String = formatearMonedaRD(monto)

fun formatearDescuentoRd(monto: Double): String = "-\$" + formatearMontoDecimal(abs(monto))

fun formatearEnvioRd(monto: Double): String =
    if (monto == 0.0) "GRATIS" else formatearMonedaRD(monto)

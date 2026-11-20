package com.example.ui

import java.util.Locale
import kotlin.math.abs

private val LOCALE_PRECIO_RD: Locale = Locale.US

fun formatearMontoDecimal(monto: Double): String {
    val normalizado = if (!monto.isFinite() || monto == 0.0) 0.0 else monto
    return String.format(LOCALE_PRECIO_RD, "%.2f", normalizado)
}

fun formatearMonedaRD(monto: Double): String = "RD$ ${formatearMontoDecimal(monto)}"

fun formatearPrecioRd(monto: Double): String = formatearMonedaRD(monto)

fun formatearDescuentoRd(monto: Double): String = "-RD$ ${formatearMontoDecimal(abs(monto))}"

fun formatearEnvioRd(monto: Double): String =
    if (!monto.isFinite() || monto <= 0.0) "GRATIS" else formatearMonedaRD(monto)

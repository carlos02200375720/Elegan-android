package com.example.ui

internal fun detectarMarcaTarjeta(cardNumberDigits: String): String {
    return when {
        cardNumberDigits.startsWith("4") -> "Visa"
        cardNumberDigits.startsWith("5") || cardNumberDigits.startsWith("2") -> "Mastercard"
        cardNumberDigits.startsWith("3") -> "American Express"
        else -> "Tarjeta"
    }
}

internal fun formatearNumeroTarjeta(cardNumberDigits: String): String {
    return cardNumberDigits.chunked(4).joinToString(" ")
}

internal fun formatearVencimientoTarjeta(cardExpiryDigits: String): String {
    return if (cardExpiryDigits.length <= 2) {
        cardExpiryDigits
    } else {
        "${cardExpiryDigits.take(2)}/${cardExpiryDigits.drop(2)}"
    }
}

internal fun validarDatosTarjeta(
    cardHolder: String,
    cardNumberDigits: String,
    cardExpiryDigits: String,
    cardCvv: String
): String? {
    val holder = cardHolder.trim()
    if (holder.length < 3) {
        return "Ingresa el nombre del titular de la tarjeta."
    }
    if (cardNumberDigits.length < 13) {
        return "Ingresa un número de tarjeta válido (mínimo 13 dígitos)."
    }
    if (cardExpiryDigits.length < 4) {
        return "Ingresa la fecha de vencimiento en formato MM/AA."
    }
    val month = cardExpiryDigits.take(2).toIntOrNull() ?: 0
    if (month !in 1..12) {
        return "El mes de vencimiento debe estar entre 01 y 12."
    }
    if (cardCvv.length < 3) {
        return "Ingresa un código CVV válido de 3 o 4 dígitos."
    }
    return null
}

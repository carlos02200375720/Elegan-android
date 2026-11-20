package com.example

import com.example.ui.formatearDescuentoRd
import com.example.ui.formatearEnvioRd
import com.example.ui.formatearMonedaRD
import com.example.ui.formatearMontoDecimal
import com.example.ui.formatearPrecioRd
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun priceFormatting_isConsistentRegardlessOfDeviceLocale() {
    val previousLocale = Locale.getDefault()
    try {
      for (locale in listOf(Locale.US, Locale.forLanguageTag("es-ES"), Locale.GERMANY, Locale.FRANCE)) {
        Locale.setDefault(locale)
        assertEquals("1250.50", formatearMontoDecimal(1250.5))
        assertEquals("0.00", formatearMontoDecimal(-0.0))
        assertEquals("0.00", formatearMontoDecimal(Double.NaN))
        assertEquals("RD$ 55.00", formatearMonedaRD(55.0))
        assertEquals("RD$ 55.00", formatearPrecioRd(55.0))
        assertEquals("-RD$ 12.75", formatearDescuentoRd(12.75))
        assertEquals("GRATIS", formatearEnvioRd(0.0))
        assertEquals("RD$ 250.00", formatearEnvioRd(250.0))
      }
    } finally {
      Locale.setDefault(previousLocale)
    }
  }
}

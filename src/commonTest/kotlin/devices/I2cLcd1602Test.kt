package ch.softappeal.kopi.devices

import ch.softappeal.kopi.I2C_ADDRESS_LCD1602
import ch.softappeal.kopi.I2C_BUS
import ch.softappeal.kopi.I2cBus
import ch.softappeal.kopi.use
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

abstract class I2cLcd1602Test {
    @Test
    fun test() = runBlocking {
        I2cBus(I2C_BUS).use { bus ->
            I2cLcd1602(bus.device(I2C_ADDRESS_LCD1602)).use { lcd ->
                delay(1.seconds)
                lcd.setBacklight(false)
                delay(1.seconds)
                lcd.setBacklight(true)
                lcd.showCursor(true)
                lcd.setBlink(true)
                lcd.setCursorPosition(1, 3)
                delay(2.seconds)
                lcd.setBlink(false)
                delay(2.seconds)
                lcd.showCursor(false)
                delay(2.seconds)
                lcd.clear()
                lcd.setCursorPosition(0, 0)
                lcd.displayString("123456789012345")
                lcd.setCursorPosition(1, lcd.config.columns - 15)
                lcd.displayString("234567890123456")
                delay(2.seconds)
                lcd.showDisplay(false)
                delay(2.seconds)
                lcd.showDisplay(true)
                delay(2.seconds)
            }
        }
    }
}

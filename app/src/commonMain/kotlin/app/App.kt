package ch.softappeal.kopi.app

import ch.softappeal.kopi.Gpio
import ch.softappeal.kopi.Gpio.Bias
import ch.softappeal.kopi.Gpio.Edge
import ch.softappeal.kopi.I2cBus
import ch.softappeal.kopi.devices.Paj7620U2.Gesture
import ch.softappeal.kopi.devices.bme280
import ch.softappeal.kopi.devices.i2cLcd1602
import ch.softappeal.kopi.devices.paj7620U2
import ch.softappeal.kopi.use
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

private fun runServer() = embeddedServer(CIO, port = 8080) {
    routing {
        get("/") {
            call.respondText("Hello from kopi")
        }
    }
}.start()

fun main() {
    runBlocking {
        runServer()
        I2cBus(I2C_BUS).use { bus ->
            i2cLcd1602(bus.device(I2C_ADDRESS_LCD1602)).use { lcd ->
                lcd.clear()
                lcd.setCursorPosition(1, 0)
                lcd.displayString("REBOOTED")
                Gpio().use { gpio ->
                    val paj7620U2 = paj7620U2(bus.device(I2C_ADDRESS_PAJ7620U2))
                    val bme280 = bme280(bus.device(I2C_ADDRESS_BME280))
                    gpio.listen(GPIO_IN_CONNECTED_TO_PAJ7620U2_INT, Bias.PullUp, 100.days) { edge, _ ->
                        if (edge == Edge.Falling) {
                            val gesture = paj7620U2.gesture()
                            val measurements = bme280.measurements()
                            val string = when (gesture) {
                                null -> "<none>"
                                Gesture.Up -> "Temp:" + (measurements.temperaturInCelsius * 10).roundToInt() + " 0.1C"
                                Gesture.Down -> "Press:" + (measurements.pressureInPascal / 100).roundToInt() + " hPa"
                                Gesture.Left -> "Humid:" + (measurements.humidityInPercent * 10).roundToInt() + " 0.1%"
                                else -> gesture.name
                            }
                            lcd.setCursorPosition(1, 0)
                            lcd.displayString(string + " ".repeat(lcd.config.columns - string.length))
                        }
                        true
                    }
                }
            }
        }
    }
}

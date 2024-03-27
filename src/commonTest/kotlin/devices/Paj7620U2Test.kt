package ch.softappeal.kopi.devices

import ch.softappeal.kopi.GPIO_IN_CONNECTED_TO_PAJ7620U2_INT
import ch.softappeal.kopi.Gpio
import ch.softappeal.kopi.Gpio.Bias
import ch.softappeal.kopi.Gpio.Edge
import ch.softappeal.kopi.I2C_ADDRESS_PAJ7620U2
import ch.softappeal.kopi.I2C_BUS
import ch.softappeal.kopi.I2cBus
import ch.softappeal.kopi.use
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

abstract class Paj7620U2Test {
    @Test
    fun test() {
        runBlocking {
            I2cBus(I2C_BUS).use { bus ->
                val paj7620U2 = paj7620U2(bus.device(I2C_ADDRESS_PAJ7620U2))
                println("gesture: ${paj7620U2.gesture()}")
                println("make all 9 gestures ...")
                Gpio().use { gpio ->
                    gpio.listen(GPIO_IN_CONNECTED_TO_PAJ7620U2_INT, Bias.PullUp, 5.seconds) { edge, _ ->
                        if (edge == Edge.Falling) println(paj7620U2.gesture())
                        true
                    }
                }
            }
        }
    }
}

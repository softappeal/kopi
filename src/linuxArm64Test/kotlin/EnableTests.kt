package ch.softappeal.kopi

import ch.softappeal.kopi.devices.Bme280Test
import ch.softappeal.kopi.devices.I2cLcd1602Test
import ch.softappeal.kopi.devices.Paj7620U2Test

class ConcreteGpioTest : GpioTest()

class ConcreteI2cLcd1602Test : I2cLcd1602Test()
class ConcreteBme280Test : Bme280Test()
class ConcretePaj7620U2Test : Paj7620U2Test()

@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("SpellCheckingInspection")

package ch.softappeal.kopi.devices

import ch.softappeal.kopi.I2cCommand
import ch.softappeal.kopi.I2cDevice
import ch.softappeal.kopi.devices.Bme280.Measurements
import ch.softappeal.kopi.write
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/*
    Combined digital humidity, pressure and temperature sensor

    Datasheet: https://www.bosch-sensortec.com/media/boschsensortec/downloads/datasheets/bst-bme280-ds002.pdf
    API      : https://github.com/boschsensortec/BME280_SensorAPI/blob/master/bme280.c
 */

private val SetupMode = listOf(
    /*
        Datasheet:
            Table 7: Settings and performance for weather monitoring
            5.4.3 Register 0xF2 "ctrl_hum"
                Changes to this register only become effective after a write operation to "ctrl_meas".
            5.4.5 Register 0xF4 "ctrl_meas"
     */
    I2cCommand(
        0xF2U, // ctrl_hum
        0x01U // Humidity oversampling x1
    ),
    I2cCommand(
        0xF4U, // ctrl_meas
        0x01U.toUByte() or // Forced mode
            0x04U or // Pressure oversampling x1
            0x20U // Temperature oversampling x1
    ),
)

public interface Bme280 {
    public data class Measurements(
        val temperaturInCelsius: Double,
        val pressureInPascal: Double,
        val humidityInPercent: Double,
    )

    public suspend fun measurements(): Measurements
}

public suspend fun Bme280(device: I2cDevice): Bme280 {
    class TemperatureCalib(
        val t1: Double,
        val t2: Double,
        val t3: Double,
    )

    class PressureCalib(
        val p1: Double,
        val p2: Double,
        val p3: Double,
        val p4: Double,
        val p5: Double,
        val p6: Double,
        val p7: Double,
        val p8: Double,
        val p9: Double,
    )

    class HumidityCalib(
        val h1: Double,
        val h2: Double,
        val h3: Double,
        val h4: Double,
        val h5: Double,
        val h6: Double,
    )

    data class TemperatureCompensateResult(val temperaturInCelsius: Double, val tFine: Double)

    fun TemperatureCalib.compensate(t: Double): TemperatureCompensateResult {
        // API: compensate_temperature
        val var1 = (t / 16_384.0 - t1 / 1024.0) * t2
        var var2 = t / 131_072.0 - t1 / 8192.0
        var2 *= var2 * t3
        val tFine = var1 + var2
        return TemperatureCompensateResult(temperaturInCelsius = (tFine / 5120.0).coerceIn(-40.0, 85.0), tFine)
    }

    fun PressureCalib.compensate(p: Double, tFine: Double): Double {
        // API: compensate_pressure
        var var1 = tFine / 2.0 - 64_000.0
        var var2 = var1 * var1 * p6 / 32_768.0
        var2 += var1 * p5 * 2.0
        var2 = var2 / 4.0 + p4 * 65_536.0
        val var3 = p3 * var1 * var1 / 524_288.0
        var1 = (var3 + p2 * var1) / 524_288.0
        var1 = (1.0 + var1 / 32_768.0) * p1
        return if (var1 <= 0.0) 30_000.0 else {
            var pressure = 1048_576.0 - p
            pressure = (pressure - var2 / 4096.0) * 6250.0 / var1
            var1 = p9 * pressure * pressure / 2147_483_648.0
            var2 = pressure * p8 / 32_768.0
            pressure += (var1 + var2 + p7) / 16.0
            pressure.coerceIn(30_000.0, 110_000.0)
        }
    }

    fun HumidityCalib.compensate(h: Double, tFine: Double): Double {
        // API: compensate_humidity
        val var1 = tFine - 76_800.0
        val var2 = h4 * 64.0 + h5 / 16_384.0 * var1
        val var3 = h - var2
        val var4 = h2 / 65_536.0
        val var5 = 1.0 + h3 / 67_108_864.0 * var1
        var var6 = 1.0 + h6 / 67_108_864.0 * var1 * var5
        var6 *= var3 * var4 * var5
        return (var6 * (1.0 - h1 * var6 / 524_288.0)).coerceIn(0.0, 100.0)
    }

    device.write(0xE0U, 0xB6U) // reset
    delay(300.milliseconds) // power-on delay
    check(device.read(0xD0U) == 0x60U.toUByte()) { "chipId isn't BME280" }
    suspend fun setupMode(device: I2cDevice) = device.write(SetupMode)
    setupMode(device)
    /*
        Datasheet:
            4.2.2 Trimming parameter readout:
                The trimming parameters are programmed into the devices non-volatile memory (NVM) during
                production and cannot be altered by the customer.
            Table 16: Compensation parameter storage, naming and data type
            Table 18: Memory map
        API:
            parse_temp_press_calib_data
            parse_humidity_calib_data
     */
    fun UByteArray.toShort(offset: Int) = ((this[offset + 1].toByte().toInt() shl 8) or this[offset].toInt()).toDouble()
    fun UByteArray.toUShort(offset: Int) = ((this[offset + 1].toInt() shl 8) or this[offset].toInt()).toDouble()
    val temperatureCalib: TemperatureCalib
    val pressureCalib: PressureCalib
    val h1: Double
    with(device.read(0x88U, 26)) { // calib00to25
        temperatureCalib = TemperatureCalib(
            t1 = toUShort(0),
            t2 = toShort(2),
            t3 = toShort(4),
        )
        pressureCalib = PressureCalib(
            p1 = toUShort(6),
            p2 = toShort(8),
            p3 = toShort(10),
            p4 = toShort(12),
            p5 = toShort(14),
            p6 = toShort(16),
            p7 = toShort(18),
            p8 = toShort(20),
            p9 = toShort(22),
        )
        h1 = this[25].toInt().toDouble()
    }
    val humidityCalib = with(device.read(0xE1U, 7)) { // calib26to32
        HumidityCalib(
            h1 = h1,
            h2 = toShort(0),
            h3 = this[2].toInt().toDouble(),
            h4 = ((this[3].toByte().toInt() shl 4) or (this[4].toInt() and 0x0F)).toDouble(),
            h5 = ((this[5].toByte().toInt() shl 4) or (this[4].toInt() shr 4)).toDouble(),
            h6 = this[6].toByte().toInt().toDouble(),
        )
    }
    return object : Bme280 {
        override suspend fun measurements(): Measurements {
            setupMode(device)
            /*
                Datasheet:
                    4. Data readout
                    Table 29: Register 0xF7 ... 0xF9 "press"
                    Table 30: Register 0xFA ... 0xFC "temp"
                    Table 31: Register 0xFD ... 0xFE "hum"
                    Table 18: Memory map
                API: parse_sensor_data
             */
            val adc = device.read(0xF7U, 8)
            fun toUShort(offset: Int) =
                ((adc[offset].toInt() shl 12) or (adc[offset + 1].toInt() shl 4) or (adc[offset + 2].toInt() shr 4)).toDouble()
            val (temperaturInCelsius, tFine) = temperatureCalib.compensate(toUShort(3))
            return Measurements(
                temperaturInCelsius = temperaturInCelsius,
                pressureInPascal = pressureCalib.compensate(toUShort(0), tFine),
                humidityInPercent = humidityCalib.compensate(((adc[6].toInt() shl 8) or adc[7].toInt()).toDouble(), tFine)
            )
        }
    }
}

@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("SpellCheckingInspection")

package ch.softappeal.kopi

import ch.softappeal.kopi.native.i2c.I2C_SLAVE
import ch.softappeal.kopi.native.i2c.i2c_smbus_read_byte
import ch.softappeal.kopi.native.i2c.i2c_smbus_read_byte_data
import ch.softappeal.kopi.native.i2c.i2c_smbus_read_i2c_block_data
import ch.softappeal.kopi.native.i2c.i2c_smbus_write_byte
import ch.softappeal.kopi.native.i2c.i2c_smbus_write_byte_data
import ch.softappeal.kopi.native.i2c.i2c_smbus_write_i2c_block_data
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.posix.O_RDWR
import platform.posix.ioctl
import platform.posix.open

/*
    https://www.kernel.org/doc/html/v5.5/i2c/smbus-protocol.html

    precondition: enable I2C in raspi-config

    i2cdetect -V
        i2cdetect version 4.3
    curl -o src/nativeInterop/cinterop/headers/i2c/smbus.h https://git.kernel.org/pub/scm/utils/i2c-tools/i2c-tools.git/plain/include/i2c/smbus.h\?h=v4.3
    scp guru@raspberrypi:/usr/include/linux/i2c-dev.h src/nativeInterop/cinterop/headers/linux/i2c-dev.h

    ldd /usr/sbin/i2cdetect
        libi2c.so.0 => /lib/aarch64-linux-gnu/libi2c.so.0 (0x00007fff45ac0000)
    scp guru@raspberrypi:/lib/aarch64-linux-gnu/libi2c.so.0 src/nativeInterop/cinterop/libs/libi2c.so
 */

public actual fun I2cBus(bus: Int): I2cBus {
    val file = open("/dev/i2c-$bus", O_RDWR)
    check(file >= 0) { "can't open I2C bus $bus" }
    val mutex = Mutex()
    var lastAddress = 0
    return object : I2cBus {
        override fun device(address: Int): I2cDevice {
            suspend fun <R> selectDevice(action: (file: Int) -> R) = mutex.withLock {
                if (lastAddress != address) {
                    check(ioctl(file, I2C_SLAVE.toULong(), address) == 0) { "can't communicate with I2C device $address" }
                    lastAddress = address
                }
                action(file)
            }
            return object : I2cDevice {
                override suspend fun write(value: UByte) = selectDevice { file ->
                    check(i2c_smbus_write_byte(file, value) == 0) { "i2c_smbus_write_byte with I2C device $address failed" }
                }

                override suspend fun read() = selectDevice { file ->
                    val value = i2c_smbus_read_byte(file)
                    check(value >= 0) { "i2c_smbus_read_byte with I2C device $address failed" }
                    value.toUByte()
                }

                override suspend fun write(register: UByte, value: UByte) = selectDevice { file ->
                    check(i2c_smbus_write_byte_data(file, register, value) == 0) {
                        "i2c_smbus_write_byte_data with I2C device $address failed"
                    }
                }

                override suspend fun read(register: UByte) = selectDevice { file ->
                    val value = i2c_smbus_read_byte_data(file, register)
                    check(value >= 0) { "i2c_smbus_read_byte_data with I2C device $address failed" }
                    value.toUByte()
                }

                override suspend fun write(register: UByte, values: UByteArray) = selectDevice { file ->
                    values.usePinned { pinned ->
                        check(i2c_smbus_write_i2c_block_data(file, register, values.size.toUByte(), pinned.addressOf(0)) == 0) {
                            "i2c_smbus_write_i2c_block_data with I2C device $address failed"
                        }
                    }
                }

                override suspend fun read(register: UByte, length: Int) = selectDevice { file ->
                    memScoped {
                        val buffer = allocArray<UByteVar>(length)
                        check(i2c_smbus_read_i2c_block_data(file, register, length.toUByte(), buffer) == length) {
                            "block read with I2C device $address failed"
                        }
                        UByteArray(length) { buffer[it] }
                    }
                }
            }
        }

        override fun close() {
            platform.posix.close(file)
        }
    }
}

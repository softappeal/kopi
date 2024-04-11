@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.softappeal.kopi.graphics

public data class Color(public val red: UByte, public val green: UByte, public val blue: UByte)

public val BLACK: Color = Color(0x00U, 0x00U, 0x00U)
public val WHITE: Color = Color(0xFFU, 0xFFU, 0xFFU)
public val RED: Color = Color(0xFFU, 0x00U, 0x00U)
public val GREEN: Color = Color(0x00U, 0xFFU, 0x00U)
public val BLUE: Color = Color(0x00U, 0x00U, 0xFFU)
public val CYAN: Color = Color(0x00U, 0xFFU, 0xFFU)
public val MAGENTA: Color = Color(0xFFU, 0x00U, 0xFFU)
public val YELLOW: Color = Color(0xFFU, 0xFFU, 0x00U)

public interface Display {
    public val width: Int
    public val height: Int
    public suspend fun update(buffer: UByteArray)
}

/**
 * (x       , y         ) pixel is right of x and below of y
 *
 * (0       , 0         ) is top    left
 * (width -1, 0         ) is top    right
 * (0       , height - 1) is bottom left
 * (width -1, height - 1) is bottom right
 */
public abstract class Graphics(private val display: Display) {
    public val width: Int = display.width
    public val height: Int = display.height

    protected abstract val buffer: UByteArray
    public suspend fun update() {
        display.update(buffer)
    }

    public abstract fun setPixel(x: Int, y: Int, color: Color)
}

public fun Graphics.fillRect(xTopLeft: Int, yTopLeft: Int, width: Int, height: Int, color: Color) {
    for (x in xTopLeft..<xTopLeft + width) {
        for (y in yTopLeft..<yTopLeft + height) {
            setPixel(x, y, color)
        }
    }
}

public fun Graphics.clear() {
    fillRect(0, 0, width, height, BLACK)
}

package ch.softappeal.kopi.graphics

public class Icon(internal val overlays: Overlays, internal val index: Int) : Dimensions(overlays)

public fun Graphics.draw(xTopLeft: Int, yTopLeft: Int, icon: Icon) {
    icon.overlays.draw(this, xTopLeft, yTopLeft, icon.index)
}

public fun Graphics.draw(topLeft: Point, icon: Icon) {
    draw(topLeft.x, topLeft.y, icon)
}

public fun Overlays.Icon(index: Int): Icon = Icon(this, index)
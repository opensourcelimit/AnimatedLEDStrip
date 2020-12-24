/*
 *  Copyright (c) 2018-2020 AnimatedLEDStrip
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package animatedledstrip.colors

import animatedledstrip.utils.base
import animatedledstrip.utils.blend
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * A class for storing colors that can be used in animations. This can store a
 * variable number of colors (stored as 24-bit `Int`s).
 *
 * Behavior when `Int`s outside the range `0..0xFFFFFF` are added is undefined.
 */
@Serializable
open class ColorContainer(
    final override val colors: MutableList<Int> = mutableListOf(),
) : ColorContainerInterface {

    /* Colors */

    /**
     * A `List` of `Long`s representing the animatedledstrip.colors
     */
//    final override val animatedledstrip.colors = mutableListOf<Long>()

    /**
     * A helper property that returns the first color in [colors]. If `animatedledstrip.colors`
     * is empty, returns 0 (black).
     */
    override val color: Int
        get() = this[0]

    /**
     * Tracks if there is only one color in [colors].
     */
    private val singleColor: Boolean
        get() = colors.size == 1


    /* Construction */

    constructor(vararg c: Int) : this() {
        for (i in c) colors += i
    }

    /**
     * Create a new ColorContainer with one color represented as a Triple
     * containing r, g, and b.
     */
    constructor(rgb: Triple<Int, Int, Int>)
            : this((rgb.first shl 16) or (rgb.second shl 8) or rgb.third)

    /**
     * Copy constructor
     */
    constructor(ccIn: ColorContainer) : this() {
        colors.addAll(ccIn.colors)
    }


    /* Get color */

    /**
     * Get one color from [colors]. If there is only one color ([singleColor]
     * returns true), this will return that color, regardless of what index
     * was sent. Otherwise, it checks if `index` is a valid index of `animatedledstrip.colors`
     * and if so, returns the color stored there, if not, returns 0 (black).
     */
    operator fun get(index: Int): Int =
        colors.getOrElse(index) { 0 }


    /**
     * Get animatedledstrip.colors from [colors]. Accepts a variable number of arguments(though
     * a single argument will be caught by the get() operator above). If no
     * indices are provided, this will return an empty list.
     * If multiple indices are provided but there is only one color
     * in the list, this will return a list containing only that one
     * color. If an index is not a valid index in `animatedledstrip.colors`, 0 is added
     * to the list. The returned list contains the animatedledstrip.colors in the order
     * specified.
     */
    operator fun get(vararg indices: Int): List<Int> =
        indices.map { colors.getOrElse(it) { 0 } }

    /**
     * Get animatedledstrip.colors from [colors]. If there is only one color in `animatedledstrip.colors`,
     * this will return a list containing only that one color. If an index
     * in the range is not a valid index in `animatedledstrip.colors`, 0 is added to the list.
     */
    operator fun get(indices: IntRange): List<Int> =
        indices.map { colors.getOrElse(it) { 0 } }


    /* Set color */

    /**
     * Set some indices of [colors] to [c]. If an index is not a valid index
     * in `animatedledstrip.colors`, this will add it to the end of `animatedledstrip.colors`, though not
     * necessarily at the index specified.
     */
    operator fun set(vararg indices: Int, c: Int) {
        for (index in indices.sorted())
            if (colors.indices.contains(index)) colors[index] = c
            else colors += c
    }

    /**
     * Set a range of indices of [colors] to [c]. If an index is not a valid
     * in [colors], this will add the color to the end of `animatedledstrip.colors`, though not
     * necessarily at the index specified.
     */
    operator fun set(indices: IntRange, c: Int) {
        for (index in indices)
            if (colors.indices.contains(index)) colors[index] = c
            else colors += c
    }

    /**
     * Adds a color at the end of [colors].
     */
    operator fun plusAssign(c: Int) {
        colors.add(c)
    }


    /* Preparation */

    /**
     * Create a collection of animatedledstrip.colors that blend between multiple animatedledstrip.colors along a 'strip'.
     *
     * The palette animatedledstrip.colors are spread out along the strip at approximately equal
     * intervals. All pixels between these 'pure' pixels are a blend between the
     * animatedledstrip.colors of the two nearest pure pixels. The blend ratio is determined by the
     * location of the pixel relative to the nearest pure pixels.
     *
     * @param numLEDs The number of LEDs to create animatedledstrip.colors for
     * @return A [PreparedColorContainer] containing all the animatedledstrip.colors
     */
    override fun prepare(numLEDs: Int): PreparedColorContainer {
        require(numLEDs > 0)
        val returnMap = mutableMapOf<Int, Int>()
        val spacing = numLEDs.toDouble() / colors.size.toDouble()
        val purePixels = (0 until colors.size).map { (spacing * it).roundToInt() }

        for (i in 0 until numLEDs) {
            for (p in purePixels) {
                if ((i - p) < spacing) {
                    val pIndex = purePixels.indexOf(p)
                    val d =
                        if (pIndex < purePixels.size - 1) purePixels[pIndex + 1] - p
                        else numLEDs - p

                    if ((i - p) == 0) // We are on a pure pixel
                        returnMap[i] = colors[pIndex]
                    else
                        returnMap[i] = blend(
                            colors[pIndex],
                            colors[(pIndex + 1) % purePixels.size],
                            (((i - p) / d.toDouble()) * 255).toInt(),
                        )
                    break
                }
            }
        }

        return PreparedColorContainer(returnMap.values.toMutableList(), colors)
    }


    /* Conversion */

    /**
     * Create a string representation of this ColorContainer.
     * The hexadecimal representation of each color in [colors] is
     * listed in comma delimited format, between brackets `[` & `]`
     * If there is only one color in this ColorContainer, the brackets
     * are dropped.
     */
    override fun toString(): String {
        return if (singleColor) color.toString(16)
        else colors.joinToString(separator = ", ", prefix = "[", postfix = "]") { it base 16 }
    }

    /**
     * Returns the first color in [colors].
     */
    fun toInt(): Int = color

    /**
     * Returns the first color in [colors] a Triple containing r, g, b.
     */
    fun toRGB(): Triple<Int, Int, Int> = Triple(
        color shr 16 and 0xFF,
        color shr 8 and 0xFF,
        color and 0xFF
    )

    /**
     * Calls toRGB()
     */
    fun toTriple() = toRGB()

    /**
     * @return This ColorContainer instance
     */
    override fun toColorContainer(): ColorContainer = this


    /* Operators/Other */

    /**
     * Compares this ColorContainer against another ColorContainer or a Long.
     * If [other] is a ColorContainer, the [colors] parameters are compared.
     * If [other] is a Long, the [color] parameter is compared to the Long.
     */
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is ColorContainer -> other.colors == this.colors
            is PreparedColorContainer -> other.originalColors == this.colors
            is Int -> singleColor && other == this.color
            else -> super.equals(other)
        }
    }

    /**
     * @return The hashCode of [colors]
     */
    override fun hashCode(): Int = colors.hashCode()

    /**
     * @return The iterator for [colors]
     */
    operator fun iterator() = colors.iterator()

    /**
     * Checks if the specified color (Long) is in [colors].
     */
    operator fun contains(c: Int): Boolean = colors.contains(c)

    /**
     * @return The size of [colors].
     */
    val size: Int
        get() = colors.size

}

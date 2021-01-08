/*
 * Copyright (c) 2018-2021 AnimatedLEDStrip
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package animatedledstrip.test.leds.locationmanagement

import animatedledstrip.leds.locationmanagement.Location
import animatedledstrip.leds.locationmanagement.PixelLocation
import animatedledstrip.leds.locationmanagement.PixelLocationManager
import animatedledstrip.utils.TestLogger
import co.touchlab.kermit.Severity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import kotlin.math.abs

class PixelLocationManagerTest : StringSpec(
    {
        val largeDoubleArb: Arb<Double> = arbitrary { rs ->
            val i = rs.random.nextInt(Int.MIN_VALUE + 1, Int.MAX_VALUE - 1)
            return@arbitrary if (i < 0) i - rs.random.nextDouble()
            else i + rs.random.nextDouble()
        }

        val locationArb: Arb<Location> =
            Arb.bind(largeDoubleArb,
                     largeDoubleArb,
                     largeDoubleArb) { x, y, z -> Location(x, y, z) }

        "constructor locations defined" {
            checkAll(Arb.list(locationArb, 1..5000)) { locs ->
                val newManager = PixelLocationManager(locs, locs.size)
                for ((index, location) in newManager.pixelLocations.withIndex())
                    location shouldBe PixelLocation(index, locs[index])
            }
        }

        "constructor location conflict" {
            shouldThrow<IllegalArgumentException> {
                PixelLocationManager(listOf(Location(1, 2, 3), Location(1, 2, 3)), 2)
            }
        }

        "constructor no locations defined" {
            checkAll(Arb.int(1..5000)) { nLocs ->
                TestLogger.startLogCapture()
                val newManager = PixelLocationManager(null, nLocs)
                for ((index, location) in newManager.pixelLocations.withIndex())
                    location shouldBe PixelLocation(index, Location(index))
                TestLogger.logs.shouldContainExactly(
                    TestLogger.Log(Severity.Warn,
                                   "No LED locations defined, assuming LEDs are in one dimensional strip with equal spacing",
                                   "Pixel Location Manager"))
                TestLogger.stopLogCapture()
            }
        }

        "min max avg" {
            checkAll(100, Arb.list(locationArb, 1..5000)) { locs ->
                val newManager = PixelLocationManager(locs, locs.size)
                newManager.xMin shouldBe locs.minByOrNull { it.x }?.x
                newManager.xMax shouldBe locs.maxByOrNull { it.x }?.x
                newManager.yMin shouldBe locs.minByOrNull { it.y }?.y
                newManager.yMax shouldBe locs.maxByOrNull { it.y }?.y
                newManager.zMin shouldBe locs.minByOrNull { it.z }?.z
                newManager.zMax shouldBe locs.maxByOrNull { it.z }?.z
                newManager.xAvg shouldBe (((locs.minByOrNull { it.x }?.x ?: 0.0) +
                                           (locs.maxByOrNull { it.x }?.x ?: 0.0)) / 2 plusOrMinus 0.01)
                newManager.yAvg shouldBe (((locs.minByOrNull { it.y }?.y ?: 0.0) +
                                           (locs.maxByOrNull { it.y }?.y ?: 0.0)) / 2 plusOrMinus 0.01)
                newManager.zAvg shouldBe (((locs.minByOrNull { it.z }?.z ?: 0.0) +
                                           (locs.maxByOrNull { it.z }?.z ?: 0.0)) / 2 plusOrMinus 0.01)
            }
        }

        "default location" {
            checkAll(100, Arb.list(locationArb, 1..5000)) { locs ->
                val newManager = PixelLocationManager(locs, locs.size)
                newManager.defaultLocation.x shouldBe (((locs.minByOrNull { it.x }?.x ?: 0.0) +
                                                        (locs.maxByOrNull { it.x }?.x ?: 0.0)) / 2 plusOrMinus 0.01)
                newManager.defaultLocation.y shouldBe (((locs.minByOrNull { it.y }?.y ?: 0.0) +
                                                        (locs.maxByOrNull { it.y }?.y ?: 0.0)) / 2 plusOrMinus 0.01)
                newManager.defaultLocation.z shouldBe (((locs.minByOrNull { it.z }?.z ?: 0.0) +
                                                        (locs.maxByOrNull { it.z }?.z ?: 0.0)) / 2 plusOrMinus 0.01)
            }
        }

        "default distance" {
            checkAll(100, Arb.list(locationArb, 1..5000)) { locs ->
                val newManager = PixelLocationManager(locs, locs.size)
                newManager.defaultDistance.x shouldBe ((abs(locs.minByOrNull { it.x }?.x ?: 0.0) +
                                                        abs(locs.maxByOrNull { it.x }?.x ?: 0.0)) plusOrMinus 0.01)
                newManager.defaultDistance.y shouldBe ((abs(locs.minByOrNull { it.y }?.y ?: 0.0) +
                                                        abs(locs.maxByOrNull { it.y }?.y ?: 0.0)) plusOrMinus 0.01)
                newManager.defaultDistance.z shouldBe ((abs(locs.minByOrNull { it.z }?.z ?: 0.0) +
                                                        abs(locs.maxByOrNull { it.z }?.z ?: 0.0)) plusOrMinus 0.01)
            }
        }

        "random location" {
            checkAll(Arb.list(locationArb, 1..500)) { locs ->
                val newManager = PixelLocationManager(locs, locs.size)
                val rLoc = newManager.randomLocation()
                rLoc.x shouldBeGreaterThanOrEqual newManager.xMin
                rLoc.x shouldBeLessThanOrEqual newManager.xMax
                rLoc.y shouldBeGreaterThanOrEqual newManager.yMin
                rLoc.y shouldBeLessThanOrEqual newManager.yMax
                rLoc.z shouldBeGreaterThanOrEqual newManager.zMin
                rLoc.z shouldBeLessThanOrEqual newManager.zMax
            }
        }

    }
)

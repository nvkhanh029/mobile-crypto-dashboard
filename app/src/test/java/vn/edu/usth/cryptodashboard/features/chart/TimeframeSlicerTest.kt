package vn.edu.usth.cryptodashboard.features.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import vn.edu.usth.cryptodashboard.domain.model.ChartPoint
import vn.edu.usth.cryptodashboard.domain.model.Timeframe

class TimeframeSlicerTest {

    private val latest = 1_700_000_000_000L

    private fun point(minutesBeforeLatest: Long, price: Double = 1.0): ChartPoint =
        ChartPoint(timestampMillis = latest - minutesBeforeLatest * 60_000L, price = price)

    @Test
    fun `h1 keeps only points within 60 minutes before the latest point`() {
        val points = listOf(
            point(120, 1.0),
            point(61, 2.0),
            point(60, 3.0),
            point(30, 4.0),
            point(0, 5.0),
        )

        val sliced = TimeframeSlicer.slice(points, Timeframe.H1)

        assertEquals(listOf(3.0, 4.0, 5.0), sliced.map { it.price })
    }

    @Test
    fun `h1 includes the point exactly 60 minutes before the latest point`() {
        val boundary = point(60, 1.0)
        val points = listOf(point(61, 2.0), boundary, point(0, 3.0))

        val sliced = TimeframeSlicer.slice(points, Timeframe.H1)

        assertEquals(listOf(1.0, 3.0), sliced.map { it.price })
    }

    @Test
    fun `h1 uses the latest point of the dataset, not the phone clock`() {
        // All points are old (captured fake data). The rule must still keep the last hour.
        val points = listOf(point(90, 1.0), point(30, 2.0), point(0, 3.0))

        val sliced = TimeframeSlicer.slice(points, Timeframe.H1)

        assertEquals(listOf(2.0, 3.0), sliced.map { it.price })
    }

    @Test
    fun `h1 finds the latest timestamp even when it is not the last element`() {
        // The repository contract promises time order, but the slicer must not depend on the
        // latest point happening to be the last element.
        val points = listOf(point(30, 1.0), point(0, 2.0), point(120, 3.0))

        val sliced = TimeframeSlicer.slice(points, Timeframe.H1)

        assertEquals(listOf(1.0, 2.0), sliced.map { it.price })
    }

    @Test
    fun `h1 keeps a single recent point`() {
        val points = listOf(point(90, 1.0), point(0, 2.0))

        val sliced = TimeframeSlicer.slice(points, Timeframe.H1)

        assertEquals(listOf(2.0), sliced.map { it.price })
    }

    @Test
    fun `h1 returns an empty list unchanged`() {
        val points = emptyList<ChartPoint>()

        assertSame(points, TimeframeSlicer.slice(points, Timeframe.H1))
    }

    @Test
    fun `other timeframes return the whole list unchanged`() {
        val points = listOf(point(120, 1.0), point(30, 2.0), point(0, 3.0))

        listOf(Timeframe.H24, Timeframe.D7, Timeframe.M1, Timeframe.Y1).forEach { timeframe ->
            assertSame(points, TimeframeSlicer.slice(points, timeframe))
        }
    }

    @Test
    fun `h1 preserves the time order of the input`() {
        val points = listOf(point(50, 1.0), point(20, 2.0), point(0, 3.0))

        val sliced = TimeframeSlicer.slice(points, Timeframe.H1)

        assertEquals(points, sliced)
    }
}

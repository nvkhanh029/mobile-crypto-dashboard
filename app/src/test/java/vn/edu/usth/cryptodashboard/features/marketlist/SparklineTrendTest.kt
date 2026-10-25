package vn.edu.usth.cryptodashboard.features.marketlist

import org.junit.Assert.assertEquals
import org.junit.Test

class SparklineTrendTest {
    @Test fun missingEmptyAndSinglePointAreNeutral() {
        for (points in listOf(null, emptyList(), listOf(5.0))) {
            assertEquals(SparklineTrend.NEUTRAL, sparklineTrend(points))
        }
    }

    @Test fun higherLastPointIsPositiveDespiteIntermediateFall() {
        assertEquals(SparklineTrend.POSITIVE, sparklineTrend(listOf(3.0, 1.0, 4.0)))
    }

    @Test fun lowerLastPointIsNegativeDespiteIntermediateRise() {
        assertEquals(SparklineTrend.NEGATIVE, sparklineTrend(listOf(3.0, 5.0, 2.0)))
    }

    @Test fun equalEndpointsAreNeutralDespiteIntermediateChanges() {
        assertEquals(SparklineTrend.NEUTRAL, sparklineTrend(listOf(3.0, 1.0, 5.0, 3.0)))
    }

    @Test fun flatLineIsNeutral() {
        assertEquals(SparklineTrend.NEUTRAL, sparklineTrend(listOf(3.0, 3.0, 3.0)))
    }

    @Test fun nonFinitePointAnywhereMakesTrendNeutral() {
        for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            for (points in listOf(listOf(invalid, 2.0), listOf(1.0, invalid), listOf(1.0, invalid, 2.0))) {
                assertEquals(SparklineTrend.NEUTRAL, sparklineTrend(points))
            }
        }
    }
}

package com.technogizguy.voltra.controller

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IsometricMetricsTest {
    @Test
    fun computesDerivedMetricsFromRealtimeSamples() {
        val metrics = computeIsometricMetrics(
            listOf(
                IsometricForceSample(elapsedMillis = 0L, forceN = 0.0),
                IsometricForceSample(elapsedMillis = 50L, forceN = 25.0),
                IsometricForceSample(elapsedMillis = 100L, forceN = 50.0),
                IsometricForceSample(elapsedMillis = 300L, forceN = 120.0),
                IsometricForceSample(elapsedMillis = 700L, forceN = 115.0),
            ),
        )

        assertEquals(115.0, metrics.currentForceN!!, 0.001)
        assertEquals(120.0, metrics.peakForceN!!, 0.001)
        assertEquals(700L, metrics.durationMillis)
        assertEquals(300L, metrics.timeToPeakMillis)
        assertEquals(500.0, metrics.rfd100Ns!!, 0.001)
        assertEquals(2.5, metrics.impulse100Ns!!, 0.001)
        assertEquals(276.0, metrics.graphMaxForceN, 0.001)
    }

    @Test
    fun leavesWindowMetricsEmptyUntilEnoughSamplesExist() {
        val metrics = computeIsometricMetrics(
            listOf(
                IsometricForceSample(elapsedMillis = 0L, forceN = 0.0),
                IsometricForceSample(elapsedMillis = 60L, forceN = 15.0),
            ),
        )

        assertEquals(15.0, metrics.currentForceN!!, 0.001)
        assertEquals(15.0, metrics.peakForceN!!, 0.001)
        assertEquals(60L, metrics.durationMillis)
        assertEquals(60L, metrics.timeToPeakMillis)
        assertNull(metrics.rfd100Ns)
        assertNull(metrics.impulse100Ns)
    }

    @Test
    fun dampensSingleSparsePeakSpikeWithoutChangingCurrentForce() {
        val metrics = computeIsometricMetrics(
            listOf(
                IsometricForceSample(elapsedMillis = 0L, forceN = 82.0),
                IsometricForceSample(elapsedMillis = 900L, forceN = 141.0),
                IsometricForceSample(elapsedMillis = 1_700L, forceN = 252.0),
                IsometricForceSample(elapsedMillis = 3_000L, forceN = 122.0),
            ),
        )

        assertEquals(122.0, metrics.currentForceN!!, 0.001)
        assertEquals(179.85, metrics.peakForceN!!, 0.001)
        assertEquals(3_000L, metrics.durationMillis)
        assertEquals(1_700L, metrics.timeToPeakMillis)
    }

    @Test
    fun keepsNaturalSparseHillPeakWhenNeighborsSupportIt() {
        val metrics = computeIsometricMetrics(
            listOf(
                IsometricForceSample(elapsedMillis = 0L, forceN = 87.0),
                IsometricForceSample(elapsedMillis = 100L, forceN = 123.0),
                IsometricForceSample(elapsedMillis = 200L, forceN = 174.0),
                IsometricForceSample(elapsedMillis = 5_900L, forceN = 122.0),
                IsometricForceSample(elapsedMillis = 6_000L, forceN = 86.0),
            ),
        )

        assertEquals(86.0, metrics.currentForceN!!, 0.001)
        assertEquals(174.0, metrics.peakForceN!!, 0.001)
        assertEquals(6_000L, metrics.durationMillis)
        assertEquals(200L, metrics.timeToPeakMillis)
    }
}

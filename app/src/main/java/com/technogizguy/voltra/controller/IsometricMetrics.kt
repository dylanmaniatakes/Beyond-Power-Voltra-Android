package com.technogizguy.voltra.controller

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class IsometricForceSample(
    val elapsedMillis: Long,
    val forceN: Double,
)

data class IsometricComputedMetrics(
    val currentForceN: Double?,
    val peakForceN: Double?,
    val durationMillis: Long?,
    val timeToPeakMillis: Long?,
    val rfd50Ns: Double?,
    val rfd100Ns: Double?,
    val rfd150Ns: Double?,
    val rfd200Ns: Double?,
    val peakRfd100Ns: Double?,
    val impulse100Ns: Double?,
    val graphMaxForceN: Double,
)

fun computeIsometricMetrics(samples: List<IsometricForceSample>): IsometricComputedMetrics {
    if (samples.isEmpty()) {
        return IsometricComputedMetrics(
            currentForceN = null,
            peakForceN = null,
            durationMillis = null,
            timeToPeakMillis = null,
            rfd50Ns = null,
            rfd100Ns = null,
            rfd150Ns = null,
            rfd200Ns = null,
            peakRfd100Ns = null,
            impulse100Ns = null,
            graphMaxForceN = DEFAULT_ISOMETRIC_GRAPH_MAX_FORCE_N,
        )
    }

    val ordered = samples
        .sortedBy { it.elapsedMillis }
        .fold(mutableListOf<IsometricForceSample>()) { acc, sample ->
            if (acc.isNotEmpty() && acc.last().elapsedMillis == sample.elapsedMillis) {
                acc[acc.lastIndex] = sample
            } else {
                acc += sample
            }
            acc
        }

    val offset = ordered.first().elapsedMillis
    val normalized = ordered.map {
        IsometricForceSample(
            elapsedMillis = (it.elapsedMillis - offset).coerceAtLeast(0L),
            forceN = it.forceN.coerceAtLeast(0.0),
        )
    }

    val current = normalized.last()
    val rawPeak = normalized.maxByOrNull { it.forceN }
    val peak = rawPeak?.let { adjustSparsePeakSample(normalized, it) }
    val graphMax = computeGraphMaxForceN(normalized)
    if (peak == null || peak.forceN < MIN_MEANINGFUL_ISOMETRIC_FORCE_N) {
        return IsometricComputedMetrics(
            currentForceN = current.forceN,
            peakForceN = peak?.forceN,
            durationMillis = current.elapsedMillis,
            timeToPeakMillis = null,
            rfd50Ns = null,
            rfd100Ns = null,
            rfd150Ns = null,
            rfd200Ns = null,
            peakRfd100Ns = null,
            impulse100Ns = null,
            graphMaxForceN = graphMax,
        )
    }

    val durationMillis = current.elapsedMillis
    val timeToPeakMillis = peak.elapsedMillis
    val rfd50Ns = rfdFromStart(normalized, ISOMETRIC_WINDOW_50MS)
    val rfd100Ns = rfdFromStart(normalized, ISOMETRIC_WINDOW_100MS)
    val rfd150Ns = rfdFromStart(normalized, ISOMETRIC_WINDOW_150MS)
    val rfd200Ns = rfdFromStart(normalized, ISOMETRIC_WINDOW_200MS)
    val peakRfd100Ns = peakRollingRfd(normalized, ISOMETRIC_WINDOW_100MS)
    val impulse100Ns = if (durationMillis >= ISOMETRIC_WINDOW_100MS && normalized.size >= 2) {
        (integrateForceUntil(normalized, ISOMETRIC_WINDOW_100MS) / 1000.0).coerceAtLeast(0.0)
    } else {
        null
    }

    return IsometricComputedMetrics(
        currentForceN = current.forceN,
        peakForceN = peak.forceN,
        durationMillis = durationMillis,
        timeToPeakMillis = timeToPeakMillis,
        rfd50Ns = rfd50Ns,
        rfd100Ns = rfd100Ns,
        rfd150Ns = rfd150Ns,
        rfd200Ns = rfd200Ns,
        peakRfd100Ns = peakRfd100Ns,
        impulse100Ns = impulse100Ns,
        graphMaxForceN = graphMax,
    )
}

private fun adjustSparsePeakSample(
    samples: List<IsometricForceSample>,
    rawPeak: IsometricForceSample,
): IsometricForceSample {
    if (samples.size !in 4..8) return rawPeak
    val peakIndex = samples.indexOfFirst {
        it.elapsedMillis == rawPeak.elapsedMillis && it.forceN == rawPeak.forceN
    }
    if (peakIndex <= 0 || peakIndex >= samples.lastIndex) return rawPeak

    val previous = samples[peakIndex - 1]
    val next = samples[peakIndex + 1]
    val neighborMax = max(previous.forceN, next.forceN)
    val neighborMin = min(previous.forceN, next.forceN)
    val isolatedSpike =
        rawPeak.forceN >= neighborMax * SPARSE_PEAK_SPIKE_RATIO &&
            (rawPeak.forceN - neighborMin) >= SPARSE_PEAK_SPIKE_MIN_DELTA_N
    if (!isolatedSpike) return rawPeak

    val adjustedForce =
        neighborMax + ((rawPeak.forceN - neighborMax) * SPARSE_PEAK_RETAIN_FACTOR)
    return rawPeak.copy(forceN = adjustedForce)
}

private fun computeGraphMaxForceN(samples: List<IsometricForceSample>): Double {
    val peakForce = samples.maxOfOrNull { it.forceN } ?: 0.0
    if (peakForce <= DEFAULT_ISOMETRIC_GRAPH_MAX_FORCE_N) {
        return DEFAULT_ISOMETRIC_GRAPH_MAX_FORCE_N
    }
    return ceil(peakForce / ISOMETRIC_GRAPH_STEP_FORCE_N) * ISOMETRIC_GRAPH_STEP_FORCE_N
}

private fun interpolateForceAt(
    samples: List<IsometricForceSample>,
    targetMillis: Long,
): Double {
    val first = samples.first()
    if (targetMillis <= first.elapsedMillis) return first.forceN
    var previous = first
    for (index in 1 until samples.size) {
        val current = samples[index]
        if (targetMillis <= current.elapsedMillis) {
            val span = (current.elapsedMillis - previous.elapsedMillis).coerceAtLeast(1L).toDouble()
            val progress = (targetMillis - previous.elapsedMillis) / span
            return previous.forceN + ((current.forceN - previous.forceN) * progress)
        }
        previous = current
    }
    return samples.last().forceN
}

private fun rfdFromStart(
    samples: List<IsometricForceSample>,
    windowMillis: Long,
): Double? {
    if (samples.size < 2) return null
    val durationMillis = samples.last().elapsedMillis
    if (durationMillis < windowMillis) return null
    val startForce = samples.first().forceN
    val forceAtWindow = interpolateForceAt(samples, windowMillis)
    return ((forceAtWindow - startForce) / (windowMillis / 1000.0)).coerceAtLeast(0.0)
}

private fun peakRollingRfd(
    samples: List<IsometricForceSample>,
    windowMillis: Long,
): Double? {
    if (samples.size < 2) return null
    val lastMillis = samples.last().elapsedMillis
    var best: Double? = null
    samples.forEach { sample ->
        val targetMillis = sample.elapsedMillis + windowMillis
        if (targetMillis <= lastMillis) {
            val forceAtWindow = interpolateForceAt(samples, targetMillis)
            val rfd = ((forceAtWindow - sample.forceN) / (windowMillis / 1000.0)).coerceAtLeast(0.0)
            best = max(best ?: rfd, rfd)
        }
    }
    return best
}

private fun integrateForceUntil(
    samples: List<IsometricForceSample>,
    targetMillis: Long,
): Double {
    if (samples.size < 2) return 0.0
    var areaNMillis = 0.0
    var previous = samples.first()
    for (index in 1 until samples.size) {
        val current = samples[index]
        if (targetMillis <= previous.elapsedMillis) break
        val segmentEnd = minOf(current.elapsedMillis, targetMillis)
        if (segmentEnd > previous.elapsedMillis) {
            val endForce = if (segmentEnd == current.elapsedMillis) {
                current.forceN
            } else {
                interpolateForceAt(listOf(previous, current), segmentEnd)
            }
            val durationMillis = (segmentEnd - previous.elapsedMillis).toDouble()
            areaNMillis += ((previous.forceN + endForce) / 2.0) * durationMillis
        }
        if (current.elapsedMillis >= targetMillis) {
            break
        }
        previous = current
    }
    return areaNMillis
}

private const val DEFAULT_ISOMETRIC_GRAPH_MAX_FORCE_N = 276.0
private const val ISOMETRIC_GRAPH_STEP_FORCE_N = 69.0
private const val ISOMETRIC_WINDOW_50MS = 50L
private const val ISOMETRIC_WINDOW_100MS = 100L
private const val ISOMETRIC_WINDOW_150MS = 150L
private const val ISOMETRIC_WINDOW_200MS = 200L
private const val MIN_MEANINGFUL_ISOMETRIC_FORCE_N = 1.0
private const val SPARSE_PEAK_SPIKE_RATIO = 1.55
private const val SPARSE_PEAK_SPIKE_MIN_DELTA_N = 60.0
private const val SPARSE_PEAK_RETAIN_FACTOR = 0.35

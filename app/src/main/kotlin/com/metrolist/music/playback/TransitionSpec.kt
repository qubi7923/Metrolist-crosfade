package com.metrolist.music.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class FadeCurve { QUADRATIC, LINEAR, EQUAL_POWER }

data class TransitionSpec(
    val durationMs: Long,
    val curve: FadeCurve = FadeCurve.EQUAL_POWER,
    val nextStartMs: Long = 0L,
)

/** Zwraca (fadeIn, fadeOut) dla postępu 0..1. */
fun FadeCurve.gains(progress: Float): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    return when (this) {
        // dotychczasowa krzywa z Metrolist
        FadeCurve.QUADRATIC -> (1f - (1f - p) * (1f - p)) to ((1f - p) * (1f - p))
        FadeCurve.LINEAR -> p to (1f - p)
        FadeCurve.EQUAL_POWER ->
            sin(p * PI.toFloat() / 2f) to cos(p * PI.toFloat() / 2f)
    }
}
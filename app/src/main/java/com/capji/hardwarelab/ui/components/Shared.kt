package com.capji.hardwarelab.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Shared-bounds transition between a dashboard card and its test screen. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCard(key: String): Modifier {
    val shared = LocalSharedScope.current
    val anim = LocalAnimScope.current
    if (shared == null || anim == null) return this
    return with(shared) {
        this@sharedCard.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = anim,
        )
    }
}

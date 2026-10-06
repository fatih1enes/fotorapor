package com.fatihenes.photoreport.feature.camera.ui.viewfinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.fatihenes.photoreport.feature.camera.theme.CameraTokens

@Composable
fun ShutterBlinkFeedback(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(20)),
        exit = fadeOut(tween(60)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        )
    }
}

@Composable
fun FreezeFrameTransition(
    isTransitioning: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isTransitioning,
        enter = fadeIn(tween(CameraTokens.CrossfadeDuration)),
        exit = fadeOut(tween(CameraTokens.CrossfadeDuration)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )
    }
}

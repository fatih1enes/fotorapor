package com.fatihenes.photoreport.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fatihenes.photoreport.R
import kotlinx.coroutines.delay

@Composable
fun LaunchAnimationOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    
    // Apple/Opal style smooth motion profile
    val premiumEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    
    val logoScale = remember { Animatable(0.85f) }
    val logoAlpha = remember { Animatable(0f) }

    // Animate Logo In
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = premiumEasing)
        )
    }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = premiumEasing)
        )
        // Brief deliberate pause before transitioning out
        delay(150)
        isVisible = false
    }

    // Trigger onComplete after the fadeOut animation finishes
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(300) // matches the fadeOut duration
            onComplete()
        }
    }

    // We don't need a fadeIn for the overlay itself because it covers the screen from the start
    AnimatedVisibility(
        visible = isVisible,
        exit = fadeOut(animationSpec = tween(durationMillis = 300, easing = premiumEasing)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )
        }
    }
}

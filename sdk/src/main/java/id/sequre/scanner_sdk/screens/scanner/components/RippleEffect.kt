package id.sequre.scanner_sdk.screens.scanner.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


fun Modifier.detectTapGestureWithRipple(
    rippleScope: CoroutineScope,
    onTap: (Offset) -> Unit,
    rippleRadius1: Animatable<Float, AnimationVector1D>,
    rippleAlpha1: Animatable<Float, AnimationVector1D>,
    rippleRadius2: Animatable<Float, AnimationVector1D>,
    rippleAlpha2: Animatable<Float, AnimationVector1D>,
): Modifier = pointerInput(Unit) {
    detectTapGestures { tapOffset ->
        // Trigger tap callback
        onTap(tapOffset)

        // Start ripple animation
        rippleScope.launch {
            launch {
                rippleRadius1.snapTo(80f)
                rippleAlpha1.snapTo(1f)

                rippleRadius1.animateTo(
                    targetValue = 50f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
            launch {
                rippleRadius2.snapTo(30f)
                rippleAlpha2.snapTo(0.3f)

                rippleRadius2.animateTo(
                    targetValue = 50f,
                    animationSpec = tween(durationMillis = 200)
                )
                rippleAlpha2.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500)
                )
            }
            delay(200)
            launch {
                rippleAlpha1.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 500)
                )
            }
        }
    }
}


@Composable
fun RippleEffectCanvas(
    rippleCenter: Offset,
    rippleRadius1: Float,
    rippleAlpha1: Float,
    rippleRadius2: Float,
    rippleAlpha2: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // First ripple
        drawCircle(
            color = Color.White.copy(alpha = rippleAlpha1),
            radius = rippleRadius1,
            center = rippleCenter,
            style = Stroke(width = 4f) // Border-only ripple
        )
        // Second ripple
        drawCircle(
            color = Color.White.copy(alpha = rippleAlpha2),
            radius = rippleRadius2,
            center = rippleCenter
        )
    }
}
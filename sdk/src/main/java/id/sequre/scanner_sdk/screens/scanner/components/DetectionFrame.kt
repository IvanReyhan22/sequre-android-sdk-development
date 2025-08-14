package id.sequre.scanner_sdk.screens.scanner.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.ui.theme.BlackAlpha50
import id.sequre.scanner_sdk.ui.theme.DarkBlue
import id.sequre.scanner_sdk.ui.theme.LimeGreen
import id.sequre.scanner_sdk.ui.theme.White
import org.tensorflow.lite.task.vision.detector.Detection

data class DetectionFrameColors(
    val detectionFrameColor: Color = DarkBlue,
    val detectionFrameOptimalColor: Color = LimeGreen,
    val overlayColor: Color = BlackAlpha50,
)

@Composable
fun DetectionFrame(
    cutoutSize: Size = Size(0f, 0f),
    objectProximityStatus: ObjectProximityStatus = ObjectProximityStatus.UNDETECTED,
    detectedObjectSize: android.util.Size = android.util.Size(0, 0),
    detectionResults: List<Detection> = listOf(),
    showDetectedBoundary: Boolean = false,
    colors: DetectionFrameColors = DetectionFrameColors(),
    isFrameSquare: Boolean,
    modifier: Modifier = Modifier
) {
    val targetWidth = remember { mutableFloatStateOf(0f) }
    val targetHeight = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isFrameSquare) {
        targetWidth.floatValue = if (isFrameSquare) {
            cutoutSize.width+(cutoutSize.width/6)
        } else {
            cutoutSize.width
        }

        targetHeight.floatValue = if (isFrameSquare) {
            targetWidth.floatValue
        } else {
            cutoutSize.height
        }
    }

    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth.floatValue,
        animationSpec = tween(durationMillis = 200), // Optional: Customize animation duration/spec
        label = "animatedWidth" // Optional: Label for inspection
    )

    val animatedHeight by animateFloatAsState(
        targetValue = targetHeight.floatValue,
        animationSpec = tween(durationMillis = 200), // Optional: Customize animation duration/spec
        label = "animatedHeight" // Optional: Label for inspection
    )

    val dynamicDetectedObjectSize = if (isFrameSquare) {
        android.util.Size(detectedObjectSize.width, detectedObjectSize.width) // Avoid division by zero
    } else {
        detectedObjectSize
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        /// cutout overlay
        QrCutoutOverlay(
            cutOutWidth = animatedWidth,
            cutOutHeight = animatedHeight,
            indicatorColor =
            if (objectProximityStatus == ObjectProximityStatus.OPTIMAL)
                colors.detectionFrameOptimalColor
            else
                colors.detectionFrameColor,
            overlayColor = colors.overlayColor,
        )

        /// object indicator
        if (showDetectedBoundary) {
            ObjectIndicator(
                containerWidth = this.maxWidth.value,
                containerHeight = this.maxHeight.value,
                detectedObjectSize = dynamicDetectedObjectSize,
                detectionResults = detectionResults,
                isFrameSquare = isFrameSquare,
                indicatorColor =
                    if (objectProximityStatus == ObjectProximityStatus.OPTIMAL)
                        colors.detectionFrameOptimalColor
                    else
                        White,
            )
        }

        LabelView(
            cutoutSize = cutoutSize,
            detectionStatus = objectProximityStatus,
        )
    }
}
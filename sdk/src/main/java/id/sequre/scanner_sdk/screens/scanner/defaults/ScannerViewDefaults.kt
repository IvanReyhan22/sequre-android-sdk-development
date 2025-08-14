package id.sequre.scanner_sdk.screens.scanner.defaults

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import id.sequre.scanner_sdk.ui.theme.DarkBlue
import id.sequre.scanner_sdk.ui.theme.LimeGreen


object ScannerViewDefaults{
    internal val defaultScannerViewColors: ScannerViewColors
        get() {
            return ScannerViewColors()
        }

    fun scannerViewColors(
        detectionFrameColor: Color = defaultScannerViewColors.detectionFrameColor,
        detectionFrameOptimalColor: Color = defaultScannerViewColors.detectionFrameOptimalColor,
        surfaceColor: Color = defaultScannerViewColors.surfaceColor,
        onSurfaceColor: Color = defaultScannerViewColors.onSurfaceColor,
        appBarContainerColor: Color = defaultScannerViewColors.appBarContainerColor,
        appBarContentColor: Color = defaultScannerViewColors.appBarContentColor,
        glareIndicatorContainerColor: Color = defaultScannerViewColors.glareIndicatorContainerColor,
        glareIndicatorContentColor: Color = defaultScannerViewColors.glareIndicatorContentColor,
    ): ScannerViewColors {
        return defaultScannerViewColors.copy(
            detectionFrameColor = detectionFrameColor,
            detectionFrameOptimalColor = detectionFrameOptimalColor,
            surfaceColor = surfaceColor,
            onSurfaceColor = onSurfaceColor,
            appBarContainerColor = appBarContainerColor,
            appBarContentColor = appBarContentColor,
            glareIndicatorContainerColor = glareIndicatorContainerColor,
            glareIndicatorContentColor = glareIndicatorContentColor,
        )
    }
    val flashButton: @Composable (() -> Unit, Boolean, Color) -> Unit = { onFlashButtonClick, isFlashLightOn, color ->

        DefaultFlashButton(
            onClick = onFlashButtonClick,
            isFlashLightOn = isFlashLightOn,
            color = color
        )
    }
}
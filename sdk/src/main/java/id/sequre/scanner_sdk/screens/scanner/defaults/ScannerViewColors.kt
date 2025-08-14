package id.sequre.scanner_sdk.screens.scanner.defaults

import androidx.compose.ui.graphics.Color
import id.sequre.scanner_sdk.ui.theme.DarkBlue
import id.sequre.scanner_sdk.ui.theme.LimeGreen


data class ScannerViewColors(
    val detectionFrameColor: Color = DarkBlue,
    val detectionFrameOptimalColor: Color = LimeGreen,
    val surfaceColor: Color = Color(0xFF232333),
    val onSurfaceColor: Color = Color.White,
    val appBarContainerColor: Color = Color(0xFF232333),
    val appBarContentColor: Color = Color.White,
    val glareIndicatorContainerColor: Color = Color(0xD94A4A4A),
    val glareIndicatorContentColor: Color = Color.White,
)

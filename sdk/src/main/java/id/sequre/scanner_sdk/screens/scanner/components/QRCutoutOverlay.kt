package id.sequre.scanner_sdk.screens.scanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import id.sequre.scanner_sdk.ui.theme.DarkBlue


@Composable
fun QrCutoutOverlay(
    cutOutWidth: Float,
    cutOutHeight: Float,
    indicatorColor: Color = DarkBlue,
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = .5F),
) {

    Box(
        modifier = Modifier
    ){
        Canvas(modifier = modifier.fillMaxSize()) {
            // rendered canvas size
            val canvasWidth = size.width
            val canvasHeight = size.height

            with(drawContext.canvas.nativeCanvas) {
                val left = (size.width - cutOutWidth) / 2
                val top = 3 * (size.height - cutOutHeight) / 5

                /// save current canvas state
                val checkPoint = saveLayer(null, null)

                /// transparent black overlay
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(0F, 0F),
                    size = Size(canvasWidth, canvasHeight)
                )

                /// qr clear cutout
                drawRoundRect(
                    topLeft = Offset(
                        x = left,
                        y = top
                    ),
                    size = Size(cutOutWidth, cutOutHeight),
                    color = Color.White.copy(alpha = .5f),
                    blendMode = BlendMode.Clear
                )

                restoreToCount(checkPoint)
            }
        }

        QRCornerIndicator(
            cutOutWidth = cutOutWidth,
            cutOutHeight = cutOutHeight,
            cornerColor = indicatorColor,
            lineLength = 80f,
            lineThickness = 16f,
            indicatorSpacing = 25,
            strokeCap = StrokeCap.Square
        )
    }

}
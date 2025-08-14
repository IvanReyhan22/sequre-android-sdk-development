package id.sequre.scanner_sdk.screens.scanner.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import id.sequre.scanner_sdk.ui.theme.DarkBlue


@Composable
fun QRCornerIndicator(
    cutOutWidth: Float,
    cutOutHeight: Float,
    lineLength: Float = 60f,
    lineThickness: Float = 10f,
    strokeCap: StrokeCap = StrokeCap.Round,
    cornerColor: Color = DarkBlue,
    indicatorSpacing: Int = 20
) {

    Canvas(modifier = Modifier.fillMaxSize()) {
        // rendered canvas size
        val canvasWidth = size.width
        val canvasHeight = size.height
        // detection optimum change the indicator color to green

        with(drawContext.canvas.nativeCanvas) {
            // save current canvas state
            val checkPoint = saveLayer(null, null)

            // inner box detection coordinate
            val left = (canvasWidth - cutOutWidth) / 2
            val top = 3 * (canvasHeight - cutOutHeight) / 5
            val right = left + cutOutWidth
            val bottom = top + cutOutHeight

            // top-left corner
            drawLine(
                color = cornerColor,
                start = Offset(left - indicatorSpacing, top - indicatorSpacing),
                end = Offset((left - indicatorSpacing) + lineLength, (top - indicatorSpacing)),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )
            drawLine(
                color = cornerColor,
                start = Offset((left - indicatorSpacing), (top - indicatorSpacing)),
                end = Offset((left - indicatorSpacing), (top + lineLength - indicatorSpacing)),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )

            // top-right corner
            drawLine(
                color = cornerColor,
                start = Offset((right + indicatorSpacing), (top - indicatorSpacing)),
                end = Offset((right + indicatorSpacing) - lineLength, (top - indicatorSpacing)),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )
            drawLine(
                color = cornerColor,
                start = Offset((right + indicatorSpacing), (top - indicatorSpacing)),
                end = Offset((right + indicatorSpacing), (top - indicatorSpacing) + lineLength),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )

            // bottom-left corner
            drawLine(
                color = cornerColor,
                start = Offset((left - indicatorSpacing), (bottom + indicatorSpacing)),
                end = Offset(
                    (left - indicatorSpacing),
                    (bottom + indicatorSpacing) - lineLength
                ),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )
            drawLine(
                color = cornerColor,
                start = Offset((left - indicatorSpacing), (bottom + indicatorSpacing)),
                end = Offset(
                    (left - indicatorSpacing) + lineLength,
                    (bottom + indicatorSpacing)
                ),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )

            // bottom-right corner
            drawLine(
                color = cornerColor,
                start = Offset((right + indicatorSpacing), (bottom + indicatorSpacing)),
                end = Offset(
                    (right + indicatorSpacing),
                    (bottom + indicatorSpacing) - lineLength
                ),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )
            drawLine(
                color = cornerColor,
                start = Offset((right + indicatorSpacing), (bottom + indicatorSpacing)),
                end = Offset(
                    (right + indicatorSpacing) - lineLength,
                    (bottom + indicatorSpacing)
                ),
                strokeWidth = lineThickness,
                cap = strokeCap,
            )
            restoreToCount(checkPoint)
        }

    }
}
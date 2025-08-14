package id.sequre.scanner_sdk.screens.scanner.components

import android.graphics.RectF
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.sequre.scanner_sdk.ui.theme.WarmRed
import org.tensorflow.lite.task.vision.detector.Detection

@Composable
fun ObjectIndicator(
    detectedObjectSize: Size = Size(0, 0),
    containerWidth: Float = 0f,
    containerHeight: Float = 0f,
    detectionResults: List<Detection> = listOf<Detection>(),
    isFrameSquare: Boolean,
    indicatorColor: Color = WarmRed,
    ) {

    if (detectionResults.isNotEmpty()) {
        val boundingBox = detectionResults.first().boundingBox ?: RectF()

        if (boundingBox != RectF()) {
            /// calculate detected obj size relative to screen
            val resultWidth = boundingBox.width() + (boundingBox.width() / 4)
//            val resultHeight = if (isFrameSquare) resultWidth else boundingBox.height() + (boundingBox.height() / 10)
            val resultHeight = if (isFrameSquare) resultWidth else boundingBox.height()
            val objWidth = (resultWidth / detectedObjectSize.width) * containerWidth
            val objHeight = if (isFrameSquare) objWidth else (resultHeight / detectedObjectSize.height) * containerHeight

            Canvas(modifier = Modifier.fillMaxSize()) {
                val topOffsetSquare = if (isFrameSquare) (objWidth / 4) else 0f
                /// calculate detected obj position relative to canvas container
                val objLeftOffset =
                    (((boundingBox.left / detectedObjectSize.width) * containerWidth) - 10).dp.toPx()
                val objTopOffset =
                    ((boundingBox.top / detectedObjectSize.height) * containerHeight - topOffsetSquare).dp.toPx()

                val color = indicatorColor
                val strokeWidth = 16f
                val lineLength = 80f
                // Top-left corner
                drawLine( // horizontal line
                    color = color,
                    start = Offset(objLeftOffset - 8, objTopOffset),
                    end = Offset(objLeftOffset + lineLength, objTopOffset),
                    strokeWidth = strokeWidth
                )
                drawLine( // vertical line
                    color = color,
                    start = Offset(objLeftOffset, objTopOffset - 8),
                    end = Offset(objLeftOffset, objTopOffset + lineLength),
                    strokeWidth = strokeWidth
                )

//                // Top-right corner
                drawLine( // horizontal line
                    color = color,
                    start = Offset(objLeftOffset + objWidth.dp.toPx() - lineLength, objTopOffset),
                    end = Offset(objLeftOffset + objWidth.dp.toPx() + 8, objTopOffset),
                    strokeWidth = strokeWidth
                )
                drawLine( // vertical line
                    color = color,
                    start = Offset(objLeftOffset + objWidth.dp.toPx(), objTopOffset),
                    end = Offset(objLeftOffset + objWidth.dp.toPx(), objTopOffset + lineLength + 8),
                    strokeWidth = strokeWidth
                )
//
//                // Bottom-left corner
                drawLine( // vertical line
                    color = color,
                    start = Offset(objLeftOffset, objTopOffset + objHeight.dp.toPx() - lineLength),
                    end = Offset(objLeftOffset, objTopOffset + objHeight.dp.toPx() + 8),
                    strokeWidth = strokeWidth
                )
                drawLine( // horizontal line
                    color = color,
                    start = Offset(objLeftOffset, objTopOffset + objHeight.dp.toPx()),
                    end = Offset(
                        objLeftOffset + lineLength + 8,
                        objTopOffset + objHeight.dp.toPx()
                    ),
                    strokeWidth = strokeWidth
                )
//
//                // Bottom-right corner
                drawLine( // horizontal line
                    color = color,
                    start = Offset(
                        objLeftOffset + objWidth.dp.toPx() - lineLength,
                        objTopOffset + objHeight.dp.toPx()
                    ),
                    end = Offset(
                        objLeftOffset + objWidth.dp.toPx() + 8,
                        objTopOffset + objHeight.dp.toPx()
                    ),
                    strokeWidth = strokeWidth
                )
                drawLine( // vertical line
                    color = color,
                    start = Offset(
                        objLeftOffset + objWidth.dp.toPx(),
                        objTopOffset + objHeight.dp.toPx() - lineLength
                    ),
                    end = Offset(
                        objLeftOffset + objWidth.dp.toPx(),
                        objTopOffset + objHeight.dp.toPx() - 8
                    ),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}
package id.sequre.scanner_sdk.common.utils.helper

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.common.utils.scanner.ImageOperation
import id.sequre.scanner_sdk.screens.scanner.ScannerViewModel.Companion.TAG
import id.sequre.scanner_sdk.screens.scanner.camera.CameraController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.detector.Detection

object ObjectDetectionHelper {

    suspend fun calculateObjectSizeRequirement(
        cameraController: CameraController?,
        objectSize: Float,
        imageBitmap: Bitmap,
        result: Detection
    ): ObjectProximityStatus {
        if (cameraController == null) return ObjectProximityStatus.UNDETECTED
        // set minimum and maximum size for object detection to fixed values
        // these values are based on the object size percentage relative to the image size
        // before, these values determined by ObjectProximityStatus Too_Far and Too_Close
        // now because the zoom limit is set to 4x, the values are fixed to 0.32 and 0.45
        // which is the different result between 2x, 3x, and 4x zoom level should be the same
        val minSize = 0.31
        val maxSize = 0.45

        Log.w(TAG, "Object Size: $objectSize")

        return when {
            objectSize <= minSize -> ObjectProximityStatus.TOO_FAR
            objectSize >= maxSize -> ObjectProximityStatus.TOO_CLOSE
            objectSize in minSize..maxSize -> {
                // crop image to reduce computational time
                val croppedImage = ImageOperation.cropBitmapImage(imageBitmap, result)

                // detect glare in background thread
                val glare = withContext(Dispatchers.IO) {
                    ImageOperation.detectGlareWithBitmap(croppedImage)
                }

                // detect if image is too dark
                val isTooDark = withContext(Dispatchers.IO) {
                    ImageOperation.detectBitmapTooDark(croppedImage)
                }

                if (isTooDark) { // if image is too dark, turn on flash light
                    Log.w(TAG, "Image is too dark")
                    cameraController.setFlashLight(true)
                }

                if (glare.second) {
                    ObjectProximityStatus.GLARED
                } else {
                    ObjectProximityStatus.OPTIMAL
                }
            }

            else -> ObjectProximityStatus.UNDETECTED
        }
    }

    fun isBoundingBoxInsideCutout(
        boundingBox: RectF,
        imageWidth: Int,
        imageHeight: Int,
        containerWidth: Float,
        containerHeight: Float,
        cutoutSize: androidx.compose.ui.geometry.Size,
    ): Boolean {
        val paddingNegativePercentage = 0.90f
        val paddingPositivePercentage = 1.10f
        // Cutout screen position (centered, with top offset)
        val cutoutLeft = (containerWidth - cutoutSize.width) / 2f
        val cutoutTop = 3f * (containerHeight - cutoutSize.height) / 5f
        val cutoutRight = cutoutLeft + cutoutSize.width
        val cutoutBottom = cutoutTop + cutoutSize.height

        // Normalized bounding box (percent of image size)
        val leftPercent = boundingBox.left / imageWidth.toFloat()
        val topPercent = boundingBox.top / imageHeight.toFloat()
        val rightPercent = boundingBox.right / imageWidth.toFloat()
        val bottomPercent = boundingBox.bottom / imageHeight.toFloat()

        // Map bounding box to screen coordinates (assume camera feed fills container)
        val boxLeft = leftPercent * containerWidth
        val boxTop = topPercent * containerHeight
        val boxRight = rightPercent * containerWidth
        val boxBottom = bottomPercent * containerHeight

        val cutoutLeftPadded = cutoutLeft * paddingNegativePercentage
        val cutoutTopPadded = cutoutTop * paddingNegativePercentage
        val cutoutRightPadded = cutoutRight * paddingPositivePercentage
        val cutoutBottomPadded = cutoutBottom * paddingPositivePercentage

        return boxLeft >= cutoutLeftPadded &&
                boxTop >= cutoutTopPadded &&
                boxRight <= cutoutRightPadded &&
                boxBottom <= cutoutBottomPadded
    }
}
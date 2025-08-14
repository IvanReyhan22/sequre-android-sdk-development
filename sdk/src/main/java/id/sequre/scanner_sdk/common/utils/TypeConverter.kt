package id.sequre.scanner_sdk.common.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.core.graphics.createBitmap

object TypeConverter {

    /**
     * Converts an ImageProxy to a rotated bitmap.
     *
     * This function takes an ImageProxy object, extracts its pixel data, creates a bitmap,
     * and applies any necessary rotation based on the rotation information provided in the ImageProxy.
     *
     * @param imageProxy The ImageProxy to be converted into a bitmap.
     * @return A bitmap representation of the ImageProxy with the appropriate rotation applied.
     */
    fun imageProxyToRotatedBitmap(imageProxy: ImageProxy): Bitmap {
        // Create a bitmap with the ARGB_8888 configuration for high quality
        val bitmapBuffer = createBitmap(imageProxy.width, imageProxy.height)

        // Copy pixel data from the ImageProxy's buffer to the bitmap
        bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)

        // Prepare a matrix for rotation
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
        }

        // Rotate the bitmap to match the preview's orientation
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        return rotatedBitmap
    }

}
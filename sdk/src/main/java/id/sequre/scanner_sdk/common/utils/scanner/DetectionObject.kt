package id.sequre.scanner_sdk.common.utils.scanner

import android.graphics.Bitmap
import org.tensorflow.lite.task.vision.detector.Detection

class DetectionObject(
    val success: Boolean = false,
    val message: String? = null,
    val results: MutableList<Detection>? = null,
    val bitmap: Bitmap? = null,
    val inferenceTime: Long? = null,
    val imageHeight: Int? = null,
    val imageWidth: Int? = null
)
package id.sequre.scanner_sdk.common.utils.scanner

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import id.sequre.scanner_sdk.R
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector


/**
 * ObjectDetector is responsible for detecting objects in images using a TensorFlow Lite model.
 *
 * The class uses a pre-trained model (stored in the assets folder) to perform object detection on input images.
 * It offers configuration options such as detection threshold, number of threads for inference, and maximum detection results.
 * The detection results are delivered through a listener callback.
 *
 * @param threshold The minimum confidence score threshold for detected objects. Defaults to 0.8.
 * @param numThreads The number of threads to use for inference. Defaults to 2.
 * @param maxResults The maximum number of detection results to return. Defaults to 1.
 * @param context The context used to load the model from the assets folder.
 * @param objectDetectorListener A listener to handle detection results or errors.
 */
class ObjectDetector(
    var threshold: Float = 0.15f,
    var numThreads: Int = 2,
    var maxResults: Int = 1,
    val context: Context,
    val objectDetectorListener: DetectorListener?,
    val isFrameSquare: Boolean = false
) {

    companion object {
        const val TAG = "ObjectDetector"
    }

    private var objectDetector: ObjectDetector? = null

    init {
        setupObjectDetector()
    }

    /**
     * Initializes the TensorFlow Lite Object Detector using the specified model options.
     *
     * This method sets up the object detector by loading the model from the assets folder and applying
     * the options (e.g., confidence threshold, number of threads, and maximum results).
     * If an error occurs during setup, it triggers the onError callback in the DetectorListener.
     */
    private fun setupObjectDetector() {
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(maxResults)
            .setScoreThreshold(threshold)
            .build()

        val modelName = "sequre-combine-v2.tflite"
        try {
            objectDetector =
                ObjectDetector.createFromFileAndOptions(context, modelName, options)

        } catch (e: IllegalStateException) {
            objectDetectorListener?.onError(
                "Object detector failed to initialize. See error logs for details"
            )
            Log.e(TAG, "Error detection: " + e.message)
        }
    }

    /**
     * Performs object detection on bitmap image using the initialized TensorFlow Lite model
     *
     * This method is used for real-time, continuous detection this case camera preview frames.
     * It processes the bitmap into a TensorImage, performs inference using the loaded model, and
     * reports results via the [DetectorListener]. Detection results include bounding boxes, class labels,
     * confidence scores, and inference metadata.
     *
     * If the object detector is not yet initialized, it will be set up automatically before proceeding.
     *
     * @param image The [Bitmap] image to be processed and analyzed for object detection.
     */
    fun detect(image: Bitmap) {
        try {
            if (objectDetector == null) {
                setupObjectDetector()
                return
            }

            // time to detect
            var inferenceTime = SystemClock.uptimeMillis()
            val imageProcessor =
                ImageProcessor.Builder()
                    .build()
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

            val results = objectDetector?.detect(tensorImage)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            objectDetectorListener?.onResults(
                results,
                image,
                inferenceTime,
//                if (isFrameSquare) tensorImage.width else tensorImage.height,
                tensorImage.height,
                tensorImage.width
            )
        } catch (e: Exception) {
            Log.e(TAG, "${e.message} :: ${e.stackTraceToString()}")
            objectDetectorListener?.onError(
                e.message ?: "Detection failed. See logs for details"
            )
        }

    }

    /**
     * Performs synchronous object detection on a bitmap and returns the result directly.
     *
     * This method is intended for high-confidence, one-time detection tasks, such as validating
     * detection results on a captured image. It processes the bitmap into a TensorImage, performs
     * inference using the loaded model, and returns a [DetectionObject] containing detection results,
     * image metadata, and success status.
     *
     * If the object detector is not initialized, it attempts to set it up. If initialization fails or
     * an error occurs during detection, a [DetectionObject] with `success = false` is returned.
     *
     * @param image The [Bitmap] image to analyze.
     * @return A [DetectionObject] containing detection results, image metadata, and inference status.
     */
    fun instantDetect(image: Bitmap): DetectionObject {
        return try {
            if (objectDetector == null) {
                setupObjectDetector()
                return DetectionObject(success = false)
            }

            // time to detect
            var inferenceTime = SystemClock.uptimeMillis()
            val imageProcessor =
                ImageProcessor.Builder()
                    .build()
            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

            val results = objectDetector?.detect(tensorImage)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime

            return DetectionObject(
                success = true,
                results = results,
                bitmap = image,
                inferenceTime = inferenceTime,
                imageHeight = tensorImage.height,
                imageWidth = tensorImage.width
            )

        } catch (e: Exception) {
            Log.e(TAG, "${e.message} :: ${e.stackTraceToString()}")
            DetectionObject(
                success = false,
                message = e.message
                    ?: context.getString(R.string.detection_failed_due_to_unknown_error)
            )
        }
    }

    /**
     * A listener interface for handling object detection results and errors.
     */
    interface DetectorListener {
        /**
         * Called when an error occurs during object detection setup or inference.
         *
         * @param error The error message describing the issue.
         */
        fun onError(error: String)

        /**
         * Called when object detection results are available.
         *
         * @param results The list of detected objects, or null if no objects were detected.
         * @param inferenceTime The time taken for inference in milliseconds.
         * @param imageHeight The height of the processed image.
         * @param imageWidth The width of the processed image.
         */
        fun onResults(
            results: MutableList<Detection>?,
            bitmap: Bitmap,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}

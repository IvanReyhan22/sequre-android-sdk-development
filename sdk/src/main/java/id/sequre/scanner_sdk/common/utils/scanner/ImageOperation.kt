package id.sequre.scanner_sdk.common.utils.scanner

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.exifinterface.media.ExifInterface
import id.sequre.scanner_sdk.common.utils.connectedComponentsWithStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.Canvas as CVS

object ImageOperation {

    const val TAG = "ImageOperation"

    fun fileToBitmap(file: File): Bitmap {
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun bitmapToFile(
        bitmap: Bitmap,
        context: Context,
        fileName: String = "sharpened_image.jpg"
    ): File {
        // Create a file in the cache directory
        val file = File(context.cacheDir, fileName)
        file.createNewFile()

        // Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos) // Use PNG if you want lossless
        val bitmapData = bos.toByteArray()

        // Write byte array to file
        FileOutputStream(file).use { fos ->
            fos.write(bitmapData)
            fos.flush()
        }

        return file
    }

    fun sharpenBitmap(src: Bitmap): Bitmap? {
        val width = src.width
        val height = src.height
        if (src.config == null) return null
        val result = createBitmap(width, height, src.config!!)

        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 5, -1),
            intArrayOf(0, -1, 0)
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = src.getPixel(x + kx, y + ky)
                        val weight = kernel[ky + 1][kx + 1]
                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }

                // Clamp values to [0, 255]
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                result[x, y] = Color.rgb(r, g, b)
            }
        }

        return result
    }

    /**
     * Save image to gallery
     *
     * @param context context used to determined memory reference.
     * @param bitmap image bitmap to save
     * @param fileName saved file name
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "image_${System.currentTimeMillis()}"
    ) {
        // Get the current timestamp in milliseconds
        val timestamp = System.currentTimeMillis()
        val mimeType = "image/jpeg"
        val imageName = "${fileName}.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above (Scoped Storage)
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/SequrePro"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Mark as finished
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
        } else {
            // For Android 7–9 (Legacy Storage)
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .toString() + "/SequrePro"
            val file = File(imagesDir)
            if (!file.exists()) file.mkdirs()

            val imageFile = File(file, imageName)
            try {
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }

                // Notify media scanner
                val uri = Uri.fromFile(imageFile)
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Crops a bitmap image based on the detection's bounding box, with optional padding.
     *
     * @param bitmap The original bitmap to crop.
     * @param detection The detection result containing the bounding box for cropping.
     * @return A new cropped bitmap.
     */
    suspend fun cropBitmapImage(
        bitmap: Bitmap,
        detection: Detection,
    ): Bitmap = withContext(Dispatchers.IO) {
        val boundingResult = detection.boundingBox
        val padding = 0
        val cropRegion = RectF(
            (boundingResult.left - padding).coerceAtLeast(0f),
            (boundingResult.top - padding).coerceAtLeast(0f),
            (boundingResult.right + padding).coerceAtMost(bitmap.width.toFloat()),
            (boundingResult.bottom + padding).coerceAtMost(bitmap.height.toFloat()),
        )
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            cropRegion.left.toInt(),
            cropRegion.top.toInt(),
            cropRegion.width().toInt(),
            cropRegion.height().toInt()
        )
        croppedBitmap
    }

    suspend fun adjustBitmapImageToFile(
        context: Context,
        bitmap: Bitmap,
        detection: Detection,
        fileName: String = "cropped_image"
    ): File = withContext(Dispatchers.IO) {
        val boundingResult = detection.boundingBox

        // Calculate 20% padding based on image dimensions
        val horizontalPadding = bitmap.width * 0.05f
        val verticalPadding = bitmap.height * 0.05f

        /// crop region with padding applied
        val cropRegion = RectF(
            (boundingResult.left - horizontalPadding).coerceAtLeast(0f),
            (boundingResult.top - verticalPadding).coerceAtLeast(0f),
            (boundingResult.right + horizontalPadding).coerceAtMost(bitmap.width.toFloat()),
            (boundingResult.bottom + verticalPadding).coerceAtMost(bitmap.height.toFloat())
        )

        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            cropRegion.left.toInt(),
            cropRegion.top.toInt(),
            cropRegion.width().toInt(),
            cropRegion.height().toInt()
        )

        /// up scale image based on original bitmap image size
        val scaleFactor = bitmap.width.toFloat() / croppedBitmap.width
        val targetHeight = (croppedBitmap.height * scaleFactor).toInt()

        val upScaled = croppedBitmap.scale(bitmap.width, targetHeight)

        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { out ->
            upScaled.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        outputFile
    }

    /**
     * compress image until the size is under the maxSizeInMB
     * if the size after compression loop is still above maxSizeInMB, return null
     *
     * @param imageFile, ImageBitmap to compress
     * @param maxSizeInMB, maximum size in MB to compress
     * @param minQuality, minimum image quality, if quality lower than this, return null
     * @param sizeThresholdInMB, if image size larger than this automatically return null
     */
    suspend fun compressImageUntil(
        imageFile: File,
        fileName: String,
        outputDir: File,
        maxSizeInMB: Double = 2.0,
        minQuality: Int = 75,
        sizeThresholdInMB: Double = 10.0
    ): File = withContext(Dispatchers.IO) {
        Log.d(TAG, "Compressing Image")

        val maxFileSizeInBytes = (maxSizeInMB * 1024 * 1024).toLong()
        val sizeThresholdInBytes = (sizeThresholdInMB * 1024 * 1024).toLong()

        // Check if the image file is already within the size limit
        if (imageFile.length() < maxFileSizeInBytes) return@withContext imageFile

        val image: Bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        var quality = if (image.byteCount > sizeThresholdInBytes) 85 else 92

        var compressedImageBytes: ByteArray

        do {
            val outputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedImageBytes = outputStream.toByteArray()

            val currentSize = compressedImageBytes.size
            if (currentSize <= maxFileSizeInBytes) break

            quality = when {
                currentSize > 8 * maxFileSizeInBytes -> quality - 30
                currentSize > 4 * maxFileSizeInBytes -> quality - 20
                currentSize > 2 * maxFileSizeInBytes -> quality - 15
                currentSize > 1.5 * maxFileSizeInBytes -> quality - 10
                else -> quality - 5
            }.coerceIn(minQuality, 100)

        } while (quality >= minQuality)

        val compressedFile = File(outputDir, fileName)
        FileOutputStream(compressedFile).use { fileOutputStream ->
            fileOutputStream.write(compressedImageBytes)
        }
        Log.d("ImageOperation", "Finished Compressing Image")
        return@withContext compressedFile
    }

    /**
     * Corrects the orientation of an image based on its EXIF data.
     *
     * @param imageFile The image file whose orientation needs correction.
     * @return A bitmap with corrected orientation.
     */
    fun correctImageOrientation(imageFile: File): Bitmap {
        val exif = ExifInterface(imageFile.absolutePath)
        val rotation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationInDegrees = when (rotation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        return if (rotationInDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationInDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /**
     * Gets the size of a file in MB.
     *
     * @param file The file whose size is to be determined.
     * @return The size of the file in MB.
     */
    fun getFileSizeInMB(file: File): Double {
        val fileSizeInKB = (file.length()) / 1024.0
        val fileSizeInMB = fileSizeInKB / 1024.0
        return fileSizeInMB
    }

    fun erode(binaryBitmap: Bitmap, iterations: Int = 2): Bitmap {
        val width = binaryBitmap.width
        val height = binaryBitmap.height
        val pixels = IntArray(width * height)
        binaryBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Create a new bitmap to store the result of the erosion
        val erodedBitmap = createBitmap(width, height)
        val erodedPixels = IntArray(width * height)

        for (i in 0 until iterations) {
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var minVal = Int.MAX_VALUE
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val neighbor = pixels[(y + dy) * width + (x + dx)]
                            minVal = minOf(minVal, Color.red(neighbor))
                        }
                    }
                    erodedPixels[y * width + x] = Color.rgb(minVal, minVal, minVal)
                }
            }
            System.arraycopy(erodedPixels, 0, pixels, 0, width * height)
        }

        erodedBitmap.setPixels(erodedPixels, 0, width, 0, 0, width, height)
        return erodedBitmap
    }

    fun dilate(binaryBitmap: Bitmap, iterations: Int = 2): Bitmap {
        val width = binaryBitmap.width
        val height = binaryBitmap.height
        val pixels = IntArray(width * height)
        binaryBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Create a new bitmap to store the result of the dilation
        val dilatedBitmap = createBitmap(width, height)
        val dilatedPixels = IntArray(width * height)

        for (i in 0 until iterations) {
            // Perform dilation by scanning through each pixel
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    var maxVal = Int.MIN_VALUE
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            val neighbor = pixels[(y + dy) * width + (x + dx)]
                            maxVal = maxOf(maxVal, Color.red(neighbor))
                        }
                    }
                    dilatedPixels[y * width + x] = Color.rgb(maxVal, maxVal, maxVal)
                }
            }
            System.arraycopy(dilatedPixels, 0, pixels, 0, width * height)
        }

        dilatedBitmap.setPixels(dilatedPixels, 0, width, 0, 0, width, height)
        return dilatedBitmap
    }

    /**
     * Detects glare in a bitmap by analyzing pixel brightness and area size.
     * If glare is detected, it highlights the glare area on the image.
     *
     * @param bitmap The bitmap image to analyze for glare.
     * @param brightnessThreshold The threshold for considering a pixel bright enough to be part of glare.
     * @param minGlareArea The minimum area (in pixels) for a region to be considered glare.
     * @return A pair consisting of the modified bitmap with glare highlighted and a boolean indicating whether glare was detected.
     */
    fun detectGlareWithBitmap(
        bitmap: Bitmap,
        brightnessThreshold: Int = 254,
        minGlareArea: Int = 300
    ): Pair<Bitmap, Boolean> {
        var isGlare = false
        val grayscaleBitmap = if (bitmap.config != Bitmap.Config.ALPHA_8) {
            toGrayscaleUsingColorMatrix(bitmap)
        } else {
            bitmap
        }

        val width = grayscaleBitmap.width
        val height = grayscaleBitmap.height
        val pixels = IntArray(width * height)

        grayscaleBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val thresholdedPixels = IntArray(width * height) { i ->
            val gray = pixels[i]
            if (Color.red(gray) >= brightnessThreshold) Color.WHITE else Color.BLACK
        }
        val binaryBitmap = createBitmap(width, height)
        binaryBitmap.setPixels(thresholdedPixels, 0, width, 0, 0, width, height)

        val erodedImage = erode(binaryBitmap)
        val dilatedImage = dilate(erodedImage)

        val (_, components) = connectedComponentsWithStats(dilatedImage)
        val resultBitmap = dilatedImage.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = CVS(resultBitmap)
        val paint = android.graphics.Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = android.graphics.Paint.Style.STROKE
        }
        val textPaint = android.graphics.Paint().apply {
            color = Color.GREEN
            textSize = 40f
            style = android.graphics.Paint.Style.FILL
        }

        for (component in components) {
            val box = component.boundingBox
            val aspectRatio =
                (box.maxX.toFloat() - box.minX.toFloat()) / (box.maxY.toFloat() - box.minY.toFloat())
            if (aspectRatio > 0.6f && aspectRatio < 2.25f && component.area > minGlareArea) {
                var whitePixelCount = 0
                val totalPixelCount = (box.maxX - box.minX) * (box.maxY - box.minY)

                for (y in box.minY until box.maxY) {
                    for (x in box.minX until box.maxX) {
                        val pixelColor = pixels[y * width + x]
                        val red = Color.red(pixelColor)
                        val green = Color.green(pixelColor)
                        val blue = Color.blue(pixelColor)
                        if (red > 200 && green > 200 && blue > 200) {
                            whitePixelCount++
                        }
                    }
                }
                val whitePixelProportion = whitePixelCount.toFloat() / totalPixelCount.toFloat()
                val dominanceThreshold = 0.85f

                if (whitePixelProportion > dominanceThreshold) {
                    isGlare = true

                    Log.e(
                        "ImageOperation",
                        "Aspect ratio: ${component.label} $aspectRatio ${component.area} :: $whitePixelProportion $dominanceThreshold"
                    )
                    canvas.drawRect(
                        box.minX.toFloat(), box.minY.toFloat(),
                        box.maxX.toFloat(), box.maxY.toFloat(),
                        paint
                    )
                    val componentNumber = component.label.toString()
                    val xPosition = box.minX.toFloat() + 10
                    val yPosition = box.minY.toFloat() + 40
                    canvas.drawText(componentNumber, xPosition, yPosition, textPaint)
                }
            }
        }
        return Pair(resultBitmap, isGlare)
    }

    /// convert bitmap image to grayscale
    fun toGrayscaleUsingColorMatrix(originalBitmap: Bitmap): Bitmap {
        val grayscaleBitmap = createBitmap(originalBitmap.width, originalBitmap.height)
        val canvas = android.graphics.Canvas(grayscaleBitmap)

        // create color matrix
        val paint = android.graphics.Paint().apply {
            val colorMatrix = ColorMatrix().apply {
                setSaturation(0f)
            }
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

        return grayscaleBitmap
    }

    /**
     * Detects if a bitmap is too dark by analyzing its average brightness.
     * This method calculates the average brightness of the bitmap and compares it to a specified darkness threshold.
     * 110 threshold mean image is detected but to dark to process by blur detector.
     *
     * @param bitmap The bitmap image to analyze.
     * @param darknessThreshold The threshold below which the image is considered too dark (0-255).
     * @return A boolean indicating whether the bitmap is too dark.
     */
    fun detectBitmapTooDark(
        bitmap: Bitmap, // Bitmap to analyze
        darknessThreshold: Int = 110 // Threshold for darkness detection (0-255)
    ): Boolean {
        val width = bitmap.width // Width of the bitmap
        val height = bitmap.height // Height of the bitmap
        val pixels = IntArray(width * height) // Array to hold pixel colors
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height) // Get pixel colors from the bitmap

        var totalBrightness = 0L // Variable to accumulate brightness values
        for (pixel in pixels) { // Iterate through each pixel
            val r = Color.red(pixel) // Extract red component
            val g = Color.green(pixel) // Extract green component
            val b = Color.blue(pixel) // Extract blue component

            // Using a common formula for perceived brightness
            totalBrightness += (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        // Calculate average brightness
        val averageBrightness = totalBrightness / (width * height)

        // Log the average brightness for debugging
        return averageBrightness < darknessThreshold
    }
}
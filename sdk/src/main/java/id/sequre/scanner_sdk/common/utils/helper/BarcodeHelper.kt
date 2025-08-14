package id.sequre.scanner_sdk.common.utils.helper

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.net.URI

class BarcodeHelper {
    companion object {
        private const val TAG = "BarcodeHelper"
    }

    private val multiFormatReader = MultiFormatReader()

    fun decode(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        multiFormatReader.setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
        try {
            val result = multiFormatReader.decode(binaryBitmap)
            return result.text
        } catch (_: Exception) {
            return null
        }
    }

    fun isValidBaseUrl(url: String): Boolean {
        /// allowed qr domain
        val allowedDomains = setOf("qtru.st", "ahm.to", "qtr.pw")

        return try {
            /// retrieve uri format
            val uri = URI(url)
            /// retrieve qr domain and remove www prefix
            val domain = uri.host?.lowercase()?.removePrefix("www.") ?: return false

            /// if domain is allowed, return true
            if (domain in allowedDomains) {
                return true
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
            false
        }
    }

    /// check wheater string is a valid url with domain or not
    fun isValidUrlWithRealDomain(url: String): Boolean {
        return try {
            val uri = URI(url)
            val host = uri.host
            host != null && host.contains(".") && !host.contains("localhost")
        } catch (_: Exception) {
            false
        }
    }

}
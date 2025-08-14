package id.sequre.scanner_sdk.data.repository

import android.util.Log
import id.sequre.scanner_sdk.common.enums.ScanResult
import id.sequre.scanner_sdk.common.state.ResultState
import id.sequre.scanner_sdk.common.utils.helper.BarcodeHelper
import id.sequre.scanner_sdk.common.utils.helper.ErrorParsing
import id.sequre.scanner_sdk.data.remote.response.Classification
import id.sequre.scanner_sdk.data.remote.response.Qrcode
import id.sequre.scanner_sdk.data.remote.response.ScanProductResponse
import id.sequre.scanner_sdk.data.remote.retrofit.ApiConfig
import id.sequre.scanner_sdk.data.remote.retrofit.ApiService
import id.sequre.scanner_sdk.data.remote.retrofit.ApiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

class ScannerRepository(
    private val classificationApiService: ApiService = ApiConfig.getApiService(type = ApiType.CLASSIFICATION),
) {
    companion object {
        const val TAG = "ScannerRepository"
    }

    private val barcodeHelper = BarcodeHelper()

    suspend fun postProductImage(
        qrCode: String,
        imgFile: File
    ): ResultState<Pair<ScanResult, ScanProductResponse?>?> = withContext(Dispatchers.IO) {
        try {
            // do double check on qrCode, ensuring the qrCode is never null, empty, or no text.
            if (qrCode.isEmpty() || qrCode.isBlank() || qrCode == "") {
                // return to withContext from here and response with empty status code
                return@withContext ResultState.Error(
                    statusCode = null,
                    message = "Qr Code is not detected"
                )
            }

            // check if decoded qr value is not a valid domain
            if (!barcodeHelper.isValidUrlWithRealDomain(qrCode)) {
                return@withContext ResultState.Error(
                    statusCode = 505,
                    message = "Qr Code Invalid"
                )
            }

            // check again if qr value is not a valid base url
            if (!barcodeHelper.isValidBaseUrl(qrCode)) {
                // if not valid, return with invalid response
                // skip hitting classification API with invalid response and FAKE result
                val invalidResponse = ScanProductResponse(
                    message = "QR Link is not recognized",
                    qrcode = Qrcode(
                        text = qrCode,
                    ),
                    pid = "0000000000000",
                    classification = Classification(
                        score = "0.0",
                        label = "fake"
                    )
                )
                return@withContext ResultState.Success(
                    Pair(
                        ScanResult.QR_FAKE,
                        invalidResponse
                    )
                )
            }

            val requestImageFile = imgFile.asRequestBody("image/*".toMediaType())
            Log.d(TAG, "Name: ${imgFile.name}")
            Log.d(TAG, "QrCode: $qrCode")

            val qrcodeBody = qrCode.toRequestBody("text/plain".toMediaTypeOrNull())
            val imageMultipart: MultipartBody.Part = MultipartBody.Part.createFormData(
                "imagefile",
                imgFile.name + ".jpg",
                requestImageFile
            )

            val response: Response<ScanProductResponse> = classificationApiService.postProductImage(
                qrcode = qrcodeBody,
                imagefile = imageMultipart
            )

            return@withContext if (response.isSuccessful) {
                /// fetch response body
                val body = response.body()

                if (body != null) {
                    // Log the response body for debugging
                    Log.d(TAG, "Result Body: ${response.body().toString()}")

                    // initialize qTrustResult with QR_NUMBER_UNKNOWN
                    var qTrustResult: ScanResult = ScanResult.QR_NUMBER_UNKNOWN

                    /// check if qr domain is valid
                    qTrustResult = if (barcodeHelper.isValidBaseUrl(qrCode)) {
                        /// return result based on response body if domain is valid
                        responseToResult(response.body())
                    } else {
                        /// return qr fake if domain is not valid
                        ScanResult.QR_FAKE
                    }

                    ResultState.Success(Pair(qTrustResult, response.body()))
                } else {
                    ResultState.Error(
                        statusCode = 500,
                        message = "There is problem with server connection please try again"
                    )
                }

            } else {
                /// handle error != 2xx code
                /// parse error string body to object class
                val (errorMessage, _) = ErrorParsing.responseParseError(
                    response.errorBody(),
                    ScanProductResponse::class.java
                )

                /// return custom error based on response body
                ResultState.Error(
                    statusCode = 500,
                    message = errorMessage
                )
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request Timed Out: ${e.localizedMessage}")
            ResultState.Error(
                statusCode = 408,
                message = "Request timed out. Please try again."
            )
        } catch (e: IOException) {
            Log.e(TAG, "No internet connection: ${e.localizedMessage}")
            ResultState.Error(
                statusCode = 500,
                message = "No internet connection. Please check your network."
            )
        } catch (e: Exception) {
            Log.e(TAG, "${e.localizedMessage} :: ${e.stackTraceToString()}")
            ResultState.Error(
                statusCode = 500,
                message = e.localizedMessage
                    ?: "There is problem with server connection please try again"
            )
        }
    }

    private fun responseToResult(response: ScanProductResponse?): ScanResult {

        response?.run {
            val label = classification?.label ?: ""

            val allStatusesDetected = listOf(
                obj?.status,
//                canvas?.status,
//                qrcode?.status,
//                classification?.status,
            ).all { it == "detected" }

            // check if classification object and label is not null or empty
            val isClassificationValid = classification != null && label.isNotEmpty()

            if (allStatusesDetected) {
                if (isClassificationValid) {
                    return when (label) {
                        "genuine" -> ScanResult.QR_GENUINE
                        "poor" -> ScanResult.QR_POOR_IMAGE
                        "fake" -> ScanResult.QR_FAKE
                        else -> ScanResult.QR_POOR_IMAGE
                    }
                } else {
                    Log.e(TAG, "Classification is not valid: $classification")
                    return ScanResult.QR_POOR_IMAGE
                }
            } else {
                Log.e(TAG, "Obj is not detected: $obj")
                return ScanResult.QR_POOR_IMAGE
            }
        }
        return ScanResult.QR_POOR_IMAGE
    }
}
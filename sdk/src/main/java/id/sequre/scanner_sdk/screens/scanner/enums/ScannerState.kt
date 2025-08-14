package id.sequre.scanner_sdk.screens.scanner.enums

import android.graphics.Bitmap
import id.sequre.scanner_sdk.common.enums.ScanResult
import id.sequre.scanner_sdk.data.remote.response.ScanProductResponse
import okhttp3.internal.http2.ErrorCode

sealed class ScannerState {
    data object Scanning : ScannerState()
    data class Processing(val text: String) : ScannerState()
    data class Error(val exception: Exception, val errorCode: Int? = null): ScannerState()
    data class Success(
        val scanResult: ScanResult,
        val bitmap: Bitmap?,
        val scanProductResponse: ScanProductResponse?,
    ) : ScannerState()
}

package id.sequre.scanner_sdk.data.repository

import android.util.Log
import id.sequre.scanner_sdk.data.remote.request.ValidateSDKRequest
import id.sequre.scanner_sdk.data.remote.response.SDKValidationResponse
import id.sequre.scanner_sdk.data.remote.retrofit.ApiConfig
import id.sequre.scanner_sdk.data.remote.retrofit.ApiService
import id.sequre.scanner_sdk.data.remote.retrofit.ApiType

class SDKRepository(
    private val apiService: ApiService = ApiConfig.getApiService(type = ApiType.BASE)
) {
    companion object {
        const val TAG = "SDKRepository"
    }

    suspend fun validateSDK(request: ValidateSDKRequest): SDKValidationResponse {
        val response = apiService.validateSDK(
            number = request.number,
            bundle = request.bundle,
            sha = request.sha
        )

        if (response.isSuccessful && response.body() != null) {
            Log.d(TAG, "Result Body: ${response.body().toString()}")
            return response.body() ?: SDKValidationResponse(
                status = "false",
                code = 500,
                message = "Error: ${response.code()}"
            )
        } else {
            Log.e(TAG, "Error: response failed ${response.code()}")

            return SDKValidationResponse(
                status = "false",
                code = response.code(),
                message = "Error: ${response.message()}"
            )
        }
    }
}
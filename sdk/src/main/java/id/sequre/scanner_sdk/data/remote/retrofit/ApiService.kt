package id.sequre.scanner_sdk.data.remote.retrofit

import id.sequre.scanner_sdk.data.remote.response.SDKValidationResponse
import id.sequre.scanner_sdk.data.remote.response.ScanProductResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("/image_validation")
    suspend fun postProductImage(
        @Part("texturl") qrcode: RequestBody,
        @Part imagefile: MultipartBody.Part,
    ): Response<ScanProductResponse>

    @FormUrlEncoded
    @POST("sdk/validate")
    suspend fun validateSDK(
        @Field("number") number: Int,
        @Field("bundle") bundle: String,
        @Field("sha") sha: String,
    ): Response<SDKValidationResponse>
}
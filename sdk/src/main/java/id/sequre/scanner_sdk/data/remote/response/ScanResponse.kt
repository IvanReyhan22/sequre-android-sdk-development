package id.sequre.scanner_sdk.data.remote.response

import com.google.gson.annotations.SerializedName


data class ScanProductResponse(
    val message: String? = null,
//    val canvas: Canvas? = null,
//    @SerializedName("email_sent")
//    val emailSent: Boolean? = null,
    var qrcode: Qrcode? = null,
    val pid: String? = null,
    val classification: Classification? = null,
//    val originals: Originals? = null,
    @SerializedName("object")
    val obj: Object? = null,
)

//data class Rect(
//    val top: Int? = null,
//    val left: Int? = null,
//    val width: Int? = null,
//    val height: Int? = null
//)

//data class Dimensions(
//    val width: Int? = null,
//    val height: Int? = null
//)

//data class Originals(
//    val dimensions: Dimensions? = null,
//    @SerializedName("file_path")
//    val filePath: String? = null,
//    @SerializedName("file_size")
//    val fileSize: Int? = null,
//    val format: String? = null,
//)

//data class BoundingBox(
//    @SerializedName("top_left")
//    val topLeft: List<Int?>? = null,
//    @SerializedName("bottom_right")
//    val bottomRight: List<Int?>? = null
//)

data class Qrcode(
//    val status: String? = null,
    val text: String? = null,
//    val type: String? = null,
//    val rect: Rect? = null,
)

//data class Canvas(
//    val status: String? = null,
//    val score: String? = null,
//    @SerializedName("model_used")
//    val modelUsed: String? = null,
//    @SerializedName("bounding_box")
//    val boundingBox: BoundingBox? = null,
//    @SerializedName("file_size")
//    val fileSize: Int? = null,
//    val dimensions: Dimensions? = null,
//)

data class Object(
    val status: String? = null,
//    val score: String? = null,
//    @SerializedName("model_used")
//    val modelUsed: String? = null,
//    @SerializedName("bounding_box")
//    val boundingBox: BoundingBox? = null,
//    @SerializedName("file_size")
//    val fileSize: Int? = null,
//    val dimensions: Dimensions? = null,
)

data class Classification(
//    val status: String? = null,
//    @SerializedName("file_path")
//    val filePath: String? = null,
//    val dimensions: Dimensions? = null,
//    @SerializedName("file_size")
//    val fileSize: String? = null,
//    @SerializedName("label_index")
//    val labelIndex: Int? = null,
    val label: String? = null,
    val score: String? = null,
//    @SerializedName("model_used")
//    val modelUsed: String? = null,
)



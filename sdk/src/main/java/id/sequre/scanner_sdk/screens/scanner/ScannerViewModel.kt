package id.sequre.scanner_sdk.screens.scanner


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.sequre.scanner_sdk.R
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.common.enums.ScanResult
import id.sequre.scanner_sdk.common.state.ResultState
import id.sequre.scanner_sdk.common.utils.helper.BarcodeHelper
import id.sequre.scanner_sdk.common.utils.helper.ObjectDetectionHelper
import id.sequre.scanner_sdk.common.utils.helper.handleException
import id.sequre.scanner_sdk.common.utils.helper.launchSafely
import id.sequre.scanner_sdk.common.utils.scanner.ImageOperation
import id.sequre.scanner_sdk.common.utils.scanner.ObjectDetector
import id.sequre.scanner_sdk.data.remote.response.Qrcode
import id.sequre.scanner_sdk.data.repository.SDKRepository
import id.sequre.scanner_sdk.data.repository.ScannerRepository
import id.sequre.scanner_sdk.screens.scanner.camera.CameraController
import id.sequre.scanner_sdk.screens.scanner.enums.ScannerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import kotlin.time.Duration.Companion.seconds

@SuppressLint("StaticFieldLeak")
class ScannerViewModel(
    private val context: Context,
    private val scannerRepository: ScannerRepository,
    private val sdkRepository: SDKRepository,
) : ViewModel() {

    companion object {
        const val TAG = "ScannerViewModel"
    }

    private var barcodeHelper = BarcodeHelper()

    private val _isFrameSquare: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFrameSquare: StateFlow<Boolean> = _isFrameSquare

    // / flag to indicate if zoom set to auto or manual
    private val _isManualZoom: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isManualZoom: StateFlow<Boolean> = _isManualZoom

    private val objectDetector = ObjectDetector(
        threshold = 0.5f,
        numThreads = 5,
        maxResults = 1,
        context = context,
        objectDetectorListener = objectDetectorListener(),
        isFrameSquare = _isFrameSquare.value
    )

    private val _saveToGalleryFlag = MutableStateFlow(false)
    private val _cutoutSize = MutableStateFlow<androidx.compose.ui.geometry.Size?>(null)
    private val _containerWidth = MutableStateFlow(0f)
    private val _containerHeight = MutableStateFlow(0f)
    private val _qrValue: MutableStateFlow<String> = MutableStateFlow("")

    /// adjust zoom level
    private val _adjustedZoomLevel: MutableStateFlow<Float> = MutableStateFlow(2f)
    val adjustedZoomLevel: StateFlow<Float> get() = _adjustedZoomLevel

    private var cameraController: CameraController? = null

    private val _sentImage: MutableStateFlow<Bitmap?> = MutableStateFlow(null)

    private val _objectProximityStatus: MutableStateFlow<ObjectProximityStatus> =
        MutableStateFlow(ObjectProximityStatus.UNDETECTED)
    val objectProximityStatus: MutableStateFlow<ObjectProximityStatus> get() = _objectProximityStatus

    private val _listDetection: MutableStateFlow<List<Detection>> = MutableStateFlow(emptyList())
    val listDetection: MutableStateFlow<List<Detection>> get() = _listDetection

    private val _detectedObjectSize: MutableStateFlow<Size> = MutableStateFlow(Size(0, 0))
    val detectedObjectSize: MutableStateFlow<Size> get() = _detectedObjectSize

    private val _isSDKValidated: MutableStateFlow<Boolean?> = MutableStateFlow(true)
    val isSDKValidated: StateFlow<Boolean?> get() = _isSDKValidated

    private val _scannerState: MutableStateFlow<ScannerState> =
        MutableStateFlow(ScannerState.Scanning)
    val scannerState: StateFlow<ScannerState> = _scannerState

    private val _shouldShowGlareIndicator: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val _isPaused: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val isCapturing: MutableStateFlow<Boolean> = MutableStateFlow(false)

    // / StateFlow to trigger flash when reach 0.0f
    private val triggerFlash: MutableStateFlow<Float> = MutableStateFlow(3.0f)

    // / Job for countdown timer
    private var countdownJob: Job? = null

    fun setShouldShowGlareIndicator(value: Boolean) {
        _shouldShowGlareIndicator.value = value
    }

    fun setIsPaused(isPaused: Boolean) {
        _isPaused.value = isPaused
    }

    private fun validateSDK() {
        viewModelScope.launch {
            delay(2.seconds)
            _isSDKValidated.value = true
            // TODO: need implementation
//            sdkRepository.validateSDK()
        }
    }

    fun updateZoomLevel(zoomLevel: Float) {
        _adjustedZoomLevel.value = zoomLevel
    }

    fun updateScannerState(newState: ScannerState) {
        _scannerState.value = newState
    }

    fun setCameraController(controller: CameraController) {
        cameraController = controller
    }

    fun setSaveToGalleryFlag(flag: Boolean) {
        _saveToGalleryFlag.value = flag
    }

    /// specific var for delayQR()
    private var qrDetected: Boolean = false
    private var delayQRProcessing: Boolean = false

    /// delay qr status update
    fun delayQR() {
        /// if debounce process already running end function
        if (delayQRProcessing) return

        /// set flag to true to prevent multiple process
        delayQRProcessing = true
        viewModelScope.launch {
            delay(0.2.seconds)
            if (!qrDetected) {
                /// reset state to initial value
                _objectProximityStatus.value = ObjectProximityStatus.UNDETECTED
                _detectedObjectSize.value = Size(0, 0)
                _listDetection.value = emptyList()
                if (_scannerState.value !is ScannerState.Processing) {
                    _qrValue.value = ""
                }
            }
            /// reset flag
            delayQRProcessing = false
        }
    }

    /// detect image qr code
    fun detect(image: Bitmap) {
        viewModelScope.launch {
            handleException(
                onException = {
                    reportExceptionError(it)
                    Log.e(TAG, "detect() exception: ${it.message}")
                }
            ) {
                if (_scannerState.value !is ScannerState.Scanning) return@launch

                /// retrieve its qr value
                val qrValue = barcodeHelper.decode(bitmap = image)
                /// set flag to true if qr value is not null
                /// this means in image/camera feed qr is detected
                qrDetected = !qrValue.isNullOrEmpty()

                /// run object detector if qr value is not null and scan state is scanning
                /// prevent from running image processing on image analysis
                if (qrValue != null) {
                    _isFrameSquare.value = qrValue.contains("QTR.PW", ignoreCase = true)
                    _qrValue.value = qrValue
                    /// result of detection will be sent to objectDetectorListener()
                    objectDetector.detect(image)
                } else {
                    /// debounce qrDetected status update
                    delayQR()
                }
            }
        }
    }

    /// get container canvas reference for object distance calculation
    fun updateCutoutParams(
        cutoutSize: androidx.compose.ui.geometry.Size,
        containerWidth: Float,
        containerHeight: Float
    ) {
        _cutoutSize.value = cutoutSize
        _containerWidth.value = containerWidth
        _containerHeight.value = containerHeight
    }

    /**
     * Function to capture image and process image for classification.
     *
     * - create a definition reference to store image in cache.
     * - capture image to get the best image quality instead of taking from video feed frame
     * - check if image contains qr
     * -
     */
    private fun processImage() {
        _sentImage.value = null

        /// check if qr value is not empty, if empty finish function
        if (_qrValue.value.isEmpty()) {
            Log.w(TAG, "processImage() QR value empty")
            showToastAndReset(R.string.qr_not_detected)
            updateScannerState(ScannerState.Scanning)
            return
        }

        if (!isCapturing.value) { // Proceed only if the image is clear and variance is not zero
            /// prepare file name
            isCapturing.value = true // Set capturing state to true to prevent multiple captures

            val photoFileName = "photo_temp"
            val photoFile = File(context.cacheDir, photoFileName)

            cameraController?.takePicture(
                photoFile,
                onSuccess = {
                    Log.i(TAG, "Image Captured")
                    val uri = it.savedUri?.path
                    if (uri.isNullOrEmpty()) {
                        Log.e(TAG, "Qr value empty $uri")
                        showToastAndReset(R.string.qr_not_detected, photoFile)
                        return@takePicture
                    }

                    /// prepare file base on saved ui
                    val file = File(uri)

                    viewModelScope.launchSafely(
                        onError = { throwable ->
                            Log.e(TAG, "processImage() error : ${throwable.message}")
                            reportExceptionError(Exception(throwable.localizedMessage))
                            return@launchSafely
                        }
                    ) {
                        /// if glare indicator is still shown finish function
                        if (_shouldShowGlareIndicator.value) {
                            Log.w(TAG, "Glare detected")
                            _shouldShowGlareIndicator.value = false
                            resetStateAndContinueScan(photoFile, turnFlashOn = false)
                            return@launchSafely
                        }

                        /// check if qr is detected in image
                        if (_qrValue.value.isBlank() && !qrDetected) {
                            Log.w(TAG, "No QR Detected")
                            showToastAndReset(R.string.qr_not_detected, photoFile)
                            return@launchSafely
                        }

                        /// turn off flash light
                        cameraController?.setFlashLight(false)
                        delay(100) /// allow torch modifying process before destroying / unbind camera

                        /// loading screen starts
                        updateScannerState(ScannerState.Processing(context.getString(R.string.processing_qr_desc)))
                        /// allow for camera binding to de-initializing before processing image to prevent glitches
                        delay(100)

                        /// fix image orientation
                        val bitmapImage: Bitmap = ImageOperation.correctImageOrientation(file)

                        /* Retrieve qr object detection and crop image based on detected location*/
                        val qrObject = objectDetector.instantDetect(bitmapImage)
                        if (!qrObject.success || qrObject.results.isNullOrEmpty()) {
                            showToastAndReset(R.string.qr_not_detected, photoFile)
                            return@launchSafely
                        }

                        /// get first detected object
                        val detection = qrObject.results.first()

                        /// crop image based on detected qr object
                        val croppedImage = ImageOperation.adjustBitmapImageToFile(
                            context,
                            bitmapImage,
                            detection
                        )

                        /* Image Uploading  */
                        Log.e(TAG, "To Upload Image")

                        /// update loading wording progress
                        updateScannerState(ScannerState.Processing(context.getString(R.string.uploading_qr_image)))

                        /// save result image for preview
                        val bitmapSentImage = ImageOperation.fileToBitmap(croppedImage)
                        _sentImage.value = bitmapSentImage

                        Log.e(TAG, "File Size: ${ImageOperation.getFileSizeInMB(croppedImage)}MB")
                        val compressedImage = ImageOperation.compressImageUntil(
                            croppedImage,
                            "compressed_image",
                            context.cacheDir,
                            maxSizeInMB = 1.5
                        )
                        Log.e(
                            TAG,
                            "File Size Compression: ${ImageOperation.getFileSizeInMB(compressedImage)}MB"
                        )

                        /// upload for classification
                        val apiResponse = scannerRepository.postProductImage(
                            qrCode = _qrValue.value,
                            compressedImage
                        )

                        when (apiResponse) {
                            is ResultState.Error -> {

                                when (apiResponse.statusCode) {
                                    null -> {
                                        /// reset value
                                        _qrValue.value = ""
                                        barcodeHelper = BarcodeHelper()
                                        // show toast and do reset scanner state
                                        showToastAndReset(
                                            messageId = R.string.qr_not_detected,
                                            photoFile,
                                            turnFlashOn = true
                                        )
                                    }

                                    505 -> {
                                        /// reset value
                                        _qrValue.value = ""
                                        barcodeHelper = BarcodeHelper()

                                        /// invalid qr, qr code is unknown
                                        showToastAndReset(
                                            messageId = R.string.qr_link_is_not_valid_please_scan_again,
                                            photoFile,
                                            turnFlashOn = true
                                        )
                                    }

                                    else -> {
                                        /// error occurred
                                        reportExceptionError(
                                            Exception(apiResponse.message),
                                            apiResponse.statusCode
                                        )
                                    }
                                }
//                                 /// if status code is empty,meaning the qrcode is empty
//                                if (apiResponse.statusCode == null) {
//                                    // show toast and do reset scanner state
//                                    showToastAndReset(
//                                        messageId = R.string.qr_not_detected,
//                                        photoFile,
//                                        turnFlashOn = true
//                                    )
//                                } else { // else return error from API
//                                    /// error occurred
//                                    reportExceptionError(
//                                        Exception(apiResponse.message),
//                                        apiResponse.statusCode
//                                    )
//                                }
                            }

                            is ResultState.Success -> {

                                /// save cropped image for preview
                                if (_saveToGalleryFlag.value) {
                                    /// save to gallery with label, score and pid as file name

                                    // get score
                                    val score: String =
                                        (apiResponse.data?.second?.classification?.score
                                            ?: 0).toString()
                                    // get label
                                    val label: String =
                                        (apiResponse.data?.second?.classification?.label).toString()
                                    // get pid
                                    val pid: String =
                                        (apiResponse.data?.second?.pid ?: "unknown_pid")
                                    // prepare file name
                                    val fileName = "${label}_${score}_(${pid})"
                                    // save image to gallery
                                    _sentImage.value?.let { bitmap ->
                                        ImageOperation.saveBitmapToGallery(
                                            context = context,
                                            bitmap = bitmap,
                                            fileName = fileName
                                        )
                                    }
                                }

                                updateScannerState(
                                    ScannerState.Success(
                                        apiResponse.data?.first ?: ScanResult.QR_NUMBER_UNKNOWN,
                                        _sentImage.value,
                                        apiResponse.data?.second
                                    )
                                )

                                /// assign detected qrcode to response if result is number unknown
                                if (apiResponse.data?.first == ScanResult.QR_NUMBER_UNKNOWN) {
                                    val qrcode = Qrcode(text = _qrValue.value)
                                    apiResponse.data.second?.qrcode = qrcode
                                }

                                Log.e(
                                    TAG,
                                    "Upload Status ${apiResponse.data?.first} :: ${apiResponse.data?.second}"
                                )
                            }
                        }

                        /// reset capturing flag, enable to take picture again
                        isCapturing.value = false
                        countDownStarted = false

                        /// delete cache
                        compressedImage.delete()
                        croppedImage.delete()
                        photoFile.delete()
                    }
                },
                onError = {
                    Log.e(TAG, "Error taking picture: ${it.message}")
                    reportExceptionError(Exception(it.message))
                    photoFile.delete()

                    isCapturing.value = false /// reset capturing flag, enable to take picture again
                }
            )
        } else { // If failed 6x (equal to 3s delay), activate flash light for better capture
//            if (triggerFlash.value <= 0.0f) {
//                cameraController?.setFlashLight(true)
//                triggerFlash.value = 3.0f // Reset cooldown to 3.0f
//            } else {
//                triggerFlash.value -= 0.5f // Decrease cooldown by 0.5f
//            }
            countDownStarted = false // Reset countdown state
        }
    }

    fun resetState() {
        qrDetected = false
        isCapturing.value = false
        countDownStarted = false // reset countdown state
        _qrValue.value = ""
        _scannerState.value = ScannerState.Scanning
        _sentImage.value = null
        _listDetection.value = emptyList()
        _detectedObjectSize.value = Size(0, 0)
        _objectProximityStatus.value = ObjectProximityStatus.UNDETECTED
    }

    /// delete file cache and continue scanning
    private fun resetStateAndContinueScan(photoFile: File, turnFlashOn: Boolean) {
        /// delete cache file
        photoFile.delete()
        /// reset state to scanning to continue scanning process
        updateScannerState(ScannerState.Scanning)
        /// reset all state to initial
        resetState()

        /// control flash
        if (turnFlashOn) {
            cameraController?.setFlashLight(true)
            return
        }
    }

    private fun showToastAndReset(
        @StringRes messageId: Int,
        file: File? = null,
        turnFlashOn: Boolean = false
    ) {
        isCapturing.value = false // reset capturing state to allow taking picture again
        countDownStarted = false // reset countdown state
        Toast.makeText(context, context.getString(messageId), Toast.LENGTH_SHORT).show()
        file?.let { resetStateAndContinueScan(it, turnFlashOn) }
    }

    private fun reportExceptionError(exception: Exception, errorCode: Int? = null) {
        viewModelScope.launch {
            cameraController?.setFlashLight(false)
            delay(100) /// allow torch modifying process before destroying / unbind camera

            _listDetection.value = emptyList()
            _objectProximityStatus.value = ObjectProximityStatus.UNDETECTED

            updateScannerState(
                ScannerState.Error(
                    errorCode = errorCode,
                    exception = exception,
                )
            )
            Log.e(TAG, "${exception.localizedMessage} -> ${exception.stackTraceToString()}")
        }
    }

    /// Start: This line of code same as before
    /// specific var for startCaptureCountdown()
    private var countDownStarted: Boolean = false

    /// delay 3 seconds before capturing
    private fun startCaptureCountdown() {
        /// skip if currently in processing image
        if (countDownStarted) return

        countDownStarted = true
        countdownJob?.cancel()

        /// set focus to center of camera
        cameraController?.setAutoFocus(
            withDelay = false,
            onFocusComplete = {
                Log.w(TAG, "Steady...")
                countdownJob = viewModelScope.launch {
                    if (_objectProximityStatus.value != ObjectProximityStatus.OPTIMAL) {
                        Log.w(TAG, "Object proximity status not in optimal distance")
                        countDownStarted = false
                        cancel()
                    }
                    delay(1.seconds)
                }
                countdownJob?.invokeOnCompletion {
                    viewModelScope.launch {
                        if (_objectProximityStatus.value == ObjectProximityStatus.OPTIMAL) {
                            Log.w(TAG, "Will Capture")
                            processImage()
                        } else {
                            Log.w(TAG, "Object proximity status not in optimal distance")
                            countDownStarted = false
                        }
                    }
                }
            },
        )
    }
    /// End: This line of code same as before

    // function triggered when detection completed
    private fun objectDetectorListener(): ObjectDetector.DetectorListener {
        val listener = object : ObjectDetector.DetectorListener {
            override fun onError(error: String) {
                reportExceptionError(Exception())
                Log.e(TAG, "objectDetectorListener() error: $error")
            }

            override fun onResults(
                results: MutableList<Detection>?,
                bitmap: Bitmap,
                inferenceTime: Long,
                imageHeight: Int,
                imageWidth: Int
            ) {
                viewModelScope.launch {
                    /// skip detection if image is captured and already in process
                    if (isCapturing.value) return@launch

                    // Early exit: no results
                    if (results.isNullOrEmpty()) {
                        _listDetection.value = emptyList()
                        _objectProximityStatus.value = ObjectProximityStatus.UNDETECTED
                        return@launch
                    }

                    val result = results.first()
                    val boundingBox = result.boundingBox

                    // Defensive checks
                    val cutoutSize = _cutoutSize.value
                    val containerWidth = _containerWidth.value
                    val containerHeight = _containerHeight.value
                    val isFrameSquare = _isFrameSquare.value
                    val isPaused = _isPaused.value
                    val showGlare = _shouldShowGlareIndicator.value

                    if (cutoutSize == null || containerWidth <= 0 || containerHeight <= 0) {
                        _listDetection.value = emptyList()
                        _objectProximityStatus.value = ObjectProximityStatus.UNDETECTED
                        return@launch
                    }

                    // Handle glare
                    if (showGlare) {
                        cameraController?.setAutoExposure(
                            exposureCompensation = -5,
                            onExposureComplete = {
                                Log.w(TAG, "Glare indicator shown, exposure compensation set to 0")
                            }
                        )
                        _objectProximityStatus.value = ObjectProximityStatus.UNDETECTED
                        return@launch
                    }

                    // Update detection state
                    _detectedObjectSize.value =
                        Size(imageWidth, if (isFrameSquare) imageWidth else imageHeight)
                    _listDetection.value = results

                    // Evaluate object size & position
                    val objectWidthPercentage = boundingBox.width() / imageWidth.toFloat()
                    val proximityStatus = ObjectDetectionHelper.calculateObjectSizeRequirement(
                        cameraController = cameraController,
                        objectSize = objectWidthPercentage,
                        imageBitmap = bitmap,
                        result = result,
                    )
                    _objectProximityStatus.value = proximityStatus

                    val isInsideCutout = ObjectDetectionHelper.isBoundingBoxInsideCutout(
                        boundingBox = boundingBox,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        containerWidth = containerWidth,
                        containerHeight = containerHeight,
                        cutoutSize = cutoutSize
                    )

                    // Combine proximity and cutout logic
                    if (_objectProximityStatus.value == ObjectProximityStatus.OPTIMAL) {
                        if (isInsideCutout) {
                            if (!isPaused) {
                                startCaptureCountdown()
                            }
                        } else {
                            _objectProximityStatus.value = ObjectProximityStatus.OUTSIDE
                        }
                    } else {
                        _listDetection.value = emptyList()
                    }
                }
            }
        }
        return listener
    }

    /**
     * Toggles the manual zoom state.
     *
     * This function inverts the current value of the `_isManualZoom` state flow.
     * It also logs the new state of manual zoom.
     */// toggle _isManualZoom state
    fun toggleManualZoom() {
        _isManualZoom.value = !_isManualZoom.value
        Log.d(TAG, "Manual zoom toggled: ${_isManualZoom.value}")
    }

    /**
     * Switch to manual zoom operation on the camera.
     *
     * This function cancels any ongoing zoom process, retrieves the current zoom level,
     * ensures it does not exceed the maximum zoom level of 4x, updates the zoom level,
     * and applies the zoom level to the camera control. If the current zoom level is null,
     * it defaults to 3x.
     */
    fun manualZoomOperation() {
        cameraController?.cancelAllZoomProcess() // cancel any ongoing zoom process
        val currentZoom = cameraController?.currentZoom?.let {
            if (it > 4f) {
                4f // set max zoom level to 4x
            } else {
                it // keep current zoom level if less than 4x
            }
        }
        updateZoomLevel(
            currentZoom ?: 3f
        ) // update zoom level to current zoom or default to 3x if null
        cameraController?.cameraControl?.setZoomRatio(
            currentZoom ?: 3f
        ) // apply zoom level to camera control or set to 3x if null
    }
}
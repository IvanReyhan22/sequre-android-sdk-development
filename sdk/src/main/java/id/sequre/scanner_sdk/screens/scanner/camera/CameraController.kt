package id.sequre.scanner_sdk.screens.scanner.camera

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.util.Log
import android.util.Range
import android.view.Surface
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import id.sequre.scanner_sdk.R
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.common.enums.ZoomDirection
import id.sequre.scanner_sdk.common.utils.TypeConverter
import id.sequre.scanner_sdk.common.utils.helper.PackageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

class CameraController(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val preferredZoomLevel: Float = 3f,
    val onZoomChanged: (Float) -> Unit,
    private val onImageProxyAnalyzed: (Bitmap) -> Unit,
) {
    companion object {
        const val TAG = "CameraController"
    }

    /// preview setup
    var preview: Preview = setupPreview()
    var previewView: PreviewView = PreviewView(context)

    /// image analysis object
    var imageAnalysis: ImageAnalysis
    var imageCapture: ImageCapture

    /// zoom setup
    private var activeZoomLevel: Float = 3f

    /// set initial max supported zoom
    private var maxSupportedZoom: Float = 3f
    var currentZoom: Float = 0F

    var camera: Camera? = null
    var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private val detectionMutex = Mutex()

    private val _isFlashAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFlashAvailable: StateFlow<Boolean> = _isFlashAvailable

    private val _isFlashLightOn: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isFlashLightOn: StateFlow<Boolean> = _isFlashLightOn

    var cameraInitialized: Boolean = false

    /// for zoom job
    var zoomJob: Job? = null

    // set to true if EIS or OIS is supported
    var isEisOrOisSupported: Boolean? = false

    init {
        imageAnalysis = setImageAnalysis()
        imageCapture = setupImageCapture()

        _isFlashAvailable.value = PackageHelper.isSystemHasFlashFeature(context)

        lifecycleOwner.lifecycleScope.launch {
            /// bind camera control & preview config
            camera = setLifecycleBoundCamera()
            camera?.let {
                cameraControl = it.cameraControl
                cameraInfo = it.cameraInfo
            }

            /// retrieve supported max zoom level
            cameraInfo?.zoomState?.observe(lifecycleOwner) { zoomState ->
                maxSupportedZoom = zoomState.maxZoomRatio
            }

            /// set zoom level based on supported zoom
            activeZoomLevel =
                if (maxSupportedZoom > preferredZoomLevel) preferredZoomLevel else maxSupportedZoom
            currentZoom = activeZoomLevel

            /// set zoom level
            onZoomChanged(activeZoomLevel)
            cameraControl?.setZoomRatio(activeZoomLevel)

            /// auto turn on flash on start up
            setFlashLight(true)
            preview.surfaceProvider = previewView.surfaceProvider

            /// set focus after 3 second delay
            setAutoFocus()
            /// enable preview stabilization if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setPreviewStabilization()
            }
            /// enable optical image stabilization
            setOpticalStabilization()

            // set target fps range by default 30, but preferably 60fps if available
            setTargetFpsRange()

            /// update camera state
            cameraInitialized = camera != null
        }
    }

    /// preview config
    @OptIn(ExperimentalCamera2Interop::class)
    fun setupPreview(): Preview {
        val previewBuilder = Preview.Builder()
        val extender = Camera2Interop.Extender(previewBuilder)

        extender
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )

        return previewBuilder.build()
    }

    /// cancel all zoom process
    fun cancelAllZoomProcess() {
        zoomJob?.cancel()
        zoomJob = null
    }

    fun smoothZoomContinuously(
        direction: ZoomDirection, // IN or OUT
        getProximityStatus: () -> ObjectProximityStatus,
        durationPerStep: Long = 20L
    ) {
        /// static max zoom limit
        val staticMaxZoomLimit = 3f
        if (!cameraInitialized) return

        /// get current zoom and min/max zoom capability limit
        val currentZoom =
            if (currentZoom != 0f) currentZoom else cameraInfo?.zoomState?.value?.zoomRatio
                ?: return

        // set max zoom limit to 3x or camera's max zoom ratio, expect if it exceeds 3x
        val maxLimitZoom = cameraInfo?.zoomState?.value?.maxZoomRatio?.let {
            if (it > staticMaxZoomLimit) { // check if the max zoom ratio exceeds 3x
                staticMaxZoomLimit // return 3x as the limit
            } else {
                cameraInfo?.zoomState?.value?.maxZoomRatio
                    ?: staticMaxZoomLimit // otherwise return the camera's max zoom ratio
            }
        }
            ?: staticMaxZoomLimit // fallback to 3x if max zoom ratio is not available, preventing null pointer exception

        val maxZoom =
            maxLimitZoom // set max zoom to 3x or camera's max zoom ratio, whichever is lower
        val minZoom = 2f

        /// cancel all previous zoom job
        zoomJob?.cancel()
        /// initiate new zoom process on main thread
        zoomJob = CoroutineScope(Dispatchers.Main).launch {
            var zoomRatio = currentZoom

            while (getProximityStatus() != ObjectProximityStatus.OPTIMAL) {
                /// calculate new zoom based on direction
                val newZoom = when (direction) {
                    // increase zoom ratio by 5% but not exceeding max zoom (from maxZoom above)
                    ZoomDirection.IN -> (zoomRatio * 1.05f).coerceAtMost(maxZoom)
                    ZoomDirection.OUT -> (zoomRatio * 0.96f).coerceAtLeast(minZoom)
                }

                /// show toast on max reached
                if (newZoom >= maxZoom) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.maximum_zoom_reached),
                        Toast.LENGTH_SHORT
                    ).show()
                    zoomJob?.cancel()
                } else if (newZoom == minZoom) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.minimum_zoom_reached),
                        Toast.LENGTH_SHORT
                    ).show()
                    zoomJob?.cancel()
                }

                /// apply zoom ratio
                zoomRatio = newZoom
                this@CameraController.currentZoom = zoomRatio
                cameraControl?.setZoomRatio(zoomRatio)

                delay(durationPerStep)
            }
            zoomJob = null
        }
    }

    // set camera auto exposure to given value
    fun setAutoExposure(
        exposureCompensation: Int,
        onExposureComplete: (() -> Unit)? = null
    ) {
        if (cameraInfo?.exposureState?.exposureCompensationRange?.contains(exposureCompensation) == true) {
            cameraControl?.setExposureCompensationIndex(exposureCompensation)
            Log.d(TAG, "Camera Exposure set to $exposureCompensation")
            onExposureComplete?.invoke()
        } else {
            Log.w(TAG, "Exposure compensation value $exposureCompensation is out of range.")
        }
    }

    /// setup focus instruction
    fun setAutoFocus(
        withDelay: Boolean = true,
        onFocusComplete: (() -> Unit)? = null
    ) {

        Log.d(TAG, "Camera Focusing")
        // Postpone the auto-focus if delay is requested
        val focusAction: () -> Unit = {
            previewView.afterMeasured {
                val autoFocusPoint = SurfaceOrientedMeteringPointFactory(
                    previewView.width.toFloat(),
                    previewView.width.toFloat()
                )
                    .createPoint(
                        previewView.width / 2f,
                        previewView.height / 2f
                    ) // Center of the preview
                try {
                    val autoFocusAction = FocusMeteringAction.Builder(
                        autoFocusPoint,
                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
                    )
                        .apply {
                            // Auto cancel after 2 seconds
                            setAutoCancelDuration(2L, TimeUnit.SECONDS)
                        }.build()
                    val future = cameraControl?.startFocusAndMetering(autoFocusAction)
                    future?.addListener({
                        if (future.isDone) {
                            Log.d(TAG, "Focus successful.")
                            onFocusComplete?.invoke()
                        } else {
                            Log.d(TAG, "Focus failed.")
                        }
                    }, ContextCompat.getMainExecutor(previewView.context))

                    Log.d(TAG, "AutoFocus initiated.")
                } catch (e: CameraInfoUnavailableException) {
                    Log.e(TAG, "Cannot access camera for focusing: ${e.message}", e)
                }
            }
        }

        // Execute focus action with or without delay
        if (withDelay) {
            lifecycleOwner.lifecycleScope.launch {
                delay(2.seconds)
                focusAction()
            }
        } else {
            focusAction()
        }
    }

    /// setup image capture
    @OptIn(ExperimentalZeroShutterLag::class)
    private fun setupImageCapture(): ImageCapture {
        return ImageCapture.Builder()
            .setFlashMode(FLASH_MODE_ON)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetRotation(Surface.ROTATION_0)
            .setJpegQuality(100)
            .build()
    }

    fun takePicture(
        outputFile: File,
        onSuccess: (ImageCapture.OutputFileResults) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        try {
            if (!cameraInitialized) {
                onError(
                    ImageCaptureException(
                        ImageCapture.ERROR_UNKNOWN,
                        context.getString(R.string.camera_not_initialized), null
                    )
                )
                return
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
            val backgroundExecutor = Executors.newSingleThreadExecutor()
            /// check if manual shutter speed is supported
            imageCapture.takePicture(
                outputOptions,
                backgroundExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        onSuccess(outputFileResults)
                        cameraControl?.enableTorch(false) // turn off torch after taking picture
                    }

                    override fun onError(exception: ImageCaptureException) {
                        onError(exception)
                    }
                }
            )
        } catch (e: Exception) {
            onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "Error taking picture", e))
        }
    }

    /// setup image analyzer
    private fun setImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetRotation(Surface.ROTATION_0)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply {
                try {
                    setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        /**
                         * detection with mutex lock prevent creating another background thread
                         * when current thread has not finished
                         */
                        CoroutineScope(Dispatchers.IO).launch {
                            detectionMutex.withLock {
//                                onImageProxyAnalyzed(imageProxy)
                                val bitmapImage =
                                    TypeConverter.imageProxyToRotatedBitmap(imageProxy)
                                onImageProxyAnalyzed(bitmapImage)
                                imageProxy.close()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error Setting Up Image Analysis: $e")
                    throw (e)
                }
            }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private suspend fun setLifecycleBoundCamera(): Camera {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()

        // Check for camera availability
        val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

        if (!hasBackCamera && !hasFrontCamera)
            throw Exception("No camera available")

        val lensFacing: Int = when {
            hasBackCamera -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_FRONT
        }
        // Prepare the camera selector based on lens facing direction
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val boundCamera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis,
            imageCapture
        )

        /// if hdr supported
        val isHdrSupported = context.isHdrSupported(cameraSelector)
        if (isHdrSupported) {
            Camera2CameraControl.from(boundCamera.cameraControl).setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CaptureRequest.CONTROL_SCENE_MODE_HDR
                    ).build()
            )
        } else {
            Log.w(TAG, "HDR is not supported on this device.")
        }

        return boundCamera
    }

    fun setFlashLight(boolean: Boolean) {
        Log.d(TAG, "Setting flashlight $boolean")
        _isFlashLightOn.value = boolean
        cameraControl?.enableTorch(_isFlashLightOn.value)
    }

    /// create focus meter to focus point on preview
    fun createFocusMeteringAction(
        x: Float = previewView.width / 2f,
        y: Float = previewView.height / 2f
    ): FocusMeteringAction {
        // Convert x, y to normalized coordinates (0 to 1)
        val factory = previewView.meteringPointFactory
        val meteringPoint = factory.createPoint(x, y)
        return FocusMeteringAction.Builder(meteringPoint).build()
    }

    fun triggerFocus(focusMeteringAction: FocusMeteringAction) {
        camera?.cameraControl?.startFocusAndMetering(focusMeteringAction)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalCamera2Interop::class)
    fun setPreviewStabilization() {
        camera?.let {
            val availableVideoStabilizationModes = Camera2CameraInfo.from(it.cameraInfo)
                .getCameraCharacteristic(
                    CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
                )

            if (
                availableVideoStabilizationModes?.contains(
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                ) == true
            ) {
                isEisOrOisSupported = true
                Log.w(TAG, "EIS is supported on this device.")
                Camera2CameraControl.from(it.cameraControl).setCaptureRequestOptions(
                    CaptureRequestOptions.Builder().setCaptureRequestOption(
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                        CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON
                    ).build()
                )
            } else {
                Log.w(TAG, "EIS is not supported on this device.")
            }
        }

    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setOpticalStabilization() {
        camera?.let {
            val availableOpticalStabilizationModes =
                Camera2CameraInfo.from(it.cameraInfo).getCameraCharacteristic(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
                )

            if (availableOpticalStabilizationModes != null && availableOpticalStabilizationModes.contains(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            ) {
                Log.w(TAG, "OIS is supported on this device.")
                isEisOrOisSupported = true
                // Check if OIS is available
                enableOpticalStabilization()
            } else {
                Log.w(TAG, "OIS is not supported on this device.")
            }
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun enableOpticalStabilization() {
        camera?.let {
            // Check for OIS and set it
            Camera2CameraControl.from(it.cameraControl).setCaptureRequestOptions(
                CaptureRequestOptions.Builder().setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                ).build()
            )
            Log.i(TAG, "Optical Image Stabilization enabled.")
        }

    }

    /**
     * Sets the target frames per second (FPS) range for the camera.
     *
     * This function attempts to set the camera's target FPS to 60 FPS if supported.
     * If 60 FPS is not available, it tries to set it to 30 FPS.
     * If neither 60 FPS nor 30 FPS is supported, the FPS range remains unchanged.
     *
     * This function uses [Camera2CameraInfo] to query available FPS ranges and
     * [Camera2CameraControl] to apply the desired FPS range.
     *
     * @OptIn [ExperimentalCamera2Interop] This annotation is used because the function
     * utilizes Camera2 interop APIs which are experimental.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun setTargetFpsRange() {
        camera?.let {
            val camera2CameraInfo = Camera2CameraInfo.from(it.cameraInfo)
            val fpsRanges =
                camera2CameraInfo.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            val targetFpsRange = when {
                fpsRanges?.contains(Range(60, 60)) == true -> Range(60, 60)
                fpsRanges?.contains(Range(30, 30)) == true -> Range(30, 30)
                else -> null
            }

            targetFpsRange?.let { range ->
                Camera2CameraControl.from(it.cameraControl).setCaptureRequestOptions(
                    CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range)
                        .build()
                )
            }
        }
    }
}

@ExperimentalCamera2Interop
private fun Context.isHdrSupported(cameraSelector: CameraSelector): Boolean {

    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    val cameraProvider = cameraProviderFuture.get()
    val camera =
        this.getLifecycleOwner()?.let { cameraProvider.bindToLifecycle(it, cameraSelector) }
    val camera2Info = camera?.let { Camera2CameraInfo.from(it.cameraInfo) }

    val characteristics =
        camera2Info?.getCameraCharacteristic(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
    return characteristics?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) == true
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                continuation.resume(cameraProvider.get())
            }, ContextCompat.getMainExecutor(this))
        }
    }

private fun Context.getLifecycleOwner(): LifecycleOwner? {
    var context = this
    while (context is ContextWrapper) {
        if (context is LifecycleOwner) {
            return context
        }
        context = context.baseContext
    }
    return null
}

inline fun View.afterMeasured(crossinline block: () -> Unit) {
    if (measuredWidth > 0 && measuredHeight > 0) {
        block()
    } else {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }
}
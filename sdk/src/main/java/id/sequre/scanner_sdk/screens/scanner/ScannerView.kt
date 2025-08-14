package id.sequre.scanner_sdk.screens.scanner

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import id.sequre.scanner_sdk.R
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.common.enums.ZoomDirection
import id.sequre.scanner_sdk.common.utils.factory.ViewModelFactory
import id.sequre.scanner_sdk.common.utils.helper.handleException
import id.sequre.scanner_sdk.screens.scanner.camera.CameraController
import id.sequre.scanner_sdk.screens.scanner.components.ErrorView
import id.sequre.scanner_sdk.screens.scanner.defaults.ScannerViewColors
import id.sequre.scanner_sdk.screens.scanner.defaults.ScannerViewDefaults
import id.sequre.scanner_sdk.screens.scanner.enums.ScannerState
import kotlinx.coroutines.delay

@Composable
fun ScannerView(
    modifier: Modifier = Modifier,

    /// app id for sdk validation
    applicationId: Int = 0,

    /// save to gallery flag
    saveToGallery: Boolean = false,

    /// show detection border(frame) around detected object
    showDetectedBoundary: Boolean = false,

    /// fullscreen
    isFullScreen: Boolean = false,

    /// emit back button
    onNavigateBack: (() -> Unit)? = null,

    scannerState: ScannerState = ScannerState.Scanning,
    /// emit scanner state changes
    onStateChanged: (ScannerState) -> Unit = {},

    /// flag to pause camera session
    isPaused: Boolean = false,

    /// scan theme
    colors: ScannerViewColors = ScannerViewDefaults.defaultScannerViewColors,

    /// flash button composable
    flashButton: @Composable (() -> Unit, Boolean, Color) -> Unit = ScannerViewDefaults.flashButton,
) {
    /// get context lifecycle
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    /// viewmodel
    val viewModel: ScannerViewModel = viewModel(factory = ViewModelFactory(context))

    /// list detected object from tflite
    val listDetection by viewModel.listDetection.collectAsState()
    /// define object size
    val detectedObjectSize by viewModel.detectedObjectSize.collectAsState()

    /**
     * define object proximity [ObjectProximityStatus]
     */
    val objectProximityStatus by viewModel.objectProximityStatus.collectAsState()

    /// check if sdk is validated
    val isSDKValidated by viewModel.isSDKValidated.collectAsState()

    /// scanning state scanning, processing, success / error
    val vmScannerState by viewModel.scannerState.collectAsState()

    /// check if frame square is enabled
    val isFrameSquare by viewModel.isFrameSquare.collectAsState()

    /// check if manual zoom is enabled
    val isManualZoom by viewModel.isManualZoom.collectAsState()

    /**
     * initiate camera controller
     * run detection on image analysis
     */
    /// active zoom level based on supported zoom
    val adjustedZoomLevel by viewModel.adjustedZoomLevel.collectAsState()
    val cameraController =
        remember {
            CameraController(
                context,
                lifecycleOwner,
                preferredZoomLevel = 3f,
                onZoomChanged = viewModel::updateZoomLevel
            ) {
                /// image proxy feedback from image analysis camerax
                handleException(
                    onException = { e ->
                        onStateChanged(ScannerState.Error(e))
                    }
                ) {
                    /// only run analysis if scanner state is scanning
                    if (vmScannerState is ScannerState.Scanning) {
                        viewModel.detect(it)
                    }
                }
            }
        }

    /// check if flash is available and if turned on
    val isFlashAvailable by cameraController.isFlashAvailable.collectAsState()
    val isFlashLightOn by cameraController.isFlashLightOn.collectAsState()

    /// init function
    LaunchedEffect(Unit) {
        viewModel.setSaveToGalleryFlag(saveToGallery)
    }

    LaunchedEffect(isPaused) {
        viewModel.setIsPaused(isPaused)
    }

    /// emit scanner state to parent app
    LaunchedEffect(vmScannerState) {
        Log.e("ScannerView", "State Changed: ${vmScannerState as? ScannerState.Error}")
        onStateChanged(vmScannerState)
    }

    /// apply scanner state to viewmodel
    LaunchedEffect(scannerState) {
        if (vmScannerState is ScannerState.Error && scannerState is ScannerState.Scanning) {
            delay(1500)
            viewModel.updateScannerState(scannerState)
            cameraController.setFlashLight(true)
        }
    }

    LaunchedEffect(cameraController) {
        /**
         * pass camera controller instance to viewmodel
         * allows camera control inside viewmodel
         */
        viewModel.setCameraController(cameraController)
    }

    DisposableEffect(lifecycleOwner) {
        /// Start observing lifecycle events
        // Create an observer that reacts to lifecycle events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Start updates, animations, or listeners that need active focus
                    if (cameraController.cameraInitialized) {
                        cameraController.setFlashLight(isFlashLightOn)
                        cameraController.cameraControl?.setZoomRatio(cameraController.currentZoom)
                    }
                }

                else -> {}
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)
        /// End observing lifecycle events

        // The onDispose block runs when the Composable leaves the composition
        // or when the keys change. It's crucial for cleanup.
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    /// triggered once to reset state. make sure all state in initial value
    DisposableEffect(Unit, lifecycleOwner) {
        onDispose {
            viewModel.resetState()
        }
    }

    /**
     * listen for object proximity status and isManualZoom changed
     * control how zoomed in the camera based on objectProximityStatus
     * this always runs with half second delay of each run
     *
     * if manual zoom is enabled, it will not run auto zoom
     * if manual zoom is not enabled, it will run auto zoom
     */
    LaunchedEffect(objectProximityStatus) {
        /// skip if camera not yet initialized
        if (!cameraController.cameraInitialized) return@LaunchedEffect

        // if manual zoom is enabled, skip auto zoom. Else execute auto zoom
        if (!isManualZoom) {
            /// execute continuous zoom until optimal
            when (objectProximityStatus) {
                ObjectProximityStatus.TOO_CLOSE -> {
                    cameraController.smoothZoomContinuously(
                        direction = ZoomDirection.OUT,
                        getProximityStatus = { objectProximityStatus }
                    )
                }

                ObjectProximityStatus.TOO_FAR -> {
                    cameraController.smoothZoomContinuously(
                        direction = ZoomDirection.IN,
                        getProximityStatus = { objectProximityStatus }
                    )
                }

                else -> cameraController.cancelAllZoomProcess()
            }

            // update zoom level in viewmodel when currentZoom changes when auto zoom is running
            viewModel.updateZoomLevel(cameraController.currentZoom)
        }
    }

    LaunchedEffect(isManualZoom) {
        /// if manual zoom is enabled, cancel all zoom process
        // Separate this code with above to prevent auto zoom from running when manual zoom is enabled
        // this is to ensure that manual zoom does not conflict with auto zoom
        if (isManualZoom) { // check if manual zoom is enabled
            viewModel.manualZoomOperation() // call manual zoom operation in viewmodel
        } else { // if manual zoom is not enabled, set the zoom level to adjustedZoomLevel
            // set the current zoom level to adjustedZoomLevel
            cameraController.currentZoom = adjustedZoomLevel
        }
    }

    /// show scanner if sdk is validated
    if (isSDKValidated == true) {
        /**
         * scan content containing
         * camera view, detection frame and label
         */
        ScannerContent(
            modifier = modifier,
            context = context,
            adjustedZoomLevel = adjustedZoomLevel,
            isFlashAvailable = isFlashAvailable,
            isFlashLightOn = isFlashLightOn,
            flashButton = flashButton,
            isFullScreen = isFullScreen,
            cameraController = cameraController,
            objectProximityStatus = objectProximityStatus,
            detectedObjectSize = detectedObjectSize,
            listDetection = listDetection,
            showDetectedBoundary = showDetectedBoundary,
            isFrameSquare = isFrameSquare,
            isManualZoom = isManualZoom, // whether manual zoom is enabled
            setToggleZoomState = viewModel::toggleManualZoom, // toggle manual zoom state
            setZoomLevel = { // set zoom level
                // fallback to prevent value go less than 2x or more than 3x
                val zoomLevel = it.coerceIn(2f, 3f)
                viewModel.updateZoomLevel(zoomLevel) // update zoom level in viewmodel
                cameraController.cameraControl?.setZoomRatio(zoomLevel) // apply zoom level to camera control
            },
            scannerState = vmScannerState,
            colors = colors,
            updateCutoutParams = { cutoutSize, containerWidth, containerHeight ->
                viewModel.updateCutoutParams(cutoutSize, containerWidth, containerHeight)
            },
            onNavigateBack = onNavigateBack,
            onShouldShowGlareIndicator = viewModel::setShouldShowGlareIndicator,
        )
    } else {
        /// if sdk is not validated show error screen showing invalid sdk cred
        if (isSDKValidated == false) {
            ErrorView(
                painterSource = R.drawable.ill_box,
                title = stringResource(R.string.sdk_is_not_validated_yet),
                description = stringResource(R.string.you_need_to_validate_your_app_to_the_sdk_before_using_it),
            )
        }
    }
}

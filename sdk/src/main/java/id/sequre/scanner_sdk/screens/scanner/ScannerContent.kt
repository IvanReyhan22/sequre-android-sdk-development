package id.sequre.scanner_sdk.screens.scanner

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sequre.scanner_sdk.R
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.common.enums.SCANNER_TOP_PADDING_PERCENTAGE
import id.sequre.scanner_sdk.common.enums.SCANNER_TOP_PADDING_PERCENTAGE_COMPACT
import id.sequre.scanner_sdk.common.utils.helper.Flavor
import id.sequre.scanner_sdk.common.utils.helper.FlavorHelper
import id.sequre.scanner_sdk.common.utils.helper.PackageHelper
import id.sequre.scanner_sdk.common.utils.testing.ContentDescriptions
import id.sequre.scanner_sdk.common.utils.testing.TestTag
import id.sequre.scanner_sdk.screens.scanner.camera.CameraController
import id.sequre.scanner_sdk.screens.scanner.camera.CameraView
import id.sequre.scanner_sdk.screens.scanner.components.DetectionFrame
import id.sequre.scanner_sdk.screens.scanner.components.DetectionFrameColors
import id.sequre.scanner_sdk.screens.scanner.components.GlareIndicator
import id.sequre.scanner_sdk.screens.scanner.components.GlareIndicatorColors
import id.sequre.scanner_sdk.screens.scanner.components.RippleEffectCanvas
import id.sequre.scanner_sdk.screens.scanner.components.ScannerAppBar
import id.sequre.scanner_sdk.screens.scanner.components.ScannerAppBarColors
import id.sequre.scanner_sdk.screens.scanner.components.detectTapGestureWithRipple
import id.sequre.scanner_sdk.screens.scanner.defaults.ScannerViewColors
import id.sequre.scanner_sdk.screens.scanner.enums.ScannerState
import id.sequre.scanner_sdk.ui.theme.Gray
import id.sequre.scanner_sdk.ui.theme.LimeGreen
import id.sequre.scanner_sdk.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.detector.Detection
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScannerContent(
    context: Context,
    modifier: Modifier = Modifier,

    /// zoom level
    adjustedZoomLevel: Float,

    /// control flash
    isFlashAvailable: Boolean,
    isFlashLightOn: Boolean,
    flashButton: @Composable (() -> Unit, Boolean, Color) -> Unit,

    /// control if show fullscreen
    isFullScreen: Boolean,

    /// camera controller
    cameraController: CameraController,

    /**
     * object related
     * size, proximity, list detection and detection frame flag
     */
    objectProximityStatus: ObjectProximityStatus,
    detectedObjectSize: android.util.Size,
    listDetection: List<Detection>,
    showDetectedBoundary: Boolean,
    isFrameSquare: Boolean,

    // var that hold state if auto zoom is enabled
    isManualZoom: Boolean = false,
    // callback to switch between auto zoom and manual zoom
    setToggleZoomState: () -> Unit = {},
    // callback to set zoom level
    setZoomLevel: (Float) -> Unit = {},

    /// scanner controller
    scannerState: ScannerState,

    /// control scanner theme
    colors: ScannerViewColors,

    updateCutoutParams: (cutoutSize: Size, containerWidth: Float, containerHeight: Float) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    onShouldShowGlareIndicator: (Boolean) -> Unit,
) {

    val configuration = LocalConfiguration.current
    // calculate container size
    val containerWidth = with(LocalDensity.current) {
        configuration.screenWidthDp.dp.toPx()
    }
    val containerHeight = with(LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }

    // Calculate cutout size
    val screenHeightPx = with(LocalDensity.current) {
        configuration.screenHeightDp.dp.toPx()
    }
    val cutoutHeightPx = screenHeightPx / if (adjustedZoomLevel >= 5f) 1.75f else 2f
    val cutoutWidthPx: Float = cutoutHeightPx * 9 / 16
    val cutoutSize = Size(cutoutWidthPx, cutoutHeightPx)

    /// get device screen height in dp
    val screenHeightDp = configuration.screenHeightDp
    val dynamicHeight =
        (screenHeightDp * (
                if (adjustedZoomLevel >= 5f)
                    SCANNER_TOP_PADDING_PERCENTAGE_COMPACT
                else
                    SCANNER_TOP_PADDING_PERCENTAGE)).dp

    /// ripple effect on screen tap
    val rippleRadius1 = remember { Animatable(0f) }
    val rippleAlpha1 = remember { Animatable(0f) }
    val rippleRadius2 = remember { Animatable(1f) }
    val rippleAlpha2 = remember { Animatable(0f) }
    var rippleCenter by remember { mutableStateOf(Offset.Zero) }
    val rippleScope = rememberCoroutineScope()

    var showGlareIndication by remember { mutableStateOf(false) }

    // check if current flavor is production
    val isProduction = FlavorHelper.currentFlavor == Flavor.PRODUCTION

    // State to track if "min zoom" or "max zoom" toast is currently active
    var isShowingLimitZoomToast by remember { mutableStateOf(false) }

    /**
     * listen for [objectProximityStatus] changes
     * show glare indicator on glare detection
     */
    LaunchedEffect(objectProximityStatus) {
        /// show glare indicator on status glared
        if (objectProximityStatus == ObjectProximityStatus.GLARED) {
            showGlareIndication = true
        }
        /// give 3 seconds before re-check show/hide glare indicator
        delay(3.seconds)
        /// re-evaluate if still glare status
        /// this prevent flickering and spamming glare indicator
        showGlareIndication = objectProximityStatus == ObjectProximityStatus.GLARED
    }

    /// changes to container size
    LaunchedEffect(cutoutSize, containerWidth, containerHeight) {
        updateCutoutParams(cutoutSize, containerWidth, containerHeight)
    }

    /// listen for [showGlareIndication] changes and emit callback to parent
    LaunchedEffect(showGlareIndication) {
        onShouldShowGlareIndicator(showGlareIndication)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize(),
        topBar = {
            Column {
                ScannerAppBar(
                    context = context,
                    onNavigateBack = onNavigateBack,
                    isFlashAvailable = isFlashAvailable,
                    isFlashLightOn = isFlashLightOn,
                    flashButton = flashButton,
                    modifier = Modifier,
                    toggleFlash = {
                        cameraController.setFlashLight(!isFlashLightOn)
                    },
                    colors = ScannerAppBarColors(
                        containerColor = if (isFullScreen) Color.Transparent else colors.appBarContainerColor,
                        contentColor = if (isFullScreen) Color.White else colors.appBarContentColor
                    )
                )

                if (showGlareIndication) {
                    // tilt the phone indicator
                    GlareIndicator(
                        colors = GlareIndicatorColors(
                            containerColor = colors.glareIndicatorContainerColor,
                            contentColor = colors.glareIndicatorContentColor
                        )
                    )
                }

            }
        }
    ) {
        Box(modifier = Modifier.padding(it))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Transparent)
                .detectTapGestureWithRipple(
                    rippleScope = rippleScope,
                    onTap = { tapOffset ->
                        rippleCenter = tapOffset
                        cameraController.setAutoFocus()
                    },
                    rippleRadius1 = rippleRadius1,
                    rippleAlpha1 = rippleAlpha1,
                    rippleRadius2 = rippleRadius2,
                    rippleAlpha2 = rippleAlpha2,
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            /// Camera feed
            CameraView(
                cameraController = cameraController,
//                isPreviewVisible = scannerState == ScannerState.Scanning
                isPreviewVisible = scannerState !is ScannerState.Processing
            )

            /// boundary frame
            DetectionFrame(
                cutoutSize = cutoutSize,
                detectedObjectSize = detectedObjectSize,
                detectionResults = listDetection,
                showDetectedBoundary = showDetectedBoundary,
                objectProximityStatus = objectProximityStatus,
                colors = DetectionFrameColors(
                    detectionFrameColor = colors.detectionFrameColor,
                    detectionFrameOptimalColor = colors.detectionFrameOptimalColor,
                ),
                isFrameSquare = isFrameSquare,
            )

            // Scanner label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(dynamicHeight))
                Text(
                    stringResource(R.string.scanner_label_title),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.scanner_label_content),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Normal
                    )
                )
            }


            if (onNavigateBack != null) {
                Text(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    text = "v${PackageHelper.getVersionName(context)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.onSurfaceColor
                    )
                )
            }

            // Draw the concentric ripples
            RippleEffectCanvas(
                rippleCenter = rippleCenter,
                rippleRadius1 = rippleRadius1.value,
                rippleAlpha1 = rippleAlpha1.value,
                rippleRadius2 = rippleRadius2.value,
                rippleAlpha2 = rippleAlpha2.value
            )

            // if not in production, show manual zoom controls
            if (!isProduction) {
                /**
                 * This Row composable is responsible for displaying the zoom controls.
                 * It is aligned to the bottom center of the screen and has padding at the bottom.
                 * It contains two main sections:
                 * 1. Manual zoom controls (decrease, current zoom level, increase) - displayed only if `isManualZoom` is true.
                 * 2. A toggle switch to switch between manual and auto zoom.
                 */
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            bottom = it.calculateBottomPadding() + 24.dp,
                            start = 20.dp,
                            end = 20.dp
                        )
                ) {
                    // This inner Row handles the layout of the manual zoom controls.
                    // It takes up 1f weight, distributing space around its children.
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isManualZoom) {
                            // manual zoom with decrease and increase buttons
                            // icon button to decrement zoom level
                            IconButton(
                                onClick = {
                                    if (adjustedZoomLevel == 2f) {
                                        // If min zoom reached AND no toast is currently active
                                        if (!isShowingLimitZoomToast) {
                                            isShowingLimitZoomToast = true // Set flag to true
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.minimum_zoom_reached),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            rippleScope.launch {
                                                delay(3000) // Wait for the debounce duration
                                                isShowingLimitZoomToast =
                                                    false // Reset flag after delay
                                            }
                                        }
                                    } else {
                                        setZoomLevel(adjustedZoomLevel - 0.5f)
                                    }
                                },
                                modifier = Modifier
                                    .background(
                                        White, shape = MaterialTheme.shapes.small.copy(
                                            all = CornerSize(50.dp)
                                        )
                                    )
                                    .semantics {
                                        testTagsAsResourceId = true
                                        contentDescription = ContentDescriptions.DECREASE_ZOOM
                                        testTag = TestTag.DECREASE_ZOOM
                                    },
                            ) {
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                )
                            }
                        }
                        // display current zoom level
                        Text(
                            text = adjustedZoomLevel.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            ),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                        )
                        if (isManualZoom) {
                            // icon button to increment zoom level
                            IconButton(
                                onClick = {
                                    if (adjustedZoomLevel == cameraController.camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ||
                                        adjustedZoomLevel == 3f
                                    ) {
                                        // If max zoom reached AND no toast is currently active
                                        if (!isShowingLimitZoomToast) {
                                            isShowingLimitZoomToast = true // Set flag to true
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.maximum_zoom_reached),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            rippleScope.launch {
                                                delay(3000) // Wait for the debounce duration
                                                isShowingLimitZoomToast =
                                                    false // Reset flag after delay
                                            }
                                        }
                                    } else {
                                        setZoomLevel(adjustedZoomLevel + 0.5f)
                                    }
                                },
                                modifier = Modifier
                                    .background(
                                        White, shape = MaterialTheme.shapes.small.copy(
                                            all = CornerSize(50.dp)
                                        )
                                    )
                                    .semantics {
                                        testTagsAsResourceId = true
                                        contentDescription = ContentDescriptions.INCREASE_ZOOM
                                        testTag = TestTag.INCREASE_ZOOM
                                    },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Increase Zoom",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.Black,
                                )
                            }
                        }
                    }

                    // This Column contains the toggle switch for auto/manual zoom and its label.
                    // It also takes up 1f weight and centers its content.
                    Column(
                        modifier = Modifier.weight(1f),

                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Text indicating the action of the switch.
                        Text(
                            text = if (isManualZoom) {
                                "Switch to auto zoom"
                            } else {
                                "Switch to manual zoom"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        )
                        // Switch to toggle between manual and auto zoom.
                        Switch(
                            checked = isManualZoom,
                            onCheckedChange = { setToggleZoomState() },
                            colors = MaterialTheme.colorScheme.run {
                                SwitchDefaults.colors(
                                    checkedThumbColor = White,
                                    uncheckedThumbColor = White,
                                    checkedTrackColor = LimeGreen,
                                    uncheckedTrackColor = Gray,
                                    checkedBorderColor = LimeGreen,
                                    uncheckedBorderColor = Gray
                                )
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .semantics {
                                    testTagsAsResourceId = true
                                    contentDescription = ContentDescriptions.TOGGLE_ZOOM
                                    testTag = TestTag.TOGGLE_ZOOM
                                },
                        )
                    }
                }
            }
        }
    }
}
package id.sequre.scanner_sdk.screens.scanner.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import id.sequre.scanner_sdk.ui.theme.Black

@Composable
fun CameraView(
    cameraController: CameraController,
    modifier: Modifier = Modifier,
    isPreviewVisible: Boolean = true
) {
    /// camera preview
    val previewView = cameraController.previewView

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        /// camera view
        if (isPreviewVisible) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Black))
        }
    }
}





package id.sequre.sample

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import id.sequre.sample.ui.theme.SequreTheme
import id.sequre.scanner_sdk.screens.scanner.ScannerView
import id.sequre.scanner_sdk.screens.scanner.enums.ScannerState


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SequreTheme {
                val context = LocalContext.current

                val scrollState = rememberScrollState()

                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val gson: Gson = GsonBuilder().setPrettyPrinting().create()

                var hasCameraPermission by remember { mutableStateOf(checkCameraPermission()) }
                var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }

                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    hasCameraPermission = isGranted
                }

                Scaffold {
                    Box(Modifier.padding(it))

                    // ask camera permission to user
                    PermissionAwareScannerView(
                        applicationId = 179507,
                        scannerState = scannerState,
                        onStateChanged = { state -> scannerState = state },
                        hasCameraPermission = hasCameraPermission,
                        requestPermissionLauncher = requestPermissionLauncher
                    )

                    when (scannerState) {
                        is ScannerState.Error -> {
                            ModalBottomSheet(
                                onDismissRequest = {
                                    scannerState = ScannerState.Scanning
                                },
                                sheetState = sheetState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 36.dp),
                                containerColor = Color.White
                            ) {

                                val exception = (scannerState as ScannerState.Error).exception
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .verticalScroll(scrollState)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Error Occurred",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = Color.Black
                                            )
                                        )
                                        exception.localizedMessage?.let { it1 ->
                                            Text(
                                                it1,
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    color = Color.Black
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is ScannerState.Processing -> {
                            Log.i("SampleApp", "PROCESSING")
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column {
                                    CircularProgressIndicator()
                                    Text((scannerState as ScannerState.Processing).text)
                                }
                            }
                        }

                        is ScannerState.Success -> {
                            val selectedImage = (scannerState as ScannerState.Success).bitmap
                            val res = (scannerState as ScannerState.Success).scanProductResponse
                            val qrResult = (scannerState as ScannerState.Success).scanResult

                            ModalBottomSheet(
                                onDismissRequest = {
                                    (context as? Activity)?.recreate()
                                },
                                sheetState = sheetState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 36.dp),
                                containerColor = Color.White
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {

                                    Column(
                                        modifier = Modifier
                                            .verticalScroll(scrollState)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            qrResult.name,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = Color.Black
                                            )
                                        )
                                        Text(
                                            "Image Resolution: ${selectedImage?.width} :  ${selectedImage?.height}",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = Color.Black
                                            )
                                        )

                                        selectedImage.let {
                                            if (it == null) {
                                                Image(
                                                    painter = painterResource(id.sequre.scanner_sdk.R.drawable.ill_box),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.FillWidth,
                                                    modifier = Modifier
                                                        .padding(vertical = 16.dp)
                                                        .size(144.dp)
                                                )
                                            } else {
                                                Image(
                                                    bitmap = selectedImage!!.asImageBitmap(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.FillWidth,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                )
                                            }
                                        }
                                        Text(
                                            gson.toJson(res),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                color = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionAwareScannerView(
    applicationId: Int,
    scannerState: ScannerState,
    onStateChanged: (ScannerState) -> Unit,
    hasCameraPermission: Boolean,
    requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        ScannerView(
            applicationId = applicationId,
            saveToGallery = true,
            scannerState = scannerState,
            showDetectedBoundary = true,
            onStateChanged = onStateChanged,
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Camera permission is required to use this feature. Please grant permission in settings.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                ElevatedButton(onClick = {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}



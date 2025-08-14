package id.sequre.scanner_sdk.screens.scanner.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.sequre.scanner_sdk.common.utils.helper.PackageHelper
import id.sequre.scanner_sdk.screens.scanner.defaults.DefaultFlashButton
import id.sequre.scanner_sdk.screens.scanner.defaults.ScannerViewDefaults.flashButton

data class ScannerAppBarColors(
    val containerColor: Color = Color.Transparent,
    val contentColor: Color = Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerAppBar(
    modifier: Modifier = Modifier,
    context: Context,
    onNavigateBack: (() -> Unit)?,

    isFlashAvailable: Boolean = false,
    isFlashLightOn: Boolean,
    toggleFlash: () -> Unit,
    flashButton: @Composable (() -> Unit, Boolean, Color) -> Unit = { onFlashButtonClick, isFlashLightOn, color->
        DefaultFlashButton(
            onClick = onFlashButtonClick,
            isFlashLightOn = isFlashLightOn,
            color = color
        )
    },
    colors: ScannerAppBarColors = ScannerAppBarColors(),
) {

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.containerColor,
        ),
        navigationIcon = {
        },
        actions = {

        },
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                onNavigateBack?.let {
                    IconButton(
                        onClick = {
                            onNavigateBack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = colors.contentColor
                        )
                    }
                } ?: Text(
                    text = "v${PackageHelper.getVersionName(context)}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = colors.contentColor
                    )
                )

                Spacer(modifier = Modifier)
                if (isFlashAvailable) {
                    flashButton(
                        {
                            toggleFlash()
                        },
                        isFlashLightOn,
                        colors.contentColor
                    )
                }
            }

        },
    )


}

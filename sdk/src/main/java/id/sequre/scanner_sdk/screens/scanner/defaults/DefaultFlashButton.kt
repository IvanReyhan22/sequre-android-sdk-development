package id.sequre.scanner_sdk.screens.scanner.defaults

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import id.sequre.scanner_sdk.R

@Composable
fun DefaultFlashButton(

    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isFlashLightOn: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface
) {

    Box(
        modifier = modifier
            .clickable {
                onClick()
            }
            .padding(paddingValues = PaddingValues(8.dp)), // inner padding
        contentAlignment = Alignment.Center

    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(if (isFlashLightOn) R.drawable.ic_flash else R.drawable.ic_flash_off),
            contentDescription = null,
            tint = color
        )
    }
}
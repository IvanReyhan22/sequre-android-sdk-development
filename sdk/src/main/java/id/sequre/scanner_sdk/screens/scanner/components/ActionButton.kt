package id.sequre.scanner_sdk.screens.scanner.components


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.sequre.scanner_sdk.R

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.click_me),
    enabled: Boolean = true,
    radius: Int = 16,
    onClick: () -> Unit = {},
) {
    Button(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(radius.dp),
        onClick = { onClick() },
        enabled = enabled

    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
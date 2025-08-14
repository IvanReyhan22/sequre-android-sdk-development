package id.sequre.scanner_sdk.screens.scanner.defaults

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sequre.scanner_sdk.ui.theme.BurntOrange

@Composable
fun DefaultScannerLabel(
    modifier: Modifier = Modifier,
    title: String = "Default Label",
) {
    Box(
        modifier = modifier
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = BurntOrange),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
            text = title,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
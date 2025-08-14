package id.sequre.scanner_sdk.screens.scanner.components

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import id.sequre.scanner_sdk.R



data class GlareIndicatorColors(
    val containerColor: Color = Color.Transparent,
    val contentColor: Color = Color.White
)

@Composable
fun GlareIndicator(
    colors: GlareIndicatorColors = GlareIndicatorColors(),
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .components {
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.containerColor),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context)
                    .data(R.drawable.ill_tilt_phone)
                    .build(),
                imageLoader = imageLoader
            ),
            contentDescription = "ill_tilt_phone",
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 8.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.glare_detection_notice),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = colors.contentColor,
            textAlign = TextAlign.Center
        )

    }
}

@Preview(showBackground = true)
@Composable
fun GlareIndicatorPreview() {
    GlareIndicator()
}
package id.sequre.scanner_sdk.screens.scanner.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import id.sequre.scanner_sdk.R
import id.sequre.scanner_sdk.common.enums.ObjectProximityStatus
import id.sequre.scanner_sdk.screens.scanner.defaults.DefaultScannerLabel

@Composable
fun LabelView(
    modifier: Modifier = Modifier,
    cutoutSize: Size = Size(0f, 0f),
    label: @Composable (Modifier, String) -> Unit = { labelModifier, labelTitle ->
        DefaultScannerLabel(labelModifier, labelTitle)
    },
    detectionStatus: ObjectProximityStatus = ObjectProximityStatus.UNDETECTED,
) {
    if (detectionStatus == ObjectProximityStatus.UNDETECTED ||
        detectionStatus == ObjectProximityStatus.GLARED) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        label(
            modifier
                .widthIn(max = cutoutSize.width.dp - 32.dp)
                .padding(horizontal = 16.dp)
                .padding(top = (cutoutSize.height / 4).dp),
            when (detectionStatus) {
                ObjectProximityStatus.OPTIMAL -> stringResource(R.string.hold_this_position)
                ObjectProximityStatus.OUTSIDE -> stringResource(R.string.not_aligned)
                ObjectProximityStatus.TOO_FAR -> stringResource(R.string.too_far)
                ObjectProximityStatus.TOO_CLOSE -> stringResource(R.string.too_close)
                else -> ""
            }
        )
    }
}

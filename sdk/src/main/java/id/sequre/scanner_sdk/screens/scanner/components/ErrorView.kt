package id.sequre.scanner_sdk.screens.scanner.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.sequre.scanner_sdk.R

@Composable
fun ErrorView(
    modifier: Modifier = Modifier,
    painterSource: Int = R.drawable.ill_box,
    title: String,
    description: String,
    actionTitle: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    Scaffold {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(it)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box {}
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(
                        painterSource
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(size = (screenWidthDp / 2).dp)
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    ),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = description,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }

            if (onAction != null) {
                ActionButton(
                    modifier = Modifier.padding(top = 16.dp),
                    title = actionTitle ?: stringResource(R.string.proceed),
                    onClick = {
                        onAction()
                    }
                )
            }else {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

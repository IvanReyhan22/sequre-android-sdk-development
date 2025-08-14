package id.sequre.scanner_sdk.common.utils.helper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


inline fun handleException(
    onException: (Exception) -> Unit,
    block: () -> Unit
) {
    try {
        block()
    } catch (e: Exception) {
        onException(e)
    }
}

fun CoroutineScope.launchSafely(
    onError: (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit
) = launch {
    handleException(
        onException = {
            if (it is CancellationException) throw it
            onError(it)
        }
    ) {
        block()
    }
}

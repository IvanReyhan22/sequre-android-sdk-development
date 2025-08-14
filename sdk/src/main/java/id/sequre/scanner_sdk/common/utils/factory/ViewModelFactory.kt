package id.sequre.scanner_sdk.common.utils.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import id.sequre.scanner_sdk.R
import id.sequre.scanner_sdk.data.repository.SDKRepository
import id.sequre.scanner_sdk.data.repository.ScannerRepository
import id.sequre.scanner_sdk.screens.scanner.ScannerViewModel

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScannerViewModel(
                context = context,
                scannerRepository = ScannerRepository(),
                sdkRepository = SDKRepository()
            ) as T
        }
        throw IllegalArgumentException(context.getString(R.string.unknown_viewmodel_class))
    }
}
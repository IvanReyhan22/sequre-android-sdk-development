package id.sequre.scanner_sdk.common.enums

enum class ScanResult {
    QR_GENUINE,
    QR_POOR_IMAGE,
    QR_FAKE,
    QR_NUMBER_UNKNOWN;

    companion object {
        fun getScanResult(name: String): ScanResult {
            return try {
                valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                QR_NUMBER_UNKNOWN
            }
        }
    }
}
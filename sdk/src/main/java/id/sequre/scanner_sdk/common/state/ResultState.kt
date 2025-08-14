package id.sequre.scanner_sdk.common.state

/**
 * A sealed class representing the possible states of a result in the application.
 * It can either be a successful result with data or an error result with additional information.
 *
 * @param T The type of the data that is returned in a successful result.
 */
sealed class ResultState<out T> {
    /**
     * Represents a successful result, containing the data returned.
     *
     * @param T The type of data returned in the success state.
     * @property data The data returned in the successful result.
     */
    data class Success<out T>(val data: T) : ResultState<T>()

    /**
     * Represents an error result, containing information about the failure.
     *
     * @property statusCode The HTTP status code or error code related to the failure (nullable).
     * @property message A message describing the error.
     * @property data Any additional data related to the error (nullable).
     */
    data class Error(
        val statusCode: Int? = null,
        val message: String,
        val data: Any? = null
    ) : ResultState<Nothing>()
}
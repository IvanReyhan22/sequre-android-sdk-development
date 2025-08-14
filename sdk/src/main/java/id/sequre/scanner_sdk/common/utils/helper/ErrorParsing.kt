package id.sequre.scanner_sdk.common.utils.helper

import android.util.Log
import com.google.gson.Gson
import okhttp3.ResponseBody

/**
 * A utility object that handles parsing error responses from a server and extracting error messages.
 * This object provides a function to parse a response body into a specified error class and retrieve
 * the error message.
 */
object ErrorParsing {

    /**
     * Parses the response body to extract the error message and maps the error response into a specified class.
     *
     * @param responseBody The raw response body received from the server.
     * @param errorClass The class type to parse the error response into.
     * @return A pair containing the error message and the parsed error response object (if successful).
     *         If parsing fails, it returns a default "Unknown error" message and null for the error object.
     */
    fun <T> responseParseError(
        responseBody: ResponseBody?,
        errorClass: Class<T>
    ): Pair<String, T?> {
        return responseBody?.string()?.let { jsonString ->
            try {
                // parse error response to class object
                val errorResponse = Gson().fromJson(jsonString, errorClass)

                // get error message
                val messageField = errorClass.getDeclaredField("message")
                messageField.isAccessible = true
                val errorMessage = messageField.get(errorResponse) as? String
                    ?: "Unable to communicate with server, please try again"

                // return value
                Pair(errorMessage, errorResponse)

            } catch (exception: Exception) {
                Log.e("ErrorParsing", "responseParseError :: ${exception.localizedMessage}")
                Pair("Unknown error :: ${exception.localizedMessage}", null)
            }
        } ?: Pair("Unknown error", null)
    }
}
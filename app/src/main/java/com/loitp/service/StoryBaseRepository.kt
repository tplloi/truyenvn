package com.loitp.service

import com.core.base.BaseApplication
import com.service.RequestStatus
import com.service.model.ErrorJson
import com.service.model.ErrorResponse
import retrofit2.Response

/**
 * Created by Loitp on 24,December,2019
 * HMS Ltd
 * Ho Chi Minh City, VN
 * www.muathu@gmail.com
 */
open class StoryBaseRepository {

    suspend fun <T : Any> makeApiCall(call: suspend () -> Response<StoryApiResponse<T>>): StoryApiResponse<T> {

        return try {
            val response = call.invoke()

            if (response.isSuccessful) {
                response.body() ?: run {
                    StoryApiResponse<T>(status = true, items = null)
                }
            } else {
                if (response.code() == RequestStatus.NO_AUTHENTICATION.value) {
                    StoryApiResponse<T>(
                        status = false,
                        errorCode = RequestStatus.NO_AUTHENTICATION.value,
                        errors = ErrorResponse(message = "error_login"),
                        items = null
                    )
                } else {
                    handleError(response = response)
                }
            }

        } catch (ex: Exception) {
            val error = ex.message
            StoryApiResponse(
                status = false,
                errorCode = null,
                errors = ErrorResponse(error),
                items = null
            )
        }
    }

    private fun <T : Any> handleError(response: Response<StoryApiResponse<T>>?): StoryApiResponse<T> {
        var errorResponse: ErrorResponse? = null
        response?.errorBody()?.let {
            try {
                // parser error body
                val jsonError = it.string()
                val errorJson =
                    BaseApplication.gson.fromJson(jsonError, ErrorJson::class.java) as ErrorJson
                errorResponse = errorJson.errors?.firstOrNull()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (errorResponse == null) {
            when {
                response?.code() == RequestStatus.BAD_GATEWAY.value -> return StoryApiResponse(
                    status = false,
                    errors = ErrorResponse(message = "error_bad_gateway"),
                    items = null,
                    errorCode = response.code()
                )
                response?.code() == RequestStatus.INTERNAL_SERVER.value -> return StoryApiResponse(
                    status = false,
                    errors = ErrorResponse(message = "error_internal_server"),
                    items = null,
                    errorCode = response.code()
                )
                else -> return StoryApiResponse(
                    status = false,
                    errors = ErrorResponse(message = "error_internal_server"),
                    items = null
                )
            }
        }

        return StoryApiResponse(
            status = false,
            errors = ErrorResponse(message = errorResponse?.message),
            items = null,
            errorCode = errorResponse?.code
        )
    }
}

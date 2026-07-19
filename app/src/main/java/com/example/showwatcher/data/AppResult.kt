package com.example.showwatcher.data

/** Result wrapper for repository calls that can fail over the network (ANDROID_HANDOFF.md §5). */
sealed class AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()
}

sealed class AppError {
    /** TMDB returned a non-2xx response with a `status_message` body (§5). */
    data class Tmdb(val message: String, val code: Int?) : AppError()

    /** No connectivity, timeout, or other transport-level failure. */
    data class Network(val cause: Throwable) : AppError()

    /** A show with this tmdb_id already exists (§4.1's UNIQUE constraint). */
    data object AlreadyAdded : AppError()

    data class Unknown(val cause: Throwable) : AppError()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}

package com.example.showwatcher.ui.common

import com.example.showwatcher.data.AppError

fun AppError.toUserMessage(): String = when (this) {
    is AppError.Tmdb -> message
    is AppError.Network -> "Couldn't reach the network. Check your connection and try again."
    is AppError.AlreadyAdded -> "That show is already being tracked."
    is AppError.Unknown -> cause.message ?: "Something went wrong."
}

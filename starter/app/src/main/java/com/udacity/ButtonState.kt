package com.udacity


sealed class ButtonState {
    object Unclicked : ButtonState()
    object Loading : ButtonState()
    object Completed : ButtonState()
}
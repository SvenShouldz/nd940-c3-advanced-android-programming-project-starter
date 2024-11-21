package com.udacity


sealed class ButtonState {
    object Default : ButtonState()
    object Loading : ButtonState()
    object Pending : ButtonState()
    object Failed : ButtonState()
    object Completed : ButtonState()
}
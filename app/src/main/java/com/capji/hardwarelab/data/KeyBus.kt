package com.capji.hardwarelab.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Bridge between MainActivity key events and the Volume Keys test. */
object KeyBus {
    @Volatile
    var capturing: Boolean = false

    val events = MutableSharedFlow<Int>(extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)
}

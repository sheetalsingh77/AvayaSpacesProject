package com.avayaspacesproject.ui.events

import java.util.concurrent.atomic.AtomicBoolean

open class SingleLiveEvent {
    private val pending = AtomicBoolean(false)

    override fun toString(): String = javaClass.simpleName
}
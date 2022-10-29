package com.avayaspacesproject.ui.events

data class PasswordRequiredEvent(
    val invalid: Boolean
) : SingleLiveEvent() {

    override fun toString(): String =
        StringBuilder(128).apply {
            append(this@PasswordRequiredEvent.javaClass.simpleName)
            append('{')
            if (invalid) {
                append("invalid")
            } else {
                append("not invalid")
            }
            append('}')
        }.toString()
}
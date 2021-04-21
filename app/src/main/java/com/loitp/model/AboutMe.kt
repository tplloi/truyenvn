package com.loitp.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class AboutMe(
    val id: Int = 0,
    var name: String = "",
    var aboutMe: String = ""
) : Serializable

package com.loitp.model

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class Banner(
    val bannerImage: Int = 0
) : Serializable
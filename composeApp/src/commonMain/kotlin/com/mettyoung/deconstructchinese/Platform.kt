package com.mettyoung.deconstructchinese

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
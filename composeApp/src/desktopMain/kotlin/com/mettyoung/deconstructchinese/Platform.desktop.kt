package com.mettyoung.deconstructchinese

class DesktopPlatform : Platform {
    override val name: String =
        "Desktop ${System.getProperty("os.name")} ${System.getProperty("os.version")}"
}

actual fun getPlatform(): Platform = DesktopPlatform()

actual val isWebPlatform: Boolean = false

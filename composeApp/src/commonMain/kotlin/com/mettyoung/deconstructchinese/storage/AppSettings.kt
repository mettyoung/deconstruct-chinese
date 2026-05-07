package com.mettyoung.deconstructchinese.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object AppSettings {
    private val settings: Settings = Settings()
    private const val KEY_USE_SIMPLIFIED = "use_simplified"

    var useSimplified: Boolean
        get() = settings.getBoolean(KEY_USE_SIMPLIFIED, false)
        set(value) {
            settings[KEY_USE_SIMPLIFIED] = value
        }
}

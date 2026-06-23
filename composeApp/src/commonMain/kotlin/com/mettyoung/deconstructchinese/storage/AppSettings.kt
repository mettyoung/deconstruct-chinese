package com.mettyoung.deconstructchinese.storage

import com.mettyoung.deconstructchinese.config.defaultApiKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

object AppSettings {
    private val settings: Settings = Settings()
    private const val KEY_USE_SIMPLIFIED = "use_simplified"
    private const val KEY_API_KEY = "doubao_api_key"

    var useSimplified: Boolean
        get() = settings.getBoolean(KEY_USE_SIMPLIFIED, false)
        set(value) {
            settings[KEY_USE_SIMPLIFIED] = value
        }

    var apiKey: String
        get() = settings.getString(KEY_API_KEY, defaultApiKey)
        set(value) {
            settings[KEY_API_KEY] = value
        }
}

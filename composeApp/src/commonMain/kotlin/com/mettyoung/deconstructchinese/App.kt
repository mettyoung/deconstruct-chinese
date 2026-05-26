package com.mettyoung.deconstructchinese

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.mettyoung.deconstructchinese.storage.AppSettings
import com.mettyoung.deconstructchinese.ui.screens.TranslatorRoute
import com.mettyoung.deconstructchinese.ui.theme.DeconstructTheme

@Composable
@Preview
fun App() {
    DeconstructTheme {
        var apiKey by remember { mutableStateOf(AppSettings.apiKey) }
        TranslatorRoute(
            apiKey = apiKey,
            onApiKeySubmit = { AppSettings.apiKey = it; apiKey = it }
        )
    }
}

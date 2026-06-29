package com.mettyoung.deconstructchinese

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mettyoung.deconstructchinese.ui.screens.TranslatorRoute
import com.mettyoung.deconstructchinese.ui.theme.DeconstructTheme

@Composable
@Preview
fun App() {
    DeconstructTheme {
        TranslatorRoute()
    }
}

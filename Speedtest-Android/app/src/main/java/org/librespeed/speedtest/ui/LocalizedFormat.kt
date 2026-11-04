package org.librespeed.speedtest.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The device locale, read through the configuration so that changing it
 * recomposes what was formatted with it. Locale.getDefault() would not: it is
 * a plain global read, which is what Compose's NonObservableLocale flags.
 */
val currentLocale: Locale
    @Composable
    @ReadOnlyComposable
    get() = LocalConfiguration.current.locales[0]

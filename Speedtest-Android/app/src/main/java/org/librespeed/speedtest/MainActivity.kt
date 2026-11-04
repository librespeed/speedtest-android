package org.librespeed.speedtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import org.librespeed.speedtest.data.AppPreferences
import org.librespeed.speedtest.data.ClientInfo
import org.librespeed.speedtest.ui.App
import org.librespeed.speedtest.ui.HingeBounds
import org.librespeed.speedtest.ui.theme.LibreSpeedTheme
import org.librespeed.speedtest.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.fdossena.speedtest.core.base.Connection.setUserAgent(ClientInfo.userAgent)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { AppPreferences(applicationContext) }
            val themeMode by prefs.themeMode.collectAsStateWithLifecycle(initialValue = "system")
            val windowSizeClass = calculateWindowSizeClass(this)
            val layoutInfo by remember { WindowInfoTracker.getOrCreate(this).windowLayoutInfo(this) }
                .collectAsStateWithLifecycle(initialValue = null)
            //half-opened fold with a horizontal hinge = tabletop posture; the hinge
            //bounds (window coordinates) let the layout keep content out of the crease
            val hinge = layoutInfo?.displayFeatures
                ?.filterIsInstance<FoldingFeature>()
                ?.firstOrNull { it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL }
                ?.let { HingeBounds(it.bounds.top, it.bounds.bottom) }
            LibreSpeedTheme(
                mode = when (themeMode) {
                    "light" -> ThemeMode.LIGHT
                    "dark" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            ) {
                App(windowWidth = windowSizeClass.widthSizeClass, hinge = hinge)
            }
        }
    }

}

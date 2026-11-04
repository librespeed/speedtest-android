package org.librespeed.speedtest.ui

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librespeed.speedtest.data.HistoryDatabase
import org.librespeed.speedtest.data.HistoryEntry
import org.librespeed.speedtest.ui.result.ResultScreen
import org.librespeed.speedtest.ui.settings.LicensesScreen
import org.librespeed.speedtest.ui.theme.LibreSpeedTheme
import org.librespeed.speedtest.ui.theme.ThemeMode
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import java.util.TimeZone

/**
 * Golden screenshots for the most tweaked screens; regenerate them with
 * ./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w448dp-h997dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    //small anti-aliasing differences between platforms must not fail the build
    private val options = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
    )

    @Before
    fun pinEnvironment() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private fun captureResultScreen(mode: ThemeMode, golden: String, waitFor: String = "DOWNLOAD") {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = HistoryDatabase(context).insert(
            HistoryEntry(
                date = 1_754_900_000_000,
                server = "Prague, Czech Republic (CESNET)",
                ping = 12.3,
                jitter = 1.4,
                download = 942.31,
                upload = 487.12,
                loss = 0.0,
                ipInfo = "203.0.113.7 - Example ISP",
                ipVersion = 4,
                shareUrl = "https://librespeed.example/results/?id=abc123",
                networkType = "Wi-Fi 6",
                downloadSamples = List(60) { 900.0 + (it % 7) * 12 },
                uploadSamples = List(60) { 460.0 + (it % 5) * 8 },
                durationMs = 21_400,
                mode = "standard",
                loadedDown = 18.0,
                loadedUp = 24.0
            )
        )
        compose.setContent {
            LibreSpeedTheme(mode = mode) {
                ResultScreen(entryId = id, onBack = {}, onTestAgain = {}, onTestDetails = {}, onShare = {})
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(waitFor).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().captureRoboImage(golden, roborazziOptions = options)
    }

    private fun captureSettingsScreen(mode: ThemeMode, golden: String) {
        compose.setContent {
            LibreSpeedTheme(mode = mode) {
                org.librespeed.speedtest.ui.settings.SettingsScreen(
                    serverLabel = "Prague, Czech Republic (CESNET)",
                    serverPinned = true,
                    serverCount = 12,
                    onServersClick = {},
                    onLicensesClick = {}
                )
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().captureRoboImage(golden, roborazziOptions = options)
    }

    @Test
    fun resultScreen() = captureResultScreen(ThemeMode.DARK, "src/test/snapshots/result_screen.png")

    //the light palette carries its own accent shades; a dark-only suite would
    //never catch a light-mode contrast regression
    @Test
    fun resultScreenLight() = captureResultScreen(ThemeMode.LIGHT, "src/test/snapshots/result_screen_light.png")

    //numbers follow the device locale; a comma-decimal locale golden keeps that honest
    @Test
    @Config(qualifiers = "+cs")
    fun resultScreenCzech() {
        Locale.setDefault(Locale.forLanguageTag("cs-CZ"))
        captureResultScreen(ThemeMode.DARK, "src/test/snapshots/result_screen_cs.png", waitFor = "STAHOVÁNÍ")
    }

    @Test
    fun settingsScreen() = captureSettingsScreen(ThemeMode.DARK, "src/test/snapshots/settings_screen.png")

    @Test
    fun settingsScreenLight() = captureSettingsScreen(ThemeMode.LIGHT, "src/test/snapshots/settings_screen_light.png")

    @Test
    fun licensesScreen() {
        compose.setContent {
            LibreSpeedTheme(mode = ThemeMode.DARK) {
                LicensesScreen(onBack = {})
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("LibreSpeed speedtest engine").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onRoot().captureRoboImage("src/test/snapshots/licenses_screen.png", roborazziOptions = options)
    }

}

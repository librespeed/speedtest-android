package org.librespeed.speedtest

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Launches the app and checks the main navigation renders on every supported API level. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationShowsAllTabs() {
        rule.onNodeWithText("Speedtest").assertExists()
        rule.onNodeWithText("History").assertExists()
        rule.onNodeWithText("Servers").assertExists()
        rule.onNodeWithText("Settings").assertExists()
    }

}

package org.librespeed.speedtest

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Configuration changes (rotation, theme, locale) recreate the activity; the UI must survive them. */
@RunWith(AndroidJUnit4::class)
class RecreationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityRecreationKeepsTheMainScreen() {
        rule.onNodeWithText("Speedtest").assertExists()
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        rule.onNodeWithText("Speedtest").assertExists()
        rule.onNodeWithText("Settings").assertExists()
    }

}

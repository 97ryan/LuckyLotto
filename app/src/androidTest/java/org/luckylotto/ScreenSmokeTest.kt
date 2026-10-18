// SPDX-License-Identifier: GPL-3.0-or-later
package org.luckylotto

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenSmokeTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    private fun screenshot(name: String) {
        screen.waitForIdle()
        val directory = File(checkNotNull(
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        ) { "Gradle additional test output directory is required for screenshot collection" })
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun appLaunchesAndMainScreensWork() {
        screen.onNodeWithText("LuckyLotto").assertIsDisplayed()
        screen.onNodeWithText("Weekly budget").assertIsDisplayed()
        screen.onNodeWithText("Prize target").assertIsDisplayed()
        screenshot("01-plan")
        screen.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Your daily plan"))
        screen.onNodeWithText("Your daily plan").assertIsDisplayed()
        screenshot("02-daily-plan")
        screen.onNodeWithText("Games", useUnmergedTree = true).performClick()
        screen.onNodeWithText("The games,\nby the numbers.").assertIsDisplayed()
        screenshot("03-games")
        screen.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        screen.onNodeWithText("Keep it simple.").assertIsDisplayed()
        screen.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Dark"))
        screen.onNodeWithText("Dark").performClick()
        screenshot("04-dark-settings")
    }

    @Test fun invalidBudgetDoesNotBecomeAPlan() {
        screen.onNode(hasSetTextAction() and hasText("Weekly budget")).performTextReplacement("600")
        closeSoftKeyboard()
        screen.onNodeWithText("Update my plan").performScrollTo().performClick()
        screen.onNodeWithText("Enter a budget from $0 to $500 and a target from $0.01 to $100 million (up to 2 decimals).")
            .assertIsDisplayed()
    }
}

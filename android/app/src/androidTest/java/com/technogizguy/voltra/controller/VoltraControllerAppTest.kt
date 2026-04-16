package com.technogizguy.voltra.controller

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class VoltraControllerAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun scanScreenIsTheFirstExperience() {
        composeRule.onNodeWithText("Voltra Controller").assertIsDisplayed()
        composeRule.onNodeWithText("Nearby Devices").assertIsDisplayed()
        composeRule.onNodeWithText("Scan with the VOLTRA awake and close by.").assertIsDisplayed()
    }
}

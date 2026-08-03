package com.fatihenes.photoreport.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.fatihenes.photoreport.MainActivity
import com.fatihenes.photoreport.R
import org.junit.Rule
import org.junit.Test

class MainUserFlowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testCreateProjectFlow() {
        // 1. Check if we are on Dashboard
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.acc_add_project)
        ).assertIsDisplayed()

        // 2. Click Add Project
        composeTestRule.onNodeWithContentDescription(
            composeTestRule.activity.getString(R.string.acc_add_project)
        ).performClick()

        // 3. Enter Name
        val testProjectName = "Field Test Alpha"
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.project_name_label)
        ).performTextInput(testProjectName)

        // 4. Confirm
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.start_project_btn)
        ).performClick()

        // 5. Verify Project appears in list
        composeTestRule.onNodeWithText(testProjectName).assertExists()
        
        // 6. Navigate to detail
        composeTestRule.onNodeWithText(testProjectName).performClick()
        
        // 7. Verify we see timeline header
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.timeline_label)
        ).assertIsDisplayed()
    }
}

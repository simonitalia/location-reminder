package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SaveReminderFragmentTest {

    private var fakeRepository: FakeAndroidTestRepository? = null

    @Before
    fun initRepository() {
        fakeRepository = FakeAndroidTestRepository()
    }

    @After
    fun cleanupDb() {
        fakeRepository = null
    }

    @Test
    fun noReminderTitleAndClickSaveFab_showSnackBarError() {

        //GIVEN - On Save Reminder Screen
        launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        // WHEN - Click on Save fab
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        //THEN - Verify "Please enter title" snackbar error message is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))
    }

    @Test
    fun noReminderLocationAndClickSaveFab_showSnackBarError() {

        //GIVEN - On Save Reminder Screen
        launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        // WHEN - Enter Title text, Enter Description text, Dismiss keyboard, Click on Save fab
        onView(withId(R.id.reminderTitle)).perform(typeText("Test Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Test Description"), closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        //THEN - Verify "Please select location" snackbar error message is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_select_location)))
    }
}
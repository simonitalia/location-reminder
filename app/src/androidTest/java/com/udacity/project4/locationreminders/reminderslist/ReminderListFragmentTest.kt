package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestRepository
import com.udacity.project4.utils.RemindersAndroidTestUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

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
    fun clickAddReminderFab_navigateToSaveReminderFragment() {

        // GIVEN - On the Reminder List screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val mockNavController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.requireView(), mockNavController)
        }

        // WHEN - Click on the "+" (fab) button
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that we navigate to the Save / Add Reminder Screen
        Mockito.verify(mockNavController).navigate(
            ReminderListFragmentDirections.actionReminderListFragmentToSaveReminderFragment()
        )
    }

    // execute as run blocking to ensure reminder is added to db before performing with ui test
    @Test
    fun savedReminder_checkReminderTextIsDisplayedInUi() = runBlockingTest {

        // GIVEN - Add ReminderDto to the DB
        val reminderDto = RemindersAndroidTestUtils.createMockReminderDto()
        fakeRepository?.saveReminder(reminderDto)

        //WHEN - On Reminder List Screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - Verify Reminder text displayed is correct
        onView(withId(R.id.remindersRecyclerView))

            //Check Title, Description, Location text
            hasDescendant(withText("Test Title"))
            hasDescendant(withText("Test Description"))
            hasDescendant(withText("Test Location"))
    }

    @Test
    fun emptyReminderList_showNoDataMessage() = runBlockingTest {

        //GIVEN - No Reminders saved in database
        fakeRepository?.deleteAllReminders()

        //WHEN - On Reminder List Screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        //Then - Verify No Data message is displayed
        onView(withId(R.id.noDataTextView))
            .check(matches(isDisplayed()))
    }
}
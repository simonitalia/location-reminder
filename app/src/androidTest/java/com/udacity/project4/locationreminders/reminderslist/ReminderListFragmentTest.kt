package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.RemindersRepositoryInterface
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.FakeAndroidTestRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepositoryTest
import com.udacity.project4.util.RemindersTestUtils
import kotlinx.android.synthetic.main.fragment_reminder_list.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private var fakeRepository: RemindersRepositoryInterface? = null

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
    fun savedReminder_DisplayedInUi() = runBlockingTest {

        // GIVEN - Add reminderDto to the DB
        val reminderDto = RemindersTestUtils.createMockReminderDto()
        fakeRepository?.saveReminder(reminderDto)

        val fetchedReminder = fakeRepository?.getReminder(reminderDto.id) as Result.Success
//        Log.i("", "${fetchedReminder.data.title}")

        // WHEN - ReminderListFragment launched to display reminder
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
//        scenario.onFragment {
//            val adapter = it.remindersRecyclerView.adapter
//            adapter?.getItemViewType(1)
//        }

        // THEN - Reminder is displayed on the screen with correct information
        onView(withText("Test Title"))
            .check(matches(isDisplayed()))
    }




//    TODO: add testing for the error messages.
}
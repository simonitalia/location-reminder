package com.udacity.project4.locationreminders

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.RemindersRepositoryInterface
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get


/**
 * END TO END test to black box test the app
 */

@RunWith(AndroidJUnit4::class)
@LargeTest

class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: RemindersRepositoryInterface
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                ReminderListViewModel(
                    appContext,
                    get() as RemindersRepositoryInterface
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as RemindersRepositoryInterface
                )
            }
            single { RemindersLocalRepository(get()) as RemindersRepositoryInterface }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    // End to End Testing

    @Test
    fun startActivity_checkAddReminderFlow() = runBlocking {

        // 0. Start Activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)

        /*
            1. Reminder List Screen
         */

        // Click Add Reminder
        onView(withId(R.id.addReminderFAB)).perform(click())

        /*
            2.Save Reminder Screen
         */

        // Perform add Title and add Description
        onView(withId(R.id.reminderTitle)).perform(replaceText("Reminder Title Test"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Reminder Description Test"))

        // Verify Reminder Title and Description displayed on screen
        onView(ViewMatchers.withText("Reminder Title Test")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("Reminder Description Test")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Click Select Location
        onView(withId(R.id.selectLocation)).perform(click())

        /*
            4. Select Location Screen
         */

        //Perform add marker on the map and Perform save location
        onView(withId(R.id.map)).perform(longClick())

        //Click on save location button
        onView(withId(R.id.saveLocationButton)).perform(click())

        /*
            5. Save Reminder Screen
         */

        // Click Save Button
        onView(withId(R.id.saveReminder)).perform(click())

        /*
            6. Reminder List Screen
         */

        // Verify Reminder Title and Description displayed on screen
        onView(ViewMatchers.withText("Reminder Title Test")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText("Reminder Description Test")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        //Close scenario
        activityScenario.close()
    }
}

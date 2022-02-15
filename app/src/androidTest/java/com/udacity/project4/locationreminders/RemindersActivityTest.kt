package com.udacity.project4.locationreminders

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.RemindersRepositoryInterface
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.DataBindingIdlingResource
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.monitorActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.junit.After
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
 * TESTING NOTES:
    * For best results, please perform test on an actual device, not on the simulator
    * Please ensure to be logged into app before executing this test
 */

@RunWith(AndroidJUnit4::class)
@LargeTest

class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private lateinit var repository: RemindersRepositoryInterface
    private lateinit var appContext: Application

    /*
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

    /*
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /*
     * Unregister Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    // End to End Testing

    @Test
    fun startActivity_checkAddReminderFlow() = runBlocking {

        // Start Activity Scenario
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        /*
             1. Reminder List Screen - Click "+" Add Reminder fab
         */

        onView(withId(R.id.addReminderFAB)).perform(click())

        /*
            2. Save Reminder Screen -
            * Perform add Title text
            * Perform add Description text
            * Verify Reminder Title text is displayed
            * Verify Description text is displayed
            * Perform Click Select Location
         */

        onView(withId(R.id.reminderTitle)).perform(replaceText("Reminder Title Test"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("Reminder Description Test"))
        onView(withText("Reminder Title Test")).check(matches(isDisplayed()))
        onView(withText("Reminder Description Test")).check(matches(isDisplayed()))
        onView(withId(R.id.selectLocation)).perform(click())

        /*
            3. Select Location Screen -
            * Perform add marker on the map
            * Perform save location
         */
        onView(withId(R.id.map)).perform(longClick())
        onView(withId(R.id.saveLocationButton)).perform(click())

        /*
            4. Save Reminder Screen - Perform Click Save Button
         */
        onView(withId(R.id.saveReminder)).perform(click())

        /*
            5. Reminder List Screen -
            * Verify "Reminder Saved !" Toast is displayed
            * Verify Reminder Title text is displayed
            * Verify Description text is displayed
            * Perform Logout
         */
        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not(getActivity(appContext)?.window?.decorView)))
            .check(matches(isDisplayed()))
        onView(withText("Reminder Title Test")).check(matches(isDisplayed()))
        onView(withText("Reminder Description Test")).check(matches(isDisplayed()))

        // Close scenario
        activityScenario.close()
    }
}

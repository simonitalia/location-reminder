package com.udacity.project4.locationreminders.reminderslist

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.viewModels
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.FakeTestRemindersRepository
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.RemindersTestUtil
import com.udacity.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.*
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.robolectric.annotation.Config
import java.security.AccessController.getContext

/**
 * Testing for ReminderListViewModel and its live data objects
 */

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [29]) // force use API 29 since API 30 not supported
class ReminderListViewModelTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake repository to be injected into the viewmodel
    private lateinit var repository: FakeTestRemindersRepository

    // Subject under test (sut)
    private lateinit var viewModel: ReminderListViewModel

    private lateinit var appContext: Application

    private lateinit var activityScenario: ActivityScenario<RemindersActivity>

    @ExperimentalCoroutinesApi
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // setup before running each test
    @Before
    fun init() {

        // get application context
        appContext = ApplicationProvider.getApplicationContext()

        // initialize repository
        repository = FakeTestRemindersRepository()

        activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
    }

    fun setupViewModel() = runBlocking {

        // initialise reminders repo with some reminders (3)
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())

        // initialize viewModel
        viewModel = ReminderListViewModel(appContext as MyApp, repository)
    }

    @After
    fun closeScenario() {
        activityScenario.close()
    }

    @Test
    fun loadReminders_showLoading() {

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        viewModel.loadReminders()

        // Then assert that the loading indicator value is set to true
        Assert.assertThat(
            viewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the loading indicator is value is set to false.
        Assert.assertThat(
            viewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(false)
        )
    }
}
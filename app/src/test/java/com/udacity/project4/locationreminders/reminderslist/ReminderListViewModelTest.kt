package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.FakeTestRemindersRepository
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.RemindersTestUtil
import com.udacity.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

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
    private val repository = FakeTestRemindersRepository()

    // Subject under test (sut)
    private lateinit var viewModel: ReminderListViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // setup before running each test
    @Before
    fun setup() = runBlocking {

        // initialize firebase app
        FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext)

        // initialize viewModel
        viewModel = ReminderListViewModel(MyApp(), repository)

        // initialise reminders repo with some reminders (3)
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
    }

    @After
    fun cleanupRepository() = runBlocking {
        repository.deleteAllReminders()
    }

    // Verify showLoading value is set to true when loading, then false after loading
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

    // Verify Snackbar error message value is triggered when loading reminders fails
    @Test
    fun loadReminders_showSnackBar() = runBlocking {

        //simulate error response
        repository.setReturnError(true)

        // trigger fetch reminders from repo
        viewModel.loadReminders()

        Assert.assertThat(viewModel.showSnackBar.getOrAwaitValue(), CoreMatchers.`is`("Test exception"))
    }




}
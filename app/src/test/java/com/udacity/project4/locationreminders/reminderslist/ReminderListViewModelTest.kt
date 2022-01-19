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
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Testing for ReminderListViewModel functions and live data objects
 */

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [29]) // force use API 29 since API 30 not supported
class ReminderListViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake repository to be injected into the viewmodel
    private val repository = FakeTestRemindersRepository()

    // Subject under test (sut)
    private lateinit var viewModel: ReminderListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // setup before running each test
    @Before
    fun setup() = runBlocking {

        // initialize firebase app
        FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext)

        // initialize viewModel
        viewModel = ReminderListViewModel(MyApp(), repository)
    }

    @After
    fun cleanup() = runBlocking {
        repository.deleteAllReminders()
        stopKoin() // ensure single instance fof Koin
    }

    // Verify showLoading value is set to true when loading, then false after loading
    @Test
    fun loadReminders_showLoading() = runBlocking {

        // GIVEN - initialise reminders repo with some reminders (3)
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // WHEN - Load the reminders in the view model.
        viewModel.loadReminders()

        // THEN - assert showLoading value
        Assert.assertThat(
            viewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )

        // GIVEN - Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // THEN - assert showLoading value
        Assert.assertThat(
            viewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(false)
        )
    }

    // Verify Snackbar error message value is triggered when loading reminders fails
    @Test
    fun loadRemindersWhenRemindersAreUnavailable_showSnackBar() = runBlocking {

        //GIVEN - Simulate repo error response
        repository.setReturnError(true)

        // WHEN - Load reminders
        viewModel.loadReminders()

        // THEN - assert showSnackBar value is set with error message
        Assert.assertThat(viewModel.showSnackBar.getOrAwaitValue(), CoreMatchers.`is`("Test exception"))
    }

    @Test
    fun loadRemindersWithNoReminders_showNoData() = runBlocking{

        // GIVEN - clear all reminders
        repository.deleteAllReminders()

        // WHEN - fetch reminders
        viewModel.loadReminders()

        //THEN - assert showNoData value
        Assert.assertThat(viewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(true))
    }

    @Test
    fun loadRemindersWithReminders_showNoData() = runBlocking{

        // GIVEN - initialise reminders repo with a reminders
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())

        // WHEN - fetch reminders
        viewModel.loadReminders()

        //THEN - assert showNoData value, and remindersList size
        Assert.assertThat(viewModel.showNoData.getOrAwaitValue(), CoreMatchers.`is`(false))
        Assert.assertThat(viewModel.reminderList.value?.size, CoreMatchers.`is`(1))
    }

}
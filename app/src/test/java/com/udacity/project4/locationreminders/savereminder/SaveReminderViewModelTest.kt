package com.udacity.project4.locationreminders.savereminder

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
 * Testing for SaveReminderView live data objects
 */

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [29]) // force use API 29 since API 30 not supported
class SaveReminderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake repository to be injected into the viewmodel
    private val repository = FakeTestRemindersRepository()

    // Subject under test (sut)
    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // setup before running each test
    @Before
    fun setup() = runBlocking {

        // initialize firebase app
        FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext)

        // initialize viewModel
        viewModel = SaveReminderViewModel(MyApp(), repository)
    }

    @After
    fun cleanup() = runBlocking {
        repository.deleteAllReminders()
        stopKoin() // ensure single instance fof Koin
    }

    @Test
    fun saveReminder_showLoading() = runBlocking{

        val reminder = RemindersTestUtil.createMockReminderDto().run {
             RemindersTestUtil.reminderDtoToReminder(this)
        }

        viewModel.reminderTitle.value = reminder.title
        viewModel.reminderDescription.value = reminder.description
        viewModel.reminderSelectedLocationStr.value = reminder.location
        viewModel.latitude.value = reminder.latitude
        viewModel.longitude.value = reminder.longitude
        viewModel.selectedPOI.value = RemindersTestUtil.createMockPOI()

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // WHEN - Load the reminders in the view model.
        viewModel.validateAndSaveReminder(reminder)

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









}
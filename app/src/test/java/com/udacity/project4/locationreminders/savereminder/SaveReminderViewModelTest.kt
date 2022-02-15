package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.FirebaseApp
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeTestRemindersRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
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

    // Use a mock instance of the app to pass into the viewModel constructor
    private lateinit var app: Application

    // Use a fake repository to be injected into the viewmodel
    private val repository = FakeTestRemindersRepository()

    // Subject under test (sut)
    private lateinit var viewModel: SaveReminderViewModel

    // Mock reminder
    private lateinit var reminder: ReminderDataItem

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // setup before running each test
    @Before
    fun setup() = runBlocking {

        app = ApplicationProvider.getApplicationContext()

        // initialize firebase app
        FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext)

        // initialize viewModel with mockApp and fake repo
        viewModel = SaveReminderViewModel(app, repository)

        // initialize mock reminder
        reminder = RemindersTestUtil.createMockReminderDto().run {
            RemindersTestUtil.reminderDtoToReminder(this)
        }

        // set view model live data values with reminder values
        viewModel.reminderTitle.value = reminder.title
        viewModel.reminderDescription.value = reminder.description
        viewModel.reminderSelectedLocationStr.value = reminder.location
        viewModel.latitude.value = reminder.latitude
        viewModel.longitude.value = reminder.longitude
        viewModel.selectedPOI.value = RemindersTestUtil.createMockPOI()
    }

    @After
    fun cleanup() = runBlocking {
        repository.deleteAllReminders()
        stopKoin() // ensure single instance fof Koin
    }

    @Test
    fun saveReminder_showLoading() = runBlocking{

        // GIVEN - New Reminder created and values assigned to view model

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // WHEN - Validate and Save the reminder
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

    @Test
    fun createReminderWithNullTitle_validateAndSaveReminder() {

        // GIVEN - Null title
        reminder.title = null

        // WHEN - Validate and Save the reminder
        viewModel.validateAndSaveReminder(reminder)

        //THEN - assert showSnackbarInt value
        Assert.assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            CoreMatchers.`is`(2131820603)
        )
    }

    @Test
    fun createReminderWithNullLocation_validateAndSaveReminder() {

        // GIVEN - Null location
        reminder.location = null

        // WHEN - Validate and Save the reminder
        viewModel.validateAndSaveReminder(reminder)

        //THEN - assert showSnackbarInt value
        Assert.assertThat(
            viewModel.showSnackBarInt.getOrAwaitValue(),
            CoreMatchers.`is`(2131820604)
        )
    }

    @Test
    fun saveReminder_showToast()  {

        // GIVEN - New Reminder created and values assigned to view model

        // WHEN - Validate and Save the reminder
        viewModel.validateAndSaveReminder(reminder)

        //THEN - assert showToast value
        Assert.assertThat(
            viewModel.showToast.value,
            CoreMatchers.`is`("Reminder Saved !")
        )
    }

    @Test
    fun saveReminder_navigateBack()  {

        // GIVEN - New Reminder created and values assigned to view model

        // WHEN - Validate and Save the reminder
        viewModel.validateAndSaveReminder(reminder)

        // THEN - assert navigationCommand value
        Assert.assertThat(
            viewModel.navigationCommand.value,
            CoreMatchers.`is`(NavigationCommand.Back)
        )
    }

    @Test
    fun createReminder_checkViewModelLiveDataValues() {

        // GIVEN - Reminder and POI

        // THEN - Validate view model properties are set to expected values
        Assert.assertThat(
            viewModel.reminderTitle.getOrAwaitValue(), CoreMatchers.`is`("Test Title")
        )
        Assert.assertThat(
            viewModel.reminderDescription.getOrAwaitValue(), CoreMatchers.`is`("Test Description")
        )
        Assert.assertThat(
            viewModel.reminderSelectedLocationStr.getOrAwaitValue(), CoreMatchers.`is`("Test Location")
        )
        Assert.assertThat(
            viewModel.latitude.getOrAwaitValue(), CoreMatchers.`is`(37.422160)
        )
        Assert.assertThat(
            viewModel.longitude.getOrAwaitValue(), CoreMatchers.`is`(-122.084270)
        )

        //check point of interest values
        Assert.assertThat(
            viewModel.selectedPOI.value!!.latLng, CoreMatchers.equalTo(LatLng(37.422160, -122.084270))
        )
        Assert.assertThat(
            viewModel.selectedPOI.value!!.name, CoreMatchers.equalTo("Test Name")
        )
        Assert.assertThat(
            viewModel.selectedPOI.value!!.placeId, CoreMatchers.equalTo("Test POI")
        )

        // WHEN - Clear view model live data values
        viewModel.onClear()

        // THEN - Validate view model properties are set to null

        Assert.assertThat(
            viewModel.reminderTitle.value, CoreMatchers.equalTo(null)
        )
        Assert.assertThat(
            viewModel.reminderDescription.getOrAwaitValue(), CoreMatchers.equalTo(null)
        )
        Assert.assertThat(
            viewModel.reminderSelectedLocationStr.getOrAwaitValue(), CoreMatchers.equalTo(null)
        )
        Assert.assertThat(
            viewModel.latitude.getOrAwaitValue(), CoreMatchers.equalTo(null)
        )
        Assert.assertThat(
            viewModel.longitude.getOrAwaitValue(), CoreMatchers.equalTo(null)
        )
        Assert.assertThat(
            viewModel.selectedPOI.getOrAwaitValue(), CoreMatchers.equalTo(null)
        )
    }
}
package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.MyApp
import com.udacity.project4.locationreminders.data.FakeTestRemindersRepository
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.RemindersTestUtil
import com.udacity.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testing for ReminderListViewModel and its live data objects
 */


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class ReminderListViewModelTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Use a fake repository to be injected into the viewmodel
    private lateinit var repository: FakeTestRemindersRepository

    // Subject under test (sut)
    private lateinit var viewModel: ReminderListViewModel

    @ExperimentalCoroutinesApi
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // setup before running each test
    @Before
    fun setupViewModel() = runBlocking {

        // initialize repository
        repository = FakeTestRemindersRepository()

        // initialise reminders repo with some reminders (3)
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())
        repository.saveReminder(RemindersTestUtil.createMockReminderDto())

        // initialize viewModel
        viewModel = ReminderListViewModel(MyApp(), repository)

    }



    @Test
    fun loadReminders_showLoading() {

        // Pause dispatcher so you can verify initial values.
        mainCoroutineRule.pauseDispatcher()

        // Load the reminders in the view model.
        viewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        Assert.assertThat(
            viewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )

        // Execute pending coroutines actions.
        mainCoroutineRule.resumeDispatcher()

        // Then assert that the progress indicator is hidden.
//        Assert.assertThat(
//            statisticsViewModel.dataLoading.getOrAwaitValue(),
//            CoreMatchers.`is`(false)
//        )
    }

}
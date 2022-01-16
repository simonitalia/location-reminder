package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeTestRemindersRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
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
}
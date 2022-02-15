package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.utils.RemindersAndroidTestUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.*
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.`is`

/**
 * Testing direct implementation of the RemindersLocalRepository.kt
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun init() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository =
            RemindersLocalRepository(
                database.remindersDao(),
                Dispatchers.Main
            )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminderDto_retrievesReminderDto() = runBlocking {

        // GIVEN - A new reminderDto saved in the database.
        val newReminderDto = RemindersAndroidTestUtil.createMockReminderDto()

        repository.saveReminder(newReminderDto)

        // WHEN - reminderDto retrieved by ID
        val result = repository.getReminder(newReminderDto.id) as Result.Success

        // THEN - Same reminderDto is returned.
        Assert.assertThat(result.succeeded, CoreMatchers.`is`(true))
        Assert.assertThat(result.data.title, CoreMatchers.`is`("Test Title"))
        Assert.assertThat(result.data.description, CoreMatchers.`is`("Test Description"))
        Assert.assertThat(result.data.location, CoreMatchers.`is`("Test Location"))
        Assert.assertThat(result.data.latitude, CoreMatchers.`is`(37.422160))
        Assert.assertThat(result.data.longitude, CoreMatchers.`is`(-122.084270))
    }

    @Test
    fun insertThreeRemindersAndGetAllReminders() = runBlocking {

        // GIVEN - Create and insert 3 mock reminderDTOs in the db.
        for (mock in 1..3) {
            RemindersAndroidTestUtil.createMockReminderDto().apply {
                database.remindersDao().saveReminder(this)
            }
        }

        // WHEN - Get all reminderDTOs from the database.
        val allReminders = repository.getReminders() as Result.Success

        // THEN - Check all loaded reminders are in the db and fetched from the db
        MatcherAssert.assertThat(allReminders.data.size, `is`(3))
    }

    @Test
    fun insertThreeRemindersAndDeleteAllReminders() = runBlocking {

        // GIVEN - Create and insert 3 mock reminderDTOs in the db.
        for (mock in 1..3) {
            RemindersAndroidTestUtil.createMockReminderDto().apply {
                database.remindersDao().saveReminder(this)
            }
        }

        // WHEN - No reminders are in the database, get all reminders
        database.remindersDao().deleteAllReminders()
        val result = repository.getReminders() as Result.Success

        // THEN - Check the database has no reminders
        MatcherAssert.assertThat(result.data.size, `is`(0))
    }

    @Test
    fun getReminderByIdWhenReminderDoesNotExist() = runBlocking {

        // GIVEN - A new reminderDto saved in the database
        val newReminderDto = RemindersAndroidTestUtil.createMockReminderDto()
        val reminderId = newReminderDto.id

        // WHEN - Reminder no longer exists, attempt to retrieve it
        database.remindersDao().deleteAllReminders()
        val result = repository.getReminder(reminderId) as Result.Error

        // THEN - Check the
        MatcherAssert.assertThat(result.message, `is`("Reminder not found!"))
    }
}
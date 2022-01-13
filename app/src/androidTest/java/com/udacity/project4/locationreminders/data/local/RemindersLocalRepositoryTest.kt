package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.util.RemindersTestUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.*
import org.junit.runner.RunWith
import java.util.*
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
    fun setup() {
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
        val newReminderDto = ReminderDTO(
            "Test Title",
            "Test Description",
            "Test Location",
            37.422160,
            -122.084270,
            id = UUID.randomUUID().toString()
        )

        repository.saveReminder(newReminderDto)

        // WHEN  - reminderDto retrieved by ID
        val result = repository.getReminder(newReminderDto.id)

        // THEN - Same reminderDto is returned.
        Assert.assertThat(result.succeeded, CoreMatchers.`is`(true))
        result as Result.Success
        Assert.assertThat(result.data.title, CoreMatchers.`is`("Test Title"))
        Assert.assertThat(result.data.description, CoreMatchers.`is`("Test Description"))
        Assert.assertThat(result.data.location, CoreMatchers.`is`("Test Location"))
        Assert.assertThat(result.data.latitude, CoreMatchers.`is`(37.422160))
        Assert.assertThat(result.data.longitude, CoreMatchers.`is`(-122.084270))
    }


    @Test
    fun insertThreeRemindersAndGetAllReminders() = runBlockingTest {

        // GIVEN
        // Create and insert 3 mock reminderDTOs in the db.
        for (mock in 1..3) {
            RemindersTestUtils.createMockReminderDto().apply {
                database.remindersDao().saveReminder(this)
            }
        }

        // WHEN - Get all reminderDTOs from the database.
        val allReminders = database.remindersDao().getReminders()

        // THEN - Check all loaded reminders are in the db and fetched from the db
        MatcherAssert.assertThat(allReminders.size, `is`(3))
    }

    @Test
    fun insertThreeRemindersAndDeleteAllReminders() = runBlockingTest {

        // GIVEN
        // Create and insert 3 mock reminderDTOs in the db.
        for (mock in 1..3) {
            RemindersTestUtils.createMockReminderDto().apply {
                database.remindersDao().saveReminder(this)
            }
        }

        // Delete all reminderDTOs from the database.
        database.remindersDao().deleteAllReminders()

        // WHEN - Get no reminders are in the db
        val result = database.remindersDao().getReminders()

        // THEN - Check the database has no reminders
        MatcherAssert.assertThat(result.size, `is`(0))
    }
}
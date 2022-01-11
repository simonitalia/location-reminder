package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test
import java.util.*

/**
 * This implementation tests the the RemindersDatabase:
 * 1) ReminderDTO objects are successfully saved to the database
 * 2) ReminderDTO objects are successfully fetched as ReminderDataItem objects
 */

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // build database instance
    private lateinit var database: RemindersDatabase

    // setup db
    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder( // completely deleted once the process runs so the db doesn't persist on disk, unlike in production
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()


    @Test
    fun insertReminderAndGetById() = runBlockingTest {

        // GIVEN
        // Create and insert a mock reminderDTO in the db.
        val reminder = ReminderDTO(
            "Test Title",
            "Test Description",
            "Test Location",
            37.422160,
            -122.084270,
            id = UUID.randomUUID().toString()
        )

        // add reminder to db
        database.remindersDao().saveReminder(reminder)

        // WHEN - Get the reminderDTO by id from the database.
        val reminderDTO = database.remindersDao().getReminderById(reminder.id)

        // THEN - Check the loaded data contains the expected values.
        assertThat<ReminderDataItem>(reminderDTO as ReminderDataItem, notNullValue())
        assertThat(reminderDTO.id, `is`(reminder.id))
        assertThat(reminderDTO.title, `is`(reminder.title))
        assertThat(reminderDTO.description, `is`(reminder.description))
        assertThat(reminderDTO.longitude, `is`(reminder.longitude))
        assertThat(reminderDTO.latitude, `is`(reminder.latitude))
    }
}
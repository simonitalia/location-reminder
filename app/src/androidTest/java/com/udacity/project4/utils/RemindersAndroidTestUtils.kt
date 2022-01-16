package com.udacity.project4.utils

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import java.util.*

object RemindersAndroidTestUtils {

    fun createMockReminderDto() : ReminderDTO {
        return ReminderDTO(
            "Test Title",
            "Test Description",
            "Test Location",
            37.422160,
            -122.084270,
            id = UUID.randomUUID().toString()
        )
    }
}
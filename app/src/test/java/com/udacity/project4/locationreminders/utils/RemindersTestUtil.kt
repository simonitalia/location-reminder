package com.udacity.project4.locationreminders.utils

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import java.util.*

object RemindersTestUtil {

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

    fun createMockPOI() : PointOfInterest {
        return PointOfInterest(LatLng(37.422160, -122.084270),"Test POI", "Googleplex")

    }

    fun reminderDtoToReminder(reminderDTO: ReminderDTO): ReminderDataItem {

        return ReminderDataItem(
            reminderDTO.title,
            reminderDTO.description,
            reminderDTO.location,
            reminderDTO.latitude,
            reminderDTO.longitude,
            reminderDTO.id
        )
    }
}
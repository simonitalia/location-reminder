package com.udacity.project4.locationreminders.reminderslist

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import java.io.Serializable
import java.util.*

/**
 * data class acts as a data mapper between the DB and the UI
 */
data class ReminderDataItem(
    var title: String?,
    var description: String?,
    var location: String?,
    var latitude: Double?,
    var longitude: Double?,
    val id: String = UUID.randomUUID().toString()
) : Serializable

/**
 *  of Reminder Database object to Reminder DataModel object
 */
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
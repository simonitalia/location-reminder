package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch
import java.util.*

class SaveReminderViewModel(
    val app: Application,
    private val dataSource: ReminderDataSource
): BaseViewModel(app)
{
    val reminderTitle = MutableLiveData<String>()
    val reminderDescription = MutableLiveData<String>()
    val reminderSelectedLocationStr = MutableLiveData<String>()
    val selectedPOI = MutableLiveData<PointOfInterest>()
    val latitude = MutableLiveData<Double>()
    val longitude = MutableLiveData<Double>()

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    fun setLocationAsString(latLng: LatLng): String {
        return String.format(
            Locale.getDefault(),
            "Lat: %1$.5f, Long: %2$.5f",
            latLng.latitude,
            latLng.longitude)
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(

                // save data as ReminderDTO object
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)

            //trigger navigation
            navigateBack()
        }
    }

    fun navigateBack() {
        navigationCommand.value = NavigationCommand.Back
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    private fun validateEnteredData(reminderData: ReminderDataItem): Boolean {

        // check user has added a reminder title
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        // check user has added a reminder location
        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }

        return true
    }
}
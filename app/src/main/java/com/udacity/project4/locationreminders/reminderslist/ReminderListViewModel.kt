package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.udacity.project4.authentication.FirebaseUserLiveData
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.locationreminders.data.RemindersRepositoryInterface
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.launch

class ReminderListViewModel(
    app: Application,
    private val repository: RemindersRepositoryInterface
) : BaseViewModel(app) {

    companion object {
        const val SIGN_IN_RESULT_CODE = 1001 // Sign in response result code
    }

    /**
     * FirebaseUI Authentication
     */
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    //authenticationState variable based off the FirebaseUserLiveData object
    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED

        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    /**
     * Get all the reminders from the DataSource and add them to the remindersList to be shown on the UI,
     * or show error if any
     */

    // list that holds the reminder data to be displayed on the UI
    val reminderList = MutableLiveData<List<ReminderDataItem>>()

    fun loadReminders() {
        showLoading.value = true
        viewModelScope.launch {
            //interacting with the dataSource has to be through a coroutine
            val result = repository.getReminders()
            showLoading.postValue(false)
            when (result) {
                is Result.Success<*> -> {
                    val dataList = ArrayList<ReminderDataItem>()
                    dataList.addAll((result.data as List<ReminderDTO>).map { reminder ->
                        //map the reminder data from the DB to the be ready to be displayed on the UI
                        ReminderDataItem(
                            reminder.title,
                            reminder.description,
                            reminder.location,
                            reminder.latitude,
                            reminder.longitude,
                            reminder.id
                        )
                    })
                    reminderList.value = dataList
                }
                is Result.Error ->
                    showSnackBar.value = result.message
            }

            //check if no data has to be shown
            invalidateShowNoData()
        }
    }

    /**
     * Inform the user that there's not any data if the remindersList is empty
     */
    private fun invalidateShowNoData() {
        showNoData.value = reminderList.value == null || reminderList.value!!.isEmpty()
    }
}
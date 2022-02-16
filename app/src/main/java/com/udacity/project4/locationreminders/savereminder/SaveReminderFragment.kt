package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.viewbinding.BuildConfig
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.ReminderGeofence
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    companion object {
        private val TAG = SaveReminderFragment::class.java.simpleName

        // background & foreground location tracking permissions
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private var reminder: ReminderDataItem? = null

    // Geofencing
    private lateinit var reminderGeofence: ReminderGeofence
    private val isDeviceRunningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_save_reminder,
            container,
            false
        )

        binding.viewModel = _viewModel

        // instantiate geofencing reminder
        reminderGeofence = ReminderGeofence(requireContext())

        setDisplayHomeAsUpEnabled(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        //  Navigate to another fragment to get the user location
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            checkPermissionsAndAddGeofence()
        }

        /*
            view model observers
         */

        _viewModel.showSnackBar.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    message,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        _viewModel.showErrorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage != null) {
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    errorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
     * 1. Check Geo fencing, foreground and background location permissions
     */

    private fun checkPermissionsAndAddGeofence() {
        if (isForegroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()

        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  2. Checks if the app has the appropriate permissions
     *  Check across Android 10+ and all other Android versions.
     */
    @TargetApi(29) // approveForegroundAndBackgroundLocation() & authorizedLocation()
    private fun isForegroundAndBackgroundLocationPermissionApproved() : Boolean {

        // check that the foreground and background permissions were approved
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION))

        val backgroundPermissionApproved =
            if (isDeviceRunningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireActivity(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )

            // pre Q versions don't require requesting background permissions
            } else {
                true
            }

        // return true if both permissions are granted
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
     *  3a. Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */

    private fun checkDeviceLocationSettingsAndStartGeofence() {

        // check that the device's location is on
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        // check if the client location settings are satisfied
        val settingsClient = LocationServices.getSettingsClient(requireActivity())

        // create a location response that acts as a listener for the device location if enabled
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }

            } else {
                val snackbar = Snackbar.make(
                    binding.fragmentSaveReminder,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                )
                snackbar.setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence() // will rerun permission request
                }
                snackbar.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                saveReminderAndAddGeofence()
            }
        }
    }

    /*
     *  3b. Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {

        // request foreground and background permissions
        if (isForegroundAndBackgroundLocationPermissionApproved()) return // return if permissions already granted

        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) // array which contains permissions requested

        val resultCode = when {
            isDeviceRunningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }

            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Requesting foreground only location permission")
        requestPermissions( // this calls onRequestPermissionsResult()
            permissionsArray,
            resultCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        // handle the result of the user's permission
        Log.d(TAG, "onRequestPermissionResult")

        // check grantResults array for necessary permissions
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        )

        // display a snackbar message if any required permission is not granted
        {
            Snackbar.make(
                binding.fragmentSaveReminder,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.LIBRARY_PACKAGE_NAME, null) // replace APPLICATION_ID with LIBRARY_PACKAGE_NAME
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show() // ends flow

        // add fence if all permissions have been granted
        } else {
            saveReminderAndAddGeofence()
        }
    }

    // 4. save reminder, add the Geofence
    private fun saveReminderAndAddGeofence() {

        // create reminder data item
        reminder = ReminderDataItem(
            _viewModel.reminderTitle.value,
            _viewModel.reminderDescription.value,
            _viewModel.reminderSelectedLocationStr.value,
            _viewModel.latitude.value,
            _viewModel.longitude.value
        )

        reminder?.let { reminder ->

            // save reminder and trigger navigation
            _viewModel.validateAndSaveReminder(reminder)

            reminderGeofence.addGeofence(
                reminder,
                success = {
                    _viewModel.showSnackBar.value = activity?.getString(R.string.geofence_added)
                    Log.i(TAG, "Geofence added")

                },

                failure = { errorMessage ->
                    _viewModel.showErrorMessage.value =
                        activity?.getString(R.string.error_adding_geofence)
                    Log.e(TAG, "Geofence error: $errorMessage")
                }
            )
        }
    }

    //removing a geofence
    private fun removeGeofence(reminder: ReminderDataItem) {
        reminderGeofence.removeGeofence(
            reminder,
            success = {
                Log.i(TAG, "Geofences removed")
            },
            failure = { errorMessage ->
                Log.e(TAG, "Geofence error: $errorMessage")
            }
        )
    }
}

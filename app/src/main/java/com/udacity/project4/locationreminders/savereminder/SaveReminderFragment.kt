package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
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

            // create reminder object
            reminder = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            ).apply {

                // add geofence, if reminder is valid
                if (_viewModel.validateEnteredData(this)) {
                    saveReminderAndAddGeofence()
                }
            }
        }

        /*
            view model observers
         */

        _viewModel.showToast.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(
                    context,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            //reset toast value
            _viewModel.showToast.value = null
        }

        _viewModel.showSnackBar.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Snackbar.make(
                    binding.fragmentSaveReminder,
                    message,
                    Snackbar.LENGTH_LONG
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
     * Create Geofence
     */

    private fun saveReminderAndAddGeofence() {

        // 1. check permissions before adding Geofence
        if (!isForegroundAndBackgroundLocationPermissionApproved()) {
            requestForegroundAndBackgroundLocationPermissions()
            return
        }

        // 2. if all permissions are granted, check location settings
        checkDeviceLocationSettings(
            onLocationSettingsResponseTask = {

                // 3a. If location services enabled
                if (it.isSuccessful) {
                    this.reminder?.let { reminder ->

                        // Add Geofence
                        reminderGeofence.addGeofence(
                            reminder,
                            //.addOnSuccessListener callback
                            success = {
                                val message = "Geofence added"
                                _viewModel.showToast.value = message
                                Log.i(TAG, message)

                                // save reminder to db (triggers navigation)
                                _viewModel.validateAndSaveReminder(reminder)
                            },
                            //.addOnFailureListener callback
                            failure = { errorMessage ->
                                val message =
                                    "Error adding Geofence: $errorMessage"
                                _viewModel.showSnackBar.value = message
                                Log.e(TAG, "Geofence error: $errorMessage")
                            }
                        )
                    }

                // 3. if location services not enabled, display error
                } else {
                    val snackbar = Snackbar.make(
                        binding.fragmentSaveReminder,
                        R.string.location_required_error,
                        Snackbar.LENGTH_INDEFINITE
                    )
                    snackbar.setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                        })
                    }
                    snackbar.show()
                }
            }
        )
    }

    /*
     * Location permissions
     */

    // Check across Android 10+ (Q) and all other Android versions.
    @TargetApi(29)
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

    // Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        val resultCode = when {
            isDeviceRunningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }

            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        Log.d(TAG, "Request foreground only location permission")
        requestPermissions( // will call onRequestPermissionsResult()
            permissionsArray,
            resultCode
        )
    }

    // on response of request permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        // handle the result of the user's permission
        Log.d(TAG, ".onRequestPermissionResult")

        // check grantResults array for necessary permissions
        if (grantResults.isEmpty() ||
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
                        action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    })
                }.show() // ends flow

        // if all permissions have been granted, addGeofence
        } else {
            Log.i(TAG, "All permissions now granted")
            saveReminderAndAddGeofence()
        }
    }

    private fun checkDeviceLocationSettings(
        onLocationSettingsResponseTask: (Task<LocationSettingsResponse>) -> Unit
    ) {

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
                    Snackbar.LENGTH_LONG
                )
                snackbar.setAction("TRY AGAIN") {
                    checkDeviceLocationSettings(
                        onLocationSettingsResponseTask = {
                            saveReminderAndAddGeofence()
                        }
                    )
                }
                snackbar.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            onLocationSettingsResponseTask(it)
        }
    }
}

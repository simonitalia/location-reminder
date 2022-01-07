package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.viewbinding.BuildConfig
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName

        // map
        private const val DEFAULT_ZOOM_LEVEL = 18f // zoom level for map

        // geofence
        private const val GEOFENCE_RADIUS_IN_METERS = 100f
        const val ACTION_GEOFENCE_EVENT =
            "SelectLocationFragment.reminder.action.ACTION_GEOFENCE_EVENT"

        // background & foreground location tracking permissions
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val REQUEST_LOCATION_PERMISSION = 1 // location tracking permission
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    // Google map properties
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    // Geofencing
    private lateinit var geofencingClient: GeofencingClient
    private val isDeviceRunningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = SelectLocationFragment.ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        // configure current location service
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // instantiate the geofencing client
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        // must declare this inside onCreateView()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment? // adds Google Map to the app
        mapFragment?.getMapAsync(this)

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0

        // map styling
        setMapStyle(map)

        // save selected location or POI
        setMapMarkerOnLongClick(map)
        setPoiClick(map)

        // set zoom and camera position to user's current location

        // Test Location (Google HQ)
        val lat = 37.422160
        val lon = -122.084270
        val place = LatLng(lat, lon)
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, DEFAULT_ZOOM_LEVEL))

        // set map to user's current location
        moveCameraToDeviceLocation(map)

        // enable location tracking
        enableMyLocation()
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        // Check if location permissions are granted and if so enable the
//        // location data layer.
//        if (requestCode == REQUEST_LOCATION_PERMISSION) {
//            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                enableMyLocation()
//            }
//        }
//    }

    /*
     * requesting location permission callback
     * In all cases, we need to have the location permission.
     * On Android 10+ (Q) we need to have the background permission as well.
     */

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
                binding.fragmentSelectLocationMap,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.LIBRARY_PACKAGE_NAME, null) // replace APPLICATION_ID with LIBRARY_PACKAGE_NAME
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()

        // start geo fence if all permissions have been granted
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // map styling
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this.activity,
                    R.raw.map_style // file name of .json file created and stored in /res/raw
                )
            )

            // if styling is unsuccessful
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }

            // if map_style file can;t be loaded, catch error
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    // add marker on user long click
    private fun setMapMarkerOnLongClick(map: GoogleMap) {

        //latLng captured / set on map long press
        map.setOnMapLongClickListener { latLng ->

            // Add Snippet text displayed below the title
            val snippet = _viewModel.setLocationAsString(latLng)

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)

                    // marker style
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            // update view model and trigger navigation
            onLocationSelected(latLng, snippet, null)
        }
    }

    // On click of map point of Interest (POI), add a marker
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            // include marker window (call out shown above marker)
            poiMarker.showInfoWindow()

            // update view model and trigger navigation
            val descr = _viewModel.setLocationAsString(poi.latLng)
            onLocationSelected(poi.latLng, descr, poi)
        }
    }

    /*
        * Get the best and most recent location of the device, which may be null in rare
        * cases when a location is not available.
     */
    private fun moveCameraToDeviceLocation(map: GoogleMap) {

        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(context as Activity) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude), DEFAULT_ZOOM_LEVEL))
                    }
                } else {
                    Log.i(TAG, "Current location is null. Using defaults.")
                    Log.i(TAG, "Exception: ${task.exception}")
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.6892, 51.3890), 15.toFloat()))
                    map.uiSettings?.isMyLocationButtonEnabled = false
                }
            }
        } catch (e: SecurityException) {
            Log.i(TAG, "Exception: ${e.message}")
        }
    }

    /**
     * Enable map location
     */

    // map location permission
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            map.isMyLocationEnabled = true

        } else {

            // ask for permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    /**
     * Geo fencing
     */

    //specify the geofence to monitor and the initial trigger
    private fun seekGeofencing(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(_viewModel.geofenceList)
        }.build()
    }

    //adding a geofence
    // addGeoFence()
    private fun addGeofenceForReminder(){
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        geofencingClient?.addGeofences(seekGeofencing(), geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(requireContext(), "Geofences added", Toast.LENGTH_SHORT).show()
            }
            addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add geofences", Toast.LENGTH_SHORT).show()

            }
        }
    }

    //removing a geofence
    private fun removeGeofence(){
        geofencingClient?.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(requireContext(), "Geofences removed", Toast.LENGTH_SHORT).show()

            }
            addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to remove geofences", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29) // approveForegroundAndBackgroundLocation() & authorizedLocation()
    private fun isForegroundAndBackgroundLocationPermissionApproved(): Boolean {

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
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 ) // askLocationPermission() {
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

        Log.d(TAG, "Request foreground only location permission")
        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    /*
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */

    // examinePermisionAndinitiatGeofence()
    private fun checkPermissionsAndStartGeofencing() {
        if (isForegroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */

    // validateGadgetAreaInitiateGeofence()
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve:Boolean = true) {

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
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }

            } else {
                Snackbar.make(
                    binding.fragmentSelectLocationMap,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                addGeofenceForReminder()
            }
        }
    }

    // update viewModel and navigation
    private fun onLocationSelected(latLng: LatLng, locationString: String, selectedPOI: PointOfInterest?) {

        // update viewModel properties
        _viewModel.reminderSelectedLocationStr.value =  locationString
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.latitude
        _viewModel.selectedPOI.value = selectedPOI

        // navigate to reminder list
        _viewModel.navigateBack()

        // add geofence
        _viewModel.geofenceList.add(Geofence.Builder()
            .setRequestId("entry.key")
            .setCircularRegion(latLng.latitude, latLng.latitude, GEOFENCE_RADIUS_IN_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build())
    }
}

package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
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
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val TAG = SelectLocationFragment::class.java.simpleName

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    // Google map properties
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val REQUEST_LOCATION_PERMISSION = 1 // location tracking permission
    private val DEFAULT_ZOOM_LEVEL = 18f // zoom level for map

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        // must declare this inside onCreateView()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment? // adds Google Map to the app
        mapFragment?.getMapAsync(this)

        // configure current location service
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0

        // enable location tracking
        enableMyLocation()

        // map styling
        setMapStyle(map)

        setMapMarkerOnLongClick(map)

        // set zoom and camera position to user's current location

        // Test Location (Google HQ)
        val lat = 37.422160
        val lon = -122.084270
        val place = LatLng(lat, lon)
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, DEFAULT_ZOOM_LEVEL))

        // set map to user's current location
        moveCameraToDeviceLocation(map)
    }

    // requesting location permission callback
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
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

    // location tracking

    // permission
    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }

    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            map.setMyLocationEnabled(true)

        } else {

            // ask for permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
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
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)

                    // marker style
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
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

    private fun onLocationSelected() {
//        TODO: When the user confirms on the selected location,
//         send back the selected location details to the view model
//         and navigate back to the previous fragment to save the reminder and add the geofence
    }


}

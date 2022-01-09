package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.io.IOException

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName

        // map
        private const val DEFAULT_ZOOM_LEVEL = 12f // zoom level for map
        private const val PLACE_PICKER_REQUEST_CODE = 3

        // background & foreground location tracking permissions
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1 // location tracking permission
        private const val PERMISSION_CODE = 101
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    // Google map properties
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    // selectedLocation
    private var reminderSelectedLocationStr: String? = null
    private var latitude: Double? = null
    private var  longitude: Double? = null
    private var selectedPOI: PointOfInterest? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        // configure current location service
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        // must declare this inside onCreateView()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment? // adds Google Map to the app
        mapFragment?.getMapAsync(this)

        // on click listener for location search fab
        binding.locationSearchFloatingActionButton.setOnClickListener {
            loadPlacePicker()
        }

        // enable map location search
        searchLocation(binding.mapLocationSearchView)

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0

        // map styling
        setMapStyle(map)

        // save selected location or POI
        setMapMarkerOnLongClick(map)
        setPoiClick(map)

        // enable location tracking
        enableMyLocation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == PLACE_PICKER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val place = PlacePicker.getPlace(requireContext(), data)
                var addressText = place.name.toString()
                addressText += "\n" + place.address.toString()

                placeMarkerOnMap(place.latLng)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if location permissions are granted and if so enable the
        // location data layer.
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
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
//            confirmLocationSelected(latLng, snippet, null)
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
//            confirmLocationSelected(poi.latLng, descr, poi)
        }
    }

    /**
        * Get the best and most recent location of the device, which may be null in rare
        * cases when a location is not available.
     */
    private fun moveCameraToDeviceLocation(map: GoogleMap) {

        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude), DEFAULT_ZOOM_LEVEL))
                    }
                } else {
                    Log.i(TAG, "Current location is null. Using defaults.")
                    Log.i(TAG, "Exception: ${task.exception}")
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.6892, 51.3890), 15.toFloat()))
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
            map.uiSettings.isMapToolbarEnabled = true

            // set zoom and camera position to user's current location

            // Test Location (Google HQ)
            val lat = 37.422160
            val lon = -122.084270
            val place = LatLng(lat, lon)
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, DEFAULT_ZOOM_LEVEL))

            // move map to user's current location
            moveCameraToDeviceLocation(map)

        } else {

            // ask for permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    // map location search
    private fun loadPlacePicker() {
        val builder = PlacePicker.IntentBuilder()

        try {
            startActivityForResult(builder.build(requireActivity()), PLACE_PICKER_REQUEST_CODE)

        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()

        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    private fun placeMarkerOnMap(location: LatLng) {
        val markerOptions = MarkerOptions().position(location)

        val titleStr = getAddress(location)  // add these two lines
        markerOptions.title(titleStr)

        map.addMarker(markerOptions)
    }

    // resolve search location address
    private fun getAddress(latLng: LatLng): String {
        // 1
        val geocoder = Geocoder(requireContext())
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, e.localizedMessage)
        }

        return addressText
    }

    // support search by location on map
    private fun searchLocation(view: SearchView) {

        view.setOnQueryTextListener(object: SearchView.OnQueryTextListener {

            // on kb search icon tapped
            override fun onQueryTextSubmit(query: String?): Boolean {

                query?.let { searchViewQueryText ->
                    var addressList: List<Address>? = null
                    val geoCoder = Geocoder(requireContext())

                    try {
                        addressList = geoCoder.getFromLocationName(searchViewQueryText, 1)

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    addressList?.firstOrNull()?.let { address ->
                        val latLng = LatLng(address.latitude, address.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLng(latLng))

                    //on no address found
                    }.run {
                        Toast.makeText(requireContext(), "no location found", Toast.LENGTH_SHORT).show()
                    }

                //on empty search text
                }.run {
                    Toast.makeText(requireContext(), "provide location", Toast.LENGTH_SHORT).show()
                }

                return false
            }

            // on search text input changed
            override fun onQueryTextChange(newText: String?): Boolean {
                // TODO: Not implemented
                return false
            }
        })
    }

    // selected location confirmation alert
    private fun confirmLocationSelected(latLng: LatLng, locationString: String, selectedPOI: PointOfInterest?) {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Confirm Location")
        alertDialog.setMessage("Save this location:\n $locationString?")
        alertDialog.setPositiveButton("Yes") { dialog, id ->
            val intent  = Intent()
            startActivity(intent)

            //set location de
            onLocationSelected(latLng, locationString, selectedPOI)
        }
        alertDialog.setNegativeButton("Cancel") { dialog, id ->
            dialog.cancel()
        }

        val alert = alertDialog.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
        val negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE)
        negativeButton.setTextColor(Color.RED)
    }

    // update viewModel and navigation
    private fun onLocationSelected(latLng: LatLng, locationString: String, selectedPOI: PointOfInterest?) {

        // update viewModel properties
        _viewModel.reminderSelectedLocationStr.value =  locationString
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        _viewModel.selectedPOI.value = selectedPOI

        // navigate to reminder list
        _viewModel.navigateBack()
    }
}

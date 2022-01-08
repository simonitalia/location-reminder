package com.udacity.project4.locationreminders.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.GeofenceErrorMessages

class ReminderGeofence(private val context: Context) {

    companion object {
        private const val GEOFENCE_RADIUS_IN_METERS = 100f
    }

    val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
    }

    // create geofence for reminder location
    fun addGeofence(
        reminder: ReminderDataItem,
        success: () -> Unit,
        failure: (error: String) -> Unit
    ) {
        // 1 build geofence
        val geofence = buildGeofence(reminder)
        if (geofence != null &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {

            // 2 build geofence request
            geofencingClient
                .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)

                 // build geofence request listener
                .addOnSuccessListener {
                    // 3 on success
                    success()
                }
                .addOnFailureListener { error ->
                    // 4 on failure
                    failure(GeofenceErrorMessages.getErrorString(context, error))
                }
        }
    }

    // build geofence for reminder location
    private fun buildGeofence(reminder: ReminderDataItem): Geofence? {
        val latitude = reminder.latitude
        val longitude = reminder.longitude
        val radius = GEOFENCE_RADIUS_IN_METERS

        if (latitude != null && longitude != null) {
            return Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                    latitude,
                    longitude,
                    radius
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        return null
    }

    // set the geofence trigger
    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(listOf(geofence))
            .build()
    }

    fun removeGeofence(
        reminder: ReminderDataItem,
           success: () -> Unit,
           failure: (error: String) -> Unit
    ) {
        geofencingClient
            .removeGeofences(listOf(reminder.id))
            .addOnSuccessListener {
                success()
            }
            .addOnFailureListener {
                failure(GeofenceErrorMessages.getErrorString(context, it))
            }
    }
}



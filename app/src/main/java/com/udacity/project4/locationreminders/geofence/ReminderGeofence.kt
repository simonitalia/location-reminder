package com.udacity.project4.locationreminders.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.udacity.project4.utils.GeofenceErrorMessages
import java.util.*

data class ReminderGeofence(
    val id: String = UUID.randomUUID().toString(),
    var latLng: LatLng,
    var radiusInMeters: Double = 100.0
)

class ReminderGeofenceRepository(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "ReminderGeofenceRepository"
        private const val REMINDERS_GEOFENCES = "REMINDERS_GEOFENCES"
    }

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun add(reminderGeofence: ReminderGeofence,
            success: () -> Unit,
            failure: (error: String) -> Unit) {
        // 1
        val geofence = buildGeofence(reminderGeofence)
        if (geofence != null
            && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 2
            geofencingClient
                .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                .addOnSuccessListener {
                    // 3
                    saveAll(getAll() + reminderGeofence)
                    success()
                }
                .addOnFailureListener { error ->
                    // 4
                    failure(GeofenceErrorMessages.getErrorString(context, error))
                }
        }
    }

    private fun buildGeofence(reminderGeofence: ReminderGeofence): Geofence? {
        val latitude = reminderGeofence.latLng?.latitude
        val longitude = reminderGeofence.latLng?.longitude
        val radius = reminderGeofence.radiusInMeters

        if (latitude != null && longitude != null && radius != null) {
            return Geofence.Builder()
                .setRequestId(reminderGeofence.id)
                .setCircularRegion(
                    latitude,
                    longitude,
                    radius.toFloat()
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }

        return null
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(listOf(geofence))
            .build()
    }

    fun remove(reminderGeofence: ReminderGeofence,
               success: () -> Unit,
               failure: (error: String) -> Unit) {
        geofencingClient
            .removeGeofences(listOf(reminderGeofence.id))
            .addOnSuccessListener {
                saveAll(getAll() - reminderGeofence)
                success()
            }
            .addOnFailureListener {
                failure(GeofenceErrorMessages.getErrorString(context, it))
            }
    }

    private fun saveAll(list: List<ReminderGeofence>) {
        preferences
            .edit()
            .putString(REMINDERS_GEOFENCES, gson.toJson(list))
            .apply()
    }

    fun getAll(): List<ReminderGeofence> {
        if (preferences.contains(REMINDERS_GEOFENCES)) {
            val remindersString = preferences.getString(REMINDERS_GEOFENCES, null)
            val arrayOfReminders = gson.fromJson(remindersString,
                Array<ReminderGeofence>::class.java)
            if (arrayOfReminders != null) {
                return arrayOfReminders.toList()
            }
        }
        return listOf()
    }

    fun get(requestId: String?) = getAll().firstOrNull { it.id == requestId }

    fun getLast() = getAll().lastOrNull()
}



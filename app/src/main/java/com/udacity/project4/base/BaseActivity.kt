package com.udacity.project4.base

import androidx.appcompat.app.AppCompatActivity
import com.udacity.project4.MyApp

abstract class BaseActivity : AppCompatActivity() {
    fun getReminderGeofenceRepo() = (application as MyApp).getReminderGeofenceRepo()
}
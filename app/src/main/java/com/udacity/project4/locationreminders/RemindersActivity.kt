package com.udacity.project4.locationreminders

import android.os.Bundle
import android.view.MenuItem
import com.udacity.project4.R
import com.udacity.project4.base.BaseActivity

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

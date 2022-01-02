package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.android.synthetic.main.activity_authentication.*

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        const val TAG = "AuthenticationActivity"
        const val SIGN_IN_RESULT_CODE = 1001
    }

    // Get a reference to the ViewModel scoped to this Fragment.
    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        login_button.setOnClickListener {
            launchSignInFlow()
        }

        // get user auth state
        viewModel.authenticationState.observe( this,  { authenticationState ->
            Log.i(TAG,"User authentication state: $authenticationState")

            // set login button visibility
            login_button.isVisible = authenticationState == AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED

            when (authenticationState) {

                //authenticated, show reminders list and print log
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {

                    Log.i(TAG,"User ${FirebaseAuth.getInstance().currentUser?.displayName} already signed in!")
                    //navController.navigate(R.id.reminderListFragment)

                    val intent = Intent(this, RemindersActivity::class.java)
                    startActivity(intent)
                }

                //not authenticated
                else -> {
                    Log.i(TAG, "User is signed out")
                }
            }
        })
    }

    // shows log-in option
    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
            // provide more ways for users to register and sign in here
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            SIGN_IN_RESULT_CODE
        )
    }
}

package com.udacity.project4.locationreminders.reminderslist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentReminderListBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {

    companion object {
        const val TAG = "ReminderListFragment"
        const val SIGN_IN_RESULT_CODE = 1001 // Sign in response result code
    }

    //use Koin to retrieve the ViewModel instance
    override val _viewModel: ReminderListViewModel by viewModel()
    private lateinit var binding: FragmentReminderListBinding
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminder_list, container, false
            )

        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        //trigger UI update on Auth state change
        onAuthenticationStateChanged()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()

        binding.lifecycleOwner = this
        setupRecyclerView()

        binding.addReminderFAB.setOnClickListener {
            navigateToSaveReminderFragment()
        }

        // If the user presses the back button, bring them back to sign in screen
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            signUserOut()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == SIGN_IN_RESULT_CODE) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode != Activity.RESULT_OK) {
                // Sign in failed.
                // check response.getError().getErrorCode()
                Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                signUserOut()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

    private fun onAuthenticationStateChanged() {
        _viewModel.authenticationState.observe(viewLifecycleOwner,  { authenticationState ->
            when (authenticationState) {

                // user authenticated
                ReminderListViewModel.AuthenticationState.AUTHENTICATED ->
                    Log.i(TAG,"User '${FirebaseAuth.getInstance().currentUser?.displayName}' signed in!")

                // not authenticated, launch sign in flow
                else -> {
                    Log.i(TAG, "User is signed out!")
                    launchSignInFlow()
                }
            }
        })
    }

    private fun navigateToSaveReminderFragment() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.actionReminderListFragmentToSaveReminderFragment()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {
        }

        // setup the recycler view using the extension function
        binding.remindersRecyclerView.setup(adapter)
    }

    // show log-in flow
    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
            // provide more ways for users to register and sign in here
        )

        // Custom login layout
        val customLayout = AuthMethodPickerLayout
            .Builder(R.layout.firebaseui_authentication)
            .setGoogleButtonId(R.id.sign_in_with_google_button)
            .setEmailButtonId(R.id.sign_in_with_email_button)
            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(customLayout)
                .setAvailableProviders(providers)
                .build(),
            AuthenticationActivity.SIGN_IN_RESULT_CODE
        )
    }

    private fun signUserOut() {
        AuthUI.getInstance().signOut(requireContext())
    }
}

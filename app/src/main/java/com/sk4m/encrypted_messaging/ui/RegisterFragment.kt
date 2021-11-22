package com.sk4m.encrypted_messaging.ui

import android.app.ActionBar
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.sk4m.encrypted_messaging.R
import com.sk4m.encrypted_messaging.SessionHolder
import com.sk4m.encrypted_messaging.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.Matrix
import java.util.regex.Pattern

class RegisterFragment : Fragment() {

    private var _views: FragmentRegisterBinding? = null
    private val views get() = _views!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentRegisterBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.RegisterButton.setOnClickListener {
            launchRegProcess()
        }
    }

    private fun launchRegProcess() {
        val username = views.usernameField.text.toString().trim().lowercase()
        val password = views.passwordField.text.toString().trim()
        val confirmPassword = views.confirmPasswordField.text.toString().trim()
        val homeserver = views.homeserverField.text.toString().trim()

        val passwordREGEX = Pattern.compile("^" +
                "(?=.*[0-9])" +         //at least 1 digit
                "(?=.*[A-Z])" +         //at least 1 upper case letter
                "(?=.*[a-zA-Z])" +      //any letter
                "(?=.*[@#$%^&+=!:;,])" +    //at least 1 special character
                "(?=\\S+$)" +           //no white spaces
                ".{8,}" +               //at least 8 characters
                "$");

        if(passwordREGEX.matcher(password).matches()) {
            if (password.equals(confirmPassword)) {
                val homeServerConnectionConfig = try {
                    HomeServerConnectionConfig
                        .Builder()
                        .withAllowHttpConnection(true)
                        .forceUsageOfTlsVersions(false)
                        .withHomeServerUri(Uri.parse(homeserver))
                        .build()
                } catch (failure: Throwable) {
                    Toast.makeText(requireContext(), "Home server is not valid", Toast.LENGTH_LONG)
                        .show()
                    return
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        Matrix.getInstance(requireContext()).authenticationService()
                            .getLoginFlow(homeServerConnectionConfig)
                        Matrix.getInstance(requireContext()).authenticationService()
                            .getRegistrationWizard()
                    } catch (failure: Throwable) {
                        Toast.makeText(requireContext(), "Failure 1: $failure", Toast.LENGTH_LONG)
                            .show()
                        Log.wtf("Error registration", failure)
                        null
                    }?.let { registrationWizard ->
                        try {
                            registrationWizard.registrationAvailable(username)
                            registrationWizard.createAccount(
                                username,
                                password,
                                "Encrypted-messaging"
                            )
                            registrationWizard.dummy()
                            Log.wtf(
                                "Log",
                                registrationWizard.createAccount(
                                    username,
                                    password,
                                    "Encrypted-messaging"
                                ).toString()
                            )
                        } catch (failure: Throwable) {
//                            Toast.makeText(
//                                requireContext(),
//                                "Failure 2: $failure",
//                                Toast.LENGTH_LONG
//                            )
//                                .show()
                            Log.wtf("Error register", failure)
                            null
                        }
                        try {
                            Matrix.getInstance(requireContext()).authenticationService()
                                .directAuthentication(
                                    homeServerConnectionConfig,
                                    username,
                                    password,
                                    null.toString() //"Encrypted-messaging:"+ Settings.Secure.getString( requireContext().contentResolver , Settings.Secure.ANDROID_ID)
                                )
                        } catch (failure: Throwable) {
                            Toast.makeText(
                                requireContext(),
                                "Failure 3: $failure",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            Log.wtf("Error login", failure)
                            null
                        }?.let { session ->
                            Toast.makeText(
                                requireContext(),
                                "Welcome ${session.myUserId}",
                                Toast.LENGTH_LONG
                            ).show()
                            SessionHolder.currentSession = session
                            session.open()
                            session.startSync(true)
                            session.startAutomaticBackgroundSync(60,30)
                            displayRoomList()
                            Toast.makeText(requireContext(), "Connected", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Confirm password don't match", Toast.LENGTH_LONG)
                    .show()
            }
        }
        else{
            Toast.makeText(requireContext(), "Password need 8 characters, 1 digit, 1 upper case letter, 1 special char", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun displayRoomList() {
        val fragment = RoomListFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment).commit()
    }
}
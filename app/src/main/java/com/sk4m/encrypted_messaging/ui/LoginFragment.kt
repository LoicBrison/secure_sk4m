package com.sk4m.encrypted_messaging.ui

import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.sk4m.encrypted_messaging.R
import com.sk4m.encrypted_messaging.SessionHolder
import com.sk4m.encrypted_messaging.ui.RegisterFragment
import com.sk4m.encrypted_messaging.databinding.FragmentLoginBinding
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.securestorage.KeySigner
import org.matrix.android.sdk.api.session.securestorage.SsssKeyCreationInfo
import org.matrix.android.sdk.api.session.securestorage.SsssKeySpec
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupAuthData
import org.matrix.android.sdk.internal.crypto.keysbackup.model.MegolmBackupCreationInfo
import org.matrix.android.sdk.internal.crypto.keysbackup.model.rest.KeysVersion


class LoginFragment : Fragment() {

    private var _views: FragmentLoginBinding? = null
    private val views get() = _views!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentLoginBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.loginButton.setOnClickListener {
            launchAuthProcess()
        }
        views.RegisterButton.setOnClickListener {
            displayRegisterForm()
        }
    }

    private fun launchAuthProcess() {
        val username = views.usernameField.text.toString().trim()
        val password = views.passwordField.text.toString().trim()
        val homeserver = views.homeserverField.text.toString().trim()

        val homeServerConnectionConfig = try {
            HomeServerConnectionConfig
                .Builder()
                .withHomeServerUri(Uri.parse(homeserver))
                .build()
        } catch (failure: Throwable) {
            Toast.makeText(requireContext(), "Home server is not valid", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Matrix.getInstance(requireContext()).authenticationService().directAuthentication(
                    homeServerConnectionConfig,
                    username,
                    password,
                    "Encrypted-messaging:"+ Settings.Secure.getString( requireContext().contentResolver , Settings.Secure.ANDROID_ID)
                )
            } catch (failure: Throwable) {
                Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                null
            }?.let { session ->

                Toast.makeText(
                    requireContext(),
                    "Welcome ${session.myUserId}",
                    Toast.LENGTH_SHORT
                ).show()
                SessionHolder.currentSession = session
                session.open()
                session.startSync(true)
                if(!session.cryptoService().keysBackupService().isEnabled) {
                    session.cryptoService().keysBackupService().createKeysBackupVersion(
                        MegolmBackupCreationInfo(
                            "m.megolm_backup.v1.curve25519-aes-sha2",
                            MegolmBackupAuthData(password),
                            session.sharedSecretStorageService.generateKey(
                                "m.megolm_backup.v1",
                                null,
                                "m.megolm_backup.v1",
                                null
                            ).recoveryKey
                        ),
                        NoOpMatrixCallback<KeysVersion>()
                    )
                }
                session.startAutomaticBackgroundSync(60,30)
                displayRoomList()
                Toast.makeText(requireContext(), "Connected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayRegisterForm(){
        val fragment = RegisterFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment).commit()
    }

    private fun displayRoomList() {
        val fragment = RoomListFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment).commit()
    }
}
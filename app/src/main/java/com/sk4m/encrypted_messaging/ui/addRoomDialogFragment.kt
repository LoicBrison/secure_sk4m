package com.sk4m.encrypted_messaging.ui

import android.app.Dialog
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.sk4m.encrypted_messaging.R
import com.sk4m.encrypted_messaging.SessionHolder
import com.sk4m.encrypted_messaging.databinding.FragmentLoginBinding
import com.sk4m.encrypted_messaging.databinding.FragmentRoomListBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.room.model.create.CreateRoomParams

class addRoomDialogFragment() : DialogFragment() {

    private val session = SessionHolder.currentSession!!


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(requireContext())

            val editText = EditText(requireContext());
            editText.hint = "Username";

            builder.setTitle("Create room")
            builder.setView(editText)
            builder.setPositiveButton("Créer",
                    DialogInterface.OnClickListener { dialog, id ->
                        GlobalScope.launch { 	// creates a new coroutine and continues
                            creatDirectRoom(editText.text.toString())			// suspending function
                        }
                    })
            builder.setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_room_dialog, container, false)
    }

    private suspend fun creatDirectRoom(username: String){
        val user = "@$username:matrix.sk4m.com"
        session.createDirectRoom(user)
    }

}
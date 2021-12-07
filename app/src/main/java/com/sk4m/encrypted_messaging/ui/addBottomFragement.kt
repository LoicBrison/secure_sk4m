package com.sk4m.encrypted_messaging.ui

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sk4m.encrypted_messaging.R
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.Nullable
import com.sk4m.encrypted_messaging.SessionHolder
import com.sk4m.encrypted_messaging.databinding.FragmentBottomSheetBinding
import com.sk4m.encrypted_messaging.databinding.FragmentLoginBinding
import com.sk4m.encrypted_messaging.databinding.FragmentRoomListBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class addBottomFragement(roomId: String) : BottomSheetDialogFragment() {

    private val id = roomId
    private val session = SessionHolder.currentSession!!
    private var _views: FragmentBottomSheetBinding? = null
    private val views get() = _views!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentBottomSheetBinding.inflate(inflater, container, false)
        return views.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.dialogBtnRemove.setOnClickListener{
            GlobalScope.launch {
                leaveRoom(id)
            }
        }
    }

    private suspend fun leaveRoom(roomId: String){
        session.getRoom(roomId)?.leave()
        getDialog()?.cancel()
    }
}
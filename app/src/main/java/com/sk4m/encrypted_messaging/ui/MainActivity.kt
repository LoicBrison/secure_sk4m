package com.sk4m.encrypted_messaging.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.sk4m.encrypted_messaging.R
import com.sk4m.encrypted_messaging.SessionHolder
import org.matrix.android.sdk.api.Matrix

class MainActivity : AppCompatActivity() {

    private lateinit var matrix: Matrix

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.colorAccent)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        matrix = Matrix.getInstance(this)
        if (savedInstanceState == null) {
            if (SessionHolder.currentSession != null) {
                displayRoomList()
            } else {
                displayLogin()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun displayLogin() {
        val fragment = LoginFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

    private fun displayRoomList() {
        val fragment = RoomListFragment()
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }

}
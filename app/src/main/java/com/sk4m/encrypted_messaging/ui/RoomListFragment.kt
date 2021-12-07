package com.sk4m.encrypted_messaging.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.sk4m.encrypted_messaging.R
import com.sk4m.encrypted_messaging.SessionHolder
import com.sk4m.encrypted_messaging.data.RoomSummaryDialogWrapper
import com.sk4m.encrypted_messaging.databinding.FragmentRoomListBinding
import com.sk4m.encrypted_messaging.formatter.RoomListDateFormatter
import com.sk4m.encrypted_messaging.utils.AvatarRenderer
import com.sk4m.encrypted_messaging.utils.MatrixItemColorProvider
import com.stfalcon.chatkit.commons.ImageLoader
import com.stfalcon.chatkit.dialogs.DialogsListAdapter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.session.SessionLifecycleObserver
import org.matrix.android.sdk.api.session.room.RoomService
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import org.matrix.android.sdk.api.util.toMatrixItem
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberEventHandler_Factory
import java.lang.reflect.Member


class RoomListFragment : Fragment(), ToolbarConfigurable {

    private val session = SessionHolder.currentSession!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _views = FragmentRoomListBinding.inflate(inflater, container, false)
        return views.root
    }

    private var _views: FragmentRoomListBinding? = null
    private val views get() = _views!!

    private val avatarRenderer by lazy {
        AvatarRenderer(MatrixItemColorProvider(requireContext()))
    }

    private val imageLoader = ImageLoader { imageView, url, _ ->
        avatarRenderer.render(url, imageView)
    }
    private val roomAdapter = DialogsListAdapter<RoomSummaryDialogWrapper>(imageLoader)
    private val addDialog = addRoomDialogFragment()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configureToolbar(views.toolbar, displayBack = false)

        views.roomSummaryList.setAdapter(roomAdapter)
        roomAdapter.setDatesFormatter(RoomListDateFormatter())
        roomAdapter.setOnDialogClickListener {
            showRoomDetail(it.roomSummary)
        }
        roomAdapter.setOnDialogLongClickListener {
            showBottomSheet(it.roomSummary)
        }

        views.fab.setOnClickListener { view ->
            addDialog.show(requireActivity().supportFragmentManager,null.toString())
        }



        // Create query to listen to room summary list
        val roomSummariesQuery = roomSummaryQueryParams {
            memberships = Membership.activeMemberships()
        }
        // Then you can subscribe to livedata..
        session.getRoomSummariesLive(roomSummariesQuery).observe(viewLifecycleOwner) {
            // ... And refresh your adapter with the list. It will be automatically updated when an item of the list is changed.
            updateRoomList(it)
        }

        // You can also listen to user. Here we listen to ourself to get our avatar
        session.getUserLive(session.myUserId).observe(viewLifecycleOwner) { user ->
            val userMatrixItem = user.map { it.toMatrixItem() }.getOrNull() ?: return@observe
            avatarRenderer.render(userMatrixItem, views.toolbarAvatarImageView)
        }

        val roomQueryParams = roomSummaryQueryParams {
            memberships = listOf(Membership.INVITE)
        }

        session.getRoomSummariesLive(roomQueryParams).observe(viewLifecycleOwner) {
            invitedRooms -> invitedRooms.map {  GlobalScope.launch { 	// creates a new coroutine and continues
                joinInvitedRooms(it.roomId)			// suspending function
            }}
        }

        setHasOptionsMenu(true)
    }

    private suspend fun joinInvitedRooms(roomId: String) {
        session.joinRoom(roomId, null, emptyList())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        lifecycleScope.launch {
            try {
                displayLoginForm()
                session.signOut(true)
                session.stopAnyBackgroundSync()
            } catch (failure: Throwable) {
                activity?.let {
                    Toast.makeText(it, "Failure: $failure", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            SessionHolder.currentSession = null
            //activity?.finish()
        }
    }

    private fun showBottomSheet(roomSummary: RoomSummary){
        val addBottomFragement =  addBottomFragement(roomSummary.roomId)
        addBottomFragement.show(
            (activity as MainActivity).supportFragmentManager,
            "fragment_bottom_sheet"
        )
    }

    private fun showRoomDetail(roomSummary: RoomSummary) {
        val roomDetailFragment = RoomDetailFragment.newInstance(roomSummary.roomId)
        (activity as MainActivity).supportFragmentManager
            .beginTransaction()
            .addToBackStack(null)
            .replace(R.id.fragmentContainer, roomDetailFragment)
            .commit()
    }

    private fun updateRoomList(roomSummaryList: List<RoomSummary>?) {
        if (roomSummaryList == null) return
        val sortedRoomSummaryList = roomSummaryList.sortedByDescending {
            it.latestPreviewableEvent?.root?.originServerTs
        }.map {
            RoomSummaryDialogWrapper(it)
        }
        roomAdapter.setItems(sortedRoomSummaryList)
    }

    private fun displayLoginForm(){
        val fragment = LoginFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment).commit()
    }

}
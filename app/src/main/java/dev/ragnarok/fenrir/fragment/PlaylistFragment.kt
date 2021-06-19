package dev.ragnarok.fenrir.fragment

import android.Manifest
import android.app.Dialog
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.ragnarok.fenrir.Extra
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.adapter.AudioRecyclerAdapter
import dev.ragnarok.fenrir.listener.BackPressCallback
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.place.PlaceFactory
import dev.ragnarok.fenrir.player.MusicPlaybackController
import dev.ragnarok.fenrir.player.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppPerms
import dev.ragnarok.fenrir.util.CustomToast.Companion.CreateCustomToast
import dev.ragnarok.fenrir.util.Utils

class PlaylistFragment : BottomSheetDialogFragment(), AudioRecyclerAdapter.ClickListener,
    BackPressCallback {
    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: AudioRecyclerAdapter? = null
    private val mData: ArrayList<Audio> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mData.clear()
        val tmp: List<Audio>? = requireArguments().getParcelableArrayList(Extra.AUDIOS)
        if (!Utils.isEmpty(tmp)) {
            if (tmp != null) {
                mData.addAll(tmp)
            }
        }
    }

    private fun getAudioPos(audio: Audio): Int {
        if (mData.isNotEmpty()) {
            for ((pos, i) in mData.withIndex()) {
                if (i.id == audio.id && i.ownerId == audio.ownerId) {
                    i.isAnimationNow = true
                    mAdapter?.notifyDataSetChanged()
                    return pos
                }
            }
        }
        return -1
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireActivity(), theme)
        val behavior = dialog.behavior
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_playlist, container, false)
        mRecyclerView = root.findViewById(R.id.list)
        val manager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        mRecyclerView?.layoutManager = manager
        val Goto: FloatingActionButton = root.findViewById(R.id.goto_button)
        Goto.setOnLongClickListener {
            val curr = MusicPlaybackController.getCurrentAudio()
            if (curr != null) {
                PlaceFactory.getPlayerPlace(Settings.get().accounts().current)
                    .tryOpenWith(requireActivity())
            } else CreateCustomToast(requireActivity()).showToastError(R.string.null_audio)
            false
        }
        Goto.setOnClickListener {
            val curr = MusicPlaybackController.getCurrentAudio()
            if (curr != null) {
                val index = getAudioPos(curr)
                if (index >= 0) {
                    mRecyclerView?.scrollToPosition(index)
                } else CreateCustomToast(requireActivity()).showToast(R.string.audio_not_found)
            } else CreateCustomToast(requireActivity()).showToastError(R.string.null_audio)
        }
        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(mRecyclerView)
        return root
    }

    private var simpleItemTouchCallback: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                viewHolder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                mAdapter?.notifyItemChanged(viewHolder.bindingAdapterPosition)
                mAdapter?.let { it1 ->
                    startForPlayList(
                        requireActivity(),
                        mData,
                        it1.getItemRawPosition(viewHolder.bindingAdapterPosition),
                        false
                    )
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAdapter = AudioRecyclerAdapter(requireActivity(), mData, false, false, 0, null)
        mAdapter?.setClickListener(this)
        mRecyclerView?.adapter = mAdapter
        val my = MusicPlaybackController.getCurrentAudio()
        if (my != null) {
            var index = 0
            var o = 0
            for (i in mData) {
                if (i === my) {
                    index = o
                    break
                }
                o++
            }
            mRecyclerView?.scrollToPosition(index)
        }
    }

    override fun onClick(position: Int, catalog: Int, audio: Audio) {
        if (MusicPlaybackController.getQueue() == mData) {
            MusicPlaybackController.skip(position)
        } else {
            startForPlayList(requireActivity(), mData, position, false)
        }
    }

    override fun onEdit(position: Int, audio: Audio?) {

    }

    override fun onDelete(position: Int) {

    }

    override fun onUrlPhotoOpen(url: String, prefix: String, photo_prefix: String) {
        PlaceFactory.getSingleURLPhotoPlace(url, prefix, photo_prefix)
            .tryOpenWith(requireActivity())
    }

    private val requestWritePermission = AppPerms.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    ) { CreateCustomToast(requireActivity()).showToast(R.string.permission_all_granted_text) }

    override fun onRequestWritePermissions() {
        requestWritePermission.launch()
    }

    override fun onBackPressed(): Boolean {
        return true
    }

    companion object {
        fun newInstance(playlist: ArrayList<Audio>?): PlaylistFragment {
            val bundle = Bundle()
            bundle.putParcelableArrayList(Extra.AUDIOS, playlist)
            val fragment = PlaylistFragment()
            fragment.arguments = bundle
            return fragment
        }
    }
}

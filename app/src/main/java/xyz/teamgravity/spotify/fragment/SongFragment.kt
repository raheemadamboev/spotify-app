package xyz.teamgravity.spotify.fragment

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import xyz.teamgravity.spotify.R
import xyz.teamgravity.spotify.databinding.FragmentSongBinding
import xyz.teamgravity.spotify.helper.util.Status
import xyz.teamgravity.spotify.helper.extension.isPlaying
import xyz.teamgravity.spotify.helper.extension.toSong
import xyz.teamgravity.spotify.model.SongModel
import xyz.teamgravity.spotify.viewmodel.MainViewModel
import xyz.teamgravity.spotify.viewmodel.SongViewModel
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var glide: RequestManager

    @Inject
    lateinit var minuteFormatter: SimpleDateFormat

    private val songViewModel by viewModels<SongViewModel>()

    private lateinit var mainViewModel: MainViewModel

    private var curPlayingSong: SongModel? = null
    private var playbackState: PlaybackStateCompat? = null

    private var shouldUpdateSeekBar = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSongBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lateInIt()
        subscribeToObservers()
        button()
    }

    private fun lateInIt() {
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if (curPlayingSong == null && songs.isNotEmpty()) {
                                curPlayingSong = songs[0]
                                updateTitleAndSongImage(songs[0])
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.curPlayingSong.observe(viewLifecycleOwner) {
            if (it == null) return@observe

            curPlayingSong = it.toSong()
            updateTitleAndSongImage(curPlayingSong!!)
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            if (isAdded) {
                binding.apply {
                    playPauseB.setImageResource(if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)
                    seekBar.progress = it?.position?.toInt() ?: 0
                }
            }
        }

        songViewModel.curPlayerPosition.observe(viewLifecycleOwner) {
            if (isAdded) {
                binding.apply {
                    if (shouldUpdateSeekBar) {
                        seekBar.progress = it.toInt()
                        setCurrentPlayerTimeToTextView(it)
                    }
                }
            }
        }

        songViewModel.curSongDuration.observe(viewLifecycleOwner) {
            if (isAdded) {
                binding.apply {
                    seekBar.max = it.toInt()
                    durationT.text = minuteFormatter.format(it)
                }
            }
        }
    }

    private fun updateTitleAndSongImage(song: SongModel) {
        val title = "${song.name} - ${song.songWriter}"

        binding.apply {
            nameT.text = title
            glide.load(song.imageUrl).into(imageI)
        }
    }

    private fun setCurrentPlayerTimeToTextView(ms: Long) {
        binding.timeT.text = minuteFormatter.format(ms)
    }

    private fun button() {
        onPlayPause()
        onNext()
        onPrevious()
        onSeekBar()
    }

    // play pause button
    private fun onPlayPause() {
        binding.playPauseB.setOnClickListener {
            curPlayingSong?.let { song ->
                mainViewModel.playOrToggleSong(song, true)
            }
        }
    }

    // next button
    private fun onNext() {
        binding.nextB.setOnClickListener {
            mainViewModel.skipToNextSong()
        }
    }

    // previous button
    private fun onPrevious() {
        binding.previousB.setOnClickListener {
            mainViewModel.skipToPreviousSong()
        }
    }

    // seek bar change
    private fun onSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setCurrentPlayerTimeToTextView(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekBar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mainViewModel.seekTo(it.progress.toLong())
                    shouldUpdateSeekBar = true
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
package xyz.teamgravity.spotify.activity

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import xyz.teamgravity.spotify.NavGraphDirections
import xyz.teamgravity.spotify.R
import xyz.teamgravity.spotify.databinding.ActivityMainBinding
import xyz.teamgravity.spotify.helper.adapter.SwipeSongAdapter
import xyz.teamgravity.spotify.helper.util.Status
import xyz.teamgravity.spotify.helper.extension.isPlaying
import xyz.teamgravity.spotify.helper.extension.toSong
import xyz.teamgravity.spotify.model.SongModel
import xyz.teamgravity.spotify.viewmodel.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var adapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var navController: NavController

    private var curPlayingSong: SongModel? = null
    private var playbackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController()
        viewPager()
        subscribeToObservers()
        button()
    }

    private fun navController() {
        navController = (supportFragmentManager.findFragmentById(R.id.fragment_container) as NavHostFragment).findNavController()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.songFragment -> hideBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun viewPager() {
        binding.apply {
            songViewPager.adapter = adapter

            // song click
            adapter.setOnItemClickListener {
                navController.navigate(NavGraphDirections.actionGlobalSongFragment())
            }
        }
    }

    private fun subscribeToObservers() {
        binding.apply {

            // all song for view pager to know
            viewModel.mediaItems.observe(this@MainActivity) {
                it?.let { result ->
                    when (result.status) {
                        Status.SUCCESS -> {
                            result.data?.let { songs ->
                                adapter.songs = songs

                                // first load new first song
                                if (songs.isNotEmpty()) {
                                    glide.load((curPlayingSong ?: songs[0]).imageUrl).into(imageI)
                                }
                                switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
                            }
                        }

                        Status.ERROR -> Unit
                        Status.LOADING -> Unit
                    }
                }
            }

            // current playing song
            viewModel.curPlayingSong.observe(this@MainActivity) {
                if (it == null) return@observe

                curPlayingSong = it.toSong()
                glide.load(curPlayingSong?.imageUrl).into(imageI)
                switchViewPagerToCurrentSong(curPlayingSong ?: return@observe)
            }

            // playing or not
            viewModel.playbackState.observe(this@MainActivity) {
                playbackState = it
                playPauseB.setImageResource(if (playbackState?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)
            }

            // is server connection error?
            viewModel.isConnected.observe(this@MainActivity) {
                it?.getContentIfNotHandled()?.let { result ->
                    when (result.status) {
                        Status.ERROR ->
                            Snackbar.make(root, result.message ?: "Unknown error happened", Snackbar.LENGTH_LONG).show()
                        else -> Unit
                    }
                }
            }

            // network error?
            viewModel.networkError.observe(this@MainActivity) {
                it?.getContentIfNotHandled()?.let { result ->
                    when (result.status) {
                        Status.ERROR ->
                            Snackbar.make(root, result.message ?: "Unknown error happened", Snackbar.LENGTH_LONG).show()
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun button() {
        onPlayPause()
    }

    private fun switchViewPagerToCurrentSong(song: SongModel) {
        binding.apply {
            val newItemIndex = adapter.songs.indexOf(song)

            if (newItemIndex != -1) {
                songViewPager.currentItem = newItemIndex
                curPlayingSong = song
            }

            // play music when viewpager is changed
            songViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (playbackState?.isPlaying == true) {
                        viewModel.playOrToggleSong(adapter.songs[position])
                    } else {
                        curPlayingSong = adapter.songs[position]
                    }
                }
            })
        }
    }

    private fun hideBottomBar() {
        binding.apply {
            imageI.visibility = View.GONE
            songViewPager.visibility = View.GONE
            playPauseB.visibility = View.GONE
        }
    }

    private fun showBottomBar() {
        binding.apply {
            imageI.visibility = View.VISIBLE
            songViewPager.visibility = View.VISIBLE
            playPauseB.visibility = View.VISIBLE
        }
    }

    // play pause button
    private fun onPlayPause() {
        binding.playPauseB.setOnClickListener {
            curPlayingSong?.let { viewModel.playOrToggleSong(it, true) }
        }
    }
}
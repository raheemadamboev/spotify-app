package xyz.teamgravity.spotify.viewmodel

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.teamgravity.spotify.helper.extension.isPlayEnabled
import xyz.teamgravity.spotify.helper.extension.isPlaying
import xyz.teamgravity.spotify.helper.extension.isPrepared
import xyz.teamgravity.spotify.helper.util.MusicServiceConnection
import xyz.teamgravity.spotify.helper.util.Resource
import xyz.teamgravity.spotify.model.SongModel
import xyz.teamgravity.spotify.service.MusicService
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _mediaItems = MutableLiveData<Resource<List<SongModel>>>()
    val mediaItems: LiveData<Resource<List<SongModel>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MusicService.MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                super.onChildrenLoaded(parentId, children)

                val items = children.map {
                    SongModel(
                        id = it.mediaId,
                        name = it.description.title.toString(),
                        songWriter = it.description.subtitle.toString(),
                        songUrl = it.description.mediaUri.toString(),
                        imageUrl = it.description.iconUri.toString()
                    )
                }

                _mediaItems.postValue(Resource.success(items))
            }
        })
    }

    fun skipToNextSong() {
        musicServiceConnection.transportCompat.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportCompat.skipToPrevious()
    }

    fun seekTo(position: Long) {
        musicServiceConnection.transportCompat.seekTo(position)
    }

    fun playOrToggleSong(mediaItem: SongModel, toggle: Boolean = false) {
        if (playbackState.value?.isPrepared == true &&
            mediaItem.id == curPlayingSong.value?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        ) {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (toggle) musicServiceConnection.transportCompat.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportCompat.play()
                    else -> Unit
                }
            }
        } else {
            musicServiceConnection.transportCompat.playFromMediaId(mediaItem.id, null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MusicService.MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {})
    }
}
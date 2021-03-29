package xyz.teamgravity.spotify.helper.extension

import android.support.v4.media.MediaMetadataCompat
import xyz.teamgravity.spotify.model.SongModel

fun MediaMetadataCompat.toSong(): SongModel? {
    return description?.let {
        SongModel(
            id = it.mediaId ?: "",
            name = it.title.toString(),
            songWriter = it.subtitle.toString(),
            songUrl = it.mediaUri.toString(),
            imageUrl = it.iconUri.toString()
        )
    }
}
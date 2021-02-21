package xyz.teamgravity.spotify.model

import com.google.firebase.firestore.DocumentId

data class SongModel(

    @DocumentId
    val id: String? = null,

    val name: String = "",
    val imageUrl: String = "",
    val songUrl: String = "",
    val songWriter: String = ""
)

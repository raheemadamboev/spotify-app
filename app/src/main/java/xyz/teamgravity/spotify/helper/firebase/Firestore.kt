package xyz.teamgravity.spotify.helper.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import xyz.teamgravity.spotify.model.SongModel

class Firestore {

    class Songs {
        companion object {
            const val C_NAME = "Songs"
        }

        suspend fun getAllSongs(): List<SongModel> {
            return try {
                FirebaseFirestore.getInstance().collection(C_NAME).get().await().toObjects(SongModel::class.java)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
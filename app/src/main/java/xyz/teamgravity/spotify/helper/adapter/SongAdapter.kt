package xyz.teamgravity.spotify.helper.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import xyz.teamgravity.spotify.databinding.CardSongBinding
import xyz.teamgravity.spotify.model.SongModel
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide: RequestManager
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    var songs: List<SongModel>
        get() = differ.currentList
        set(value) = differ.submitList(value)

    private val diffCallBack = object : DiffUtil.ItemCallback<SongModel>() {
        override fun areItemsTheSame(oldItem: SongModel, newItem: SongModel) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SongModel, newItem: SongModel) = oldItem == newItem
    }

    private val differ = AsyncListDiffer(this, diffCallBack)

    inner class SongViewHolder(private val binding: CardSongBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.let { click -> click(differ.currentList[position]) }
                }
            }
        }

        fun bind(model: SongModel) {
            binding.apply {
                primaryT.text = model.name
                secondaryT.text = model.songWriter
                glide.load(model.imageUrl).into(imageI)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(CardSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount() = differ.currentList.size

    private var onItemClickListener: ((SongModel) -> Unit)? = null

    fun setOnItemClickListener(listener: ((SongModel) -> Unit)) {
        onItemClickListener = listener
    }
}
package xyz.teamgravity.spotify.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import xyz.teamgravity.spotify.databinding.FragmentHomeBinding
import xyz.teamgravity.spotify.helper.adapter.SongAdapter
import xyz.teamgravity.spotify.helper.util.Status
import xyz.teamgravity.spotify.viewmodel.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var adapter: SongAdapter

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lateInIt()
        recyclerView()
        subscribeToObservers()
    }

    private fun lateInIt() {
        viewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)
    }

    private fun recyclerView() {
        binding.apply {
            recyclerView.adapter = adapter

            // song click
            adapter.setOnItemClickListener { song ->
                viewModel.playOrToggleSong(song)
            }
        }
    }

    private fun subscribeToObservers() {
        binding.apply {
            viewModel.mediaItems.observe(viewLifecycleOwner) { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        progressBar.isVisible = false
                        result.data?.let { songs ->
                            adapter.songs = songs
                        }
                    }

                    Status.ERROR -> Unit

                    Status.LOADING -> {
                        progressBar.isVisible = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
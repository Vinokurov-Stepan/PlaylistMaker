package com.practicum.playlistmaker.media.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.markodevcic.peko.PermissionRequester
import com.markodevcic.peko.PermissionResult
import com.practicum.playlistmaker.R
import com.practicum.playlistmaker.core.domain.models.Playlist
import com.practicum.playlistmaker.databinding.FragmentPlaylistMakerBinding
import com.practicum.playlistmaker.media.presentation.PlaylistsState
import com.practicum.playlistmaker.media.presentation.view_model.PlaylistsViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

class PlaylistMakerFragment : Fragment() {

    private var _binding: FragmentPlaylistMakerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: PlaylistsViewModel
    val requester = PermissionRequester.instance()
    private lateinit var galleryIntentLauncher: ActivityResultLauncher<Intent>
    private var isPhotoChanged = false
    private var playlist: Playlist = Playlist(0, null, null, null, null, 0)
    private var currentPlaylistId = -1

    private val playlistId: Int by lazy {
        arguments?.getInt("playlistId", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaylistMakerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = getViewModel()

        setupViews()
        setupObservers()

        currentPlaylistId = playlistId
        viewModel.loadPlaylistById(currentPlaylistId)
    }

    private fun setupViews() {
        galleryIntentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val uri = result.data?.data
                    if (uri != null) {
                        Glide.with(requireContext()).load(uri).centerCrop().transform(
                            RoundedCorners(
                                TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP, 8F, resources.displayMetrics
                                ).toInt()
                            )
                        ).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                            .into(binding.addPhotoButton)
                        isPhotoChanged = true
                        playlist.pathToPlaylistIcon = uri.toString()
                    }
                }
            }

        with(binding) {
            backButton.setNavigationOnClickListener {
                handleBackButtonPress()
            }

            nameOfPlaylist.doOnTextChanged { text, _, _, _ ->
                val trimmedText = text?.toString()?.trim()
                val isTextValid = !trimmedText.isNullOrEmpty() && trimmedText.isNotBlank()
                val hasFocus = nameOfPlaylist.hasFocus()
                updateEditTextState(
                    editText = nameOfPlaylist,
                    title = titleNameOfPlaylist,
                    hasFocus = hasFocus,
                    isTextValid = isTextValid
                )
                createPlaylistButton.isEnabled = isTextValid
                binding.createPlaylistButton.isClickable = isTextValid
                if (isTextValid) {
                    createPlaylistButton.setBackgroundResource(R.drawable.button_pressed)
                } else {
                    createPlaylistButton.setBackgroundResource(R.drawable.button_enabled)
                }
            }

            nameOfPlaylist.setOnFocusChangeListener { _, hasFocus ->
                val text = nameOfPlaylist.text?.toString()?.trim()
                val isTextValid = !text.isNullOrEmpty() && text.isNotBlank()
                updateEditTextState(
                    editText = nameOfPlaylist,
                    title = titleNameOfPlaylist,
                    hasFocus = hasFocus,
                    isTextValid = isTextValid
                )
                if (!hasFocus) {
                    nameOfPlaylist.setText(text)
                }
            }

            descriptionOfPlaylist.doOnTextChanged { text, _, _, _ ->
                val trimmedText = text?.toString()?.trim()
                val isTextValid = !trimmedText.isNullOrEmpty() && trimmedText.isNotBlank()
                val hasFocus = descriptionOfPlaylist.hasFocus()
                updateEditTextState(
                    editText = descriptionOfPlaylist,
                    title = titleDescriptionOfPlaylist,
                    hasFocus = hasFocus,
                    isTextValid = isTextValid
                )
            }

            descriptionOfPlaylist.setOnFocusChangeListener { _, hasFocus ->
                val text = descriptionOfPlaylist.text?.toString()?.trim()
                val isTextValid = !text.isNullOrEmpty() && text.isNotBlank()
                updateEditTextState(
                    editText = descriptionOfPlaylist,
                    title = titleDescriptionOfPlaylist,
                    hasFocus = hasFocus,
                    isTextValid = isTextValid
                )
                if (!hasFocus) {
                    descriptionOfPlaylist.setText(text)
                }
            }

            addPhotoButton.setOnClickListener {
                lifecycleScope.launch {
                    requester.request(Manifest.permission.READ_MEDIA_IMAGES).collect { result ->
                        when (result) {
                            is PermissionResult.Granted -> {
                                launchGalleryIntent()
                            }

                            is PermissionResult.Denied.DeniedPermanently -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.data = Uri.fromParts("package", context?.packageName, null)
                                context?.startActivity(intent)
                            }

                            is PermissionResult.Denied.NeedsRationale -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.access_point_to_the_gallery),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            is PermissionResult.Cancelled -> {
                                return@collect
                            }
                        }
                    }
                }
            }

            createPlaylistButton.setOnClickListener {
                handleCreateOrSaveButton()
            }
        }
    }

    private fun handleBackButtonPress() {
        if (playlist.playlistName != null) {
            findNavController().navigateUp()
        } else {
            if (isPhotoChanged || binding.nameOfPlaylist.text.isNotEmpty() || binding.descriptionOfPlaylist.text.isNotEmpty()) {
                MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.message_of_playlist_dialog))
                    .setMessage(getString(R.string.message_text_of_playlist_dialog))
                    .setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                    }.setPositiveButton(R.string.complete) { dialog, which ->
                        findNavController().navigateUp()
                    }.show()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun handleCreateOrSaveButton() {
        playlist.playlistName = binding.nameOfPlaylist.text.toString().trim()
        playlist.playlistDescription = binding.descriptionOfPlaylist.text.toString()
        if (currentPlaylistId != -1) {
            val updatedPlaylist = playlist.copy(
                playlistName = playlist.playlistName,
                playlistDescription = playlist.playlistDescription,
                pathToPlaylistIcon = playlist.pathToPlaylistIcon
            )
            val imageUriForUpdate = if (isPhotoChanged) {
                playlist.pathToPlaylistIcon?.let { Uri.parse(it) }
            } else {
                null
            }
            viewModel.updatePlaylist(updatedPlaylist, imageUriForUpdate)
            findNavController().popBackStack()
        } else {
            val newPlaylist = playlist.copy(
                playlistName = playlist.playlistName,
                playlistDescription = playlist.playlistDescription,
                pathToPlaylistIcon = playlist.pathToPlaylistIcon
            )
            viewModel.onPlaylistCreate(
                newPlaylist, playlist.pathToPlaylistIcon?.let { Uri.parse(it) })

            val playlistName = "Плейлист \"${playlist.playlistName}\" создан"
            Toast.makeText(requireContext(), playlistName, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.playlistState.collect { state ->
                render(state)
                if (state is PlaylistsState.PlaylistContent) {
                    setupEditModeIfNeeded()
                }
            }
        }
    }

    private fun render(state: PlaylistsState) {
        when (state) {
            is PlaylistsState.PlaylistContent -> loadPlaylist(state.playlist)
        }
    }

    private fun loadPlaylist(playlist: Playlist?) {
        this.playlist = playlist!!
    }

    private fun setupEditModeIfNeeded() {
        if (currentPlaylistId != -1) {
            binding.backButton.title = getString(R.string.to_redact)
            binding.createPlaylistButton.text = getString(R.string.to_save)
            binding.nameOfPlaylist.setText(playlist.playlistName)
            binding.descriptionOfPlaylist.setText(playlist.playlistDescription)
            playlist.pathToPlaylistIcon?.let { imagePath ->
                Glide.with(requireContext()).load(imagePath).centerCrop().transform(
                    RoundedCorners(
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 8F, resources.displayMetrics
                        ).toInt()
                    )
                ).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                    .into(binding.addPhotoButton)
                isPhotoChanged = true
            }
            val isNameValid = !playlist.playlistName.isNullOrEmpty()
            binding.createPlaylistButton.isEnabled = isNameValid
            binding.createPlaylistButton.isClickable = isNameValid
            if (isNameValid) {
                binding.createPlaylistButton.setBackgroundResource(R.drawable.button_pressed)
            }
        }
    }

    private fun launchGalleryIntent() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
        }
        galleryIntentLauncher.launch(intent)
    }

    private fun updateEditTextState(
        editText: EditText, title: TextView, hasFocus: Boolean, isTextValid: Boolean
    ) {
        if (hasFocus) {
            title.isVisible = true
            editText.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.edit_pressed)
        } else if (isTextValid) {
            title.isVisible = true
            editText.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.edit_pressed)
        } else {
            title.isVisible = false
            editText.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.edit_enabled)
        }
    }
}

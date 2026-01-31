package com.practicum.playlistmaker.settings.presentation.view_model

import androidx.lifecycle.ViewModel
import com.practicum.playlistmaker.settings.domain.api.SettingsInteractor
import com.practicum.playlistmaker.sharing.domain.api.SharingInteractor

class SettingsViewModel(
    private val settingsInteractor: SettingsInteractor,
    private val sharingInteractor: SharingInteractor
) : ViewModel() {

    fun updateThemeSettings(checked: Boolean) {
        settingsInteractor.updateThemeSetting(checked)
    }

    fun shareState() {
        sharingInteractor.shareApp()
    }

    fun supportingState() {
        sharingInteractor.openSupport()
    }

    fun agreementState() {
        sharingInteractor.openTerms()
    }
}

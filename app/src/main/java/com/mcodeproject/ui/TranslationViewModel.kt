package com.mcodeproject.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcodeproject.billing.BillingManager
import com.mcodeproject.data.repository.SettingsRepository
import com.mcodeproject.data.repository.TranslationRepository
import com.mcodeproject.data.repository.TranslationResult
import com.mcodeproject.util.NetworkObserver
import com.mcodeproject.util.TtsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiState(
    val sourceText: String = "",
    val translatedText: String = "",
    val sourceLang: String = "pl", // "pl" or "en"
    val targetLang: String = "en", // "en" or "pl"
    val isNetworkOnline: Boolean = true,
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val isSpeaking: Boolean = false,
    val isPremium: Boolean = false,
    val canPurchasePremium: Boolean = false,
    // Nowe ustawienia
    val fontSize: String = "Średnia",
    val autoCopy: Boolean = false,
    val hapticEnabled: Boolean = true
)

@HiltViewModel
class TranslationViewModel @Inject constructor(
    private val repository: TranslationRepository,
    private val networkObserver: NetworkObserver,
    private val ttsHelper: TtsHelper,
    private val billingManager: BillingManager,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                networkObserver.observeNetworkState(),
                ttsHelper.isSpeaking,
                billingManager.isPremiumPurchased,
                billingManager.products,
                settingsRepository.userSettingsFlow
            ) { online, speaking, premium, products, settings ->
                _uiState.value.copy(
                    isNetworkOnline = online,
                    isSpeaking = speaking,
                    isPremium = premium,
                    canPurchasePremium = products.any { it.productId == "premium_upgrade" } && !premium,
                    fontSize = settings.fontSize,
                    autoCopy = settings.autoCopy,
                    hapticEnabled = settings.hapticEnabled
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    // Metody ustawień
    fun setFontSize(size: String) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
        }
    }

    fun toggleAutoCopy(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoCopy(enabled)
        }
    }

    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticEnabled(enabled)
        }
    }

    fun buyPremium(activity: Activity) {
        val product = billingManager.products.value.find { it.productId == "premium_upgrade" }
        if (product != null) {
            billingManager.launchPurchaseFlow(activity, product)
        } else {
            Toast.makeText(context, "Produkt niedostępny", Toast.LENGTH_SHORT).show()
        }
    }

    fun onSourceTextChanged(newText: String, isImmediate: Boolean = false) {
        _uiState.value = _uiState.value.copy(sourceText = newText)

        if (newText.isBlank()) {
            _uiState.value = _uiState.value.copy(translatedText = "", isLoading = false, statusMessage = null)
            return
        }

        if (isImmediate) {
            executeTranslation()
        }
    }

    fun executeTranslation() {
        val current = _uiState.value
        val text = current.sourceText.trim()
        if (text.isEmpty()) return

        _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = null)

        viewModelScope.launch {
            val result = repository.translateText(
                sourceText = text,
                sourceLang = current.sourceLang,
                targetLang = current.targetLang
            )

            when (result) {
                is TranslationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        translatedText = result.translatedText,
                        isLoading = false,
                        statusMessage = null
                    )
                    // Automatyczne kopiowanie jeśli włączone
                    if (_uiState.value.autoCopy) {
                        copyToClipboard(result.translatedText, showToast = false)
                    }
                }
                is TranslationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = result.message
                    )
                }
            }
        }
    }

    fun swapLanguages() {
        val current = _uiState.value
        val newSourceLang = current.targetLang
        val newTargetLang = current.sourceLang
        val newSourceText = current.translatedText
        val newTranslatedText = current.sourceText

        _uiState.value = current.copy(
            sourceLang = newSourceLang,
            targetLang = newTargetLang,
            sourceText = newSourceText,
            translatedText = newTranslatedText
        )

        if (newSourceText.isNotBlank()) {
            executeTranslation()
        }
    }

    fun speakOrStop(text: String, lang: String) {
        if (_uiState.value.isSpeaking) {
            ttsHelper.stopSpeaking()
        } else {
            ttsHelper.speak(text, lang)
        }
    }

    fun pasteFromClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString() ?: ""
                if (text.isNotBlank()) {
                    onSourceTextChanged(text, isImmediate = true)
                    Toast.makeText(context, "Wklejono ze schowka", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Schowek jest pusty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Schowek jest pusty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Błąd odczytu ze schowka", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(textToCopy: String, showToast: Boolean = true) {
        if (textToCopy.isBlank()) return
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Tłumaczenie", textToCopy)
            clipboard.setPrimaryClip(clip)
            if (showToast) {
                Toast.makeText(context, "Skopiowano do schowka!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(context, "Błąd kopiowania", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearInput() {
        _uiState.value = _uiState.value.copy(sourceText = "", translatedText = "", statusMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.stop()
    }
}

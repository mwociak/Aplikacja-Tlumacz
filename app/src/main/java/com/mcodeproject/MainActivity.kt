package com.mcodeproject

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mcodeproject.ui.TranslationViewModel
import com.mcodeproject.ui.components.AdBanner
import com.mcodeproject.ui.components.LanguageSelectorBar
import com.mcodeproject.ui.components.TranslationInputCard
import com.mcodeproject.ui.components.TranslationResultCard
import com.mcodeproject.ui.theme.TranslatorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TranslatorTheme {
                val viewModel: TranslationViewModel = hiltViewModel()

                TranslatorApp(
                    viewModel = viewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorApp(
    viewModel: TranslationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showSettingsMenu by remember { mutableStateOf(false) }

    // Helper for haptic feedback respecting the user setting
    val performHaptic = {
        if (uiState.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val errorMessage = uiState.statusMessage
        if (!errorMessage.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Ponów próbę",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.executeTranslation()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tłumacz",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (uiState.isPremium) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = "Premium",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.isNetworkOnline) "Szybkie tłumaczenie Online" else "Brak połączenia",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.canPurchasePremium) {
                        IconButton(onClick = { viewModel.buyPremium(context as Activity) }) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Kup Premium",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ustawienia",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            Text(
                                text = "Ogólne",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            DropdownMenuItem(
                                text = { Text("Automatyczne kopiowanie") },
                                onClick = { viewModel.toggleAutoCopy(!uiState.autoCopy) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = uiState.autoCopy,
                                        onCheckedChange = null
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Wibracje (Haptic)") },
                                onClick = { viewModel.toggleHaptic(!uiState.hapticEnabled) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = uiState.hapticEnabled,
                                        onCheckedChange = null
                                    )
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                text = "Wielkość czcionki",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            listOf("Mała", "Średnia", "Duża").forEach { size ->
                                DropdownMenuItem(
                                    text = { Text(size) },
                                    onClick = { viewModel.setFontSize(size) },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = uiState.fontSize == size,
                                            onClick = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!uiState.isPremium) {
                AdBanner(modifier = Modifier.padding(bottom = 16.dp))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Language Selector Bar
            LanguageSelectorBar(
                sourceLang = uiState.sourceLang,
                targetLang = uiState.targetLang,
                isOnline = uiState.isNetworkOnline,
                onSwap = {
                    performHaptic()
                    viewModel.swapLanguages()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Input Card
            TranslationInputCard(
                sourceText = uiState.sourceText,
                sourceLang = uiState.sourceLang,
                isSpeaking = uiState.isSpeaking,
                onTextChanged = { viewModel.onSourceTextChanged(it) },
                onPaste = {
                    performHaptic()
                    viewModel.pasteFromClipboard()
                },
                onClear = {
                    performHaptic()
                    viewModel.clearInput()
                },
                onSpeak = { viewModel.speakOrStop(uiState.sourceText, uiState.sourceLang) },
                onTranslate = { viewModel.executeTranslation() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Result Card
            TranslationResultCard(
                translatedText = uiState.translatedText,
                targetLang = uiState.targetLang,
                isLoading = uiState.isLoading,
                isSpeaking = uiState.isSpeaking,
                fontSize = uiState.fontSize,
                onCopy = {
                    performHaptic()
                    viewModel.copyToClipboard(uiState.translatedText)
                },
                onSpeak = { viewModel.speakOrStop(uiState.translatedText, uiState.targetLang) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

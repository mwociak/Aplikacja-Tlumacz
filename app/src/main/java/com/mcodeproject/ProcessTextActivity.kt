package com.mcodeproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mcodeproject.ui.TranslationViewModel
import com.mcodeproject.ui.theme.TranslatorTheme
import dagger.hilt.android.AndroidEntryPoint

private val POLISH_CHARS = Regex("[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]")

/**
 * Proste wykrywanie języka: obecność polskich znaków → polski, w innym
 * przypadku zakładamy angielski ("en").
 */
private fun detectLanguage(text: String): String =
    if (POLISH_CHARS.containsMatchIn(text)) "pl" else "en"

/**
 * Aktywność uruchamiana z menu akcji na zaznaczonym tekście
 * (android.intent.action.PROCESS_TEXT). Android przekazuje zaznaczony
 * tekst w Intent.EXTRA_PROCESS_TEXT.
 *
 * Pokazuje małe okno z tłumaczeniem. Automatyczne kopiowanie do schowka
 * działa zgodnie z ustawieniem „Automatyczne kopiowanie".
 */
@AndroidEntryPoint
class ProcessTextActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent
            .getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            ?.toString()
            ?.trim()
            .orEmpty()

        // Główne okno domyślnie tłumaczy PL→EN; tutaj wykrywamy kierunek
        // na podstawie zaznaczonego tekstu (np. angielski tekst → EN→PL).
        val sourceLang = detectLanguage(selectedText)
        val targetLang = if (sourceLang == "pl") "en" else "pl"

        setContent {
            TranslatorTheme {
                val viewModel: TranslationViewModel = hiltViewModel()
                ProcessTextView(
                    selectedText = selectedText,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    viewModel = viewModel,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
private fun ProcessTextView(
    selectedText: String,
    sourceLang: String,
    targetLang: String,
    viewModel: TranslationViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(selectedText) {
        started = true
        if (selectedText.isNotBlank()) {
            viewModel.setSourceAndTarget(sourceLang, targetLang)
            viewModel.onSourceTextChanged(selectedText, isImmediate = true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Tłumacz",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sourceLang.uppercase()} → ${targetLang.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedText.isNotBlank()) {
                    Text(
                        text = "„$selectedText”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when {
                    !started -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tłumaczenie…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    selectedText.isBlank() -> {
                        Text(
                            text = "Nie zaznaczono tekstu do przetłumaczenia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    uiState.isLoading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Tłumaczenie…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    uiState.translatedText.isNotBlank() -> {
                        Text(
                            text = uiState.translatedText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    else -> {
                        Text(
                            text = uiState.statusMessage ?: "Nie udało się przetłumaczyć tekstu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    if (uiState.translatedText.isNotBlank()) {
                        OutlinedButton(onClick = { viewModel.copyToClipboard(uiState.translatedText) }) {
                            Text("Kopiuj")
                        }
                    }
                    if (uiState.statusMessage != null && selectedText.isNotBlank()) {
                        Button(onClick = { viewModel.executeTranslation() }) {
                            Text("Ponów")
                        }
                    }
                    TextButton(onClick = onClose) {
                        Text("Zamknij")
                    }
                }
            }
        }
    }
}

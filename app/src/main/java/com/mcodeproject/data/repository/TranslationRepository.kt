package com.mcodeproject.data.repository

import com.mcodeproject.data.remote.FallbackApiClient
import com.mcodeproject.util.NetworkObserver
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository obsługujący tłumaczenia z 2-tierową strategią fallback:
 * 1. Google Translate API (główne źródło)
 * 2. MyMemory API (fallback)
 * 
 * Dodatkowo wykorzystuje pamięć podręczną (cache) w celu
 * przyspieszenia powtarzających się zapytań.
 */
class TranslationRepository(
    private val networkObserver: NetworkObserver
) {
    // In-memory cache for instant translations and zero duplicate API calls
    private val translationCache = ConcurrentHashMap<String, String>()

    suspend fun translateText(
        sourceText: String,
        sourceLang: String,
        targetLang: String
    ): TranslationResult {
        val trimmed = sourceText.trim()
        if (trimmed.isEmpty()) {
            return TranslationResult.Success("")
        }

        if (!networkObserver.isOnline()) {
            return TranslationResult.Error("Brak połączenia z siecią Internet. Wymagany tryb Online.")
        }

        val cacheKey = "${sourceLang.lowercase()}_${targetLang.lowercase()}_$trimmed"
        translationCache[cacheKey]?.let { cached ->
            return TranslationResult.Success(cached)
        }

        // Tier 1: Szybki silnik Google Translate (Główne źródło)
        val googleResult = FallbackApiClient.translateGoogle(trimmed, sourceLang, targetLang)
        if (googleResult.isSuccess) {
            val translated = googleResult.getOrThrow()
            translationCache[cacheKey] = translated
            return TranslationResult.Success(translated)
        }

        // Tier 2: MyMemory Translation API (Fallback)
        val myMemoryResult = FallbackApiClient.translateMyMemory(trimmed, sourceLang, targetLang)
        if (myMemoryResult.isSuccess) {
            val translated = myMemoryResult.getOrThrow()
            translationCache[cacheKey] = translated
            return TranslationResult.Success(translated)
        }

        return TranslationResult.Error("Nie udało się przetłumaczyć tekstu. Spróbuj ponownie za chwilę.")
    }
}

sealed class TranslationResult {
    data class Success(val translatedText: String) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}




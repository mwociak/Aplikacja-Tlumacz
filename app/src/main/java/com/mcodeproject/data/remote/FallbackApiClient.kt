package com.mcodeproject.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object FallbackApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Fallback Engine 1: Google Translate Public Endpoint
     */
    suspend fun translateGoogle(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && !responseString.isNullOrEmpty()) {
                val jsonArray = JSONArray(responseString)
                val sentences = jsonArray.optJSONArray(0)
                if (sentences != null) {
                    val sb = StringBuilder()
                    for (i in 0 until sentences.length()) {
                        val sentence = sentences.optJSONArray(i)
                        if (sentence != null && !sentence.isNull(0)) {
                            sb.append(sentence.getString(0))
                        }
                    }
                    val result = sb.toString().trim()
                    if (result.isNotEmpty()) {
                        return@withContext Result.success(result)
                    }
                }
            }
            Result.failure(Exception("Błąd odpowiedzi z silnika Google Translate."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fallback Engine 2: MyMemory Translation API
     */
    suspend fun translateMyMemory(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val langPair = "${sourceLang.lowercase()}|${targetLang.lowercase()}"
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encodedText&langpair=$langPair"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string()

            if (response.isSuccessful && !responseString.isNullOrEmpty()) {
                val jsonObject = JSONObject(responseString)
                val responseData = jsonObject.optJSONObject("responseData")
                val translatedText = responseData?.optString("translatedText")?.trim()
                if (!translatedText.isNullOrEmpty() && !translatedText.contains("MYMEMORY WARNING")) {
                    return@withContext Result.success(translatedText)
                }
            }
            Result.failure(Exception("Błąd odpowiedzi z silnika MyMemory."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

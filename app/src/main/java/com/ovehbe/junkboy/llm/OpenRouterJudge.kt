package com.ovehbe.junkboy.llm

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Asks a model on OpenRouter (https://openrouter.ai) for a [SpamVerdict]. Messages are redacted first. */
class OpenRouterJudge(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL
) : SpamJudge {

    companion object {
        private const val TAG = "OpenRouterJudge"
        const val DEFAULT_MODEL = "anthropic/claude-haiku-5.5"
        private const val ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 8_000

        internal const val SYSTEM_PROMPT =
            "You judge text messages (SMS) received by one person from senders they have never texted. " +
                "Answer with exactly one word.\n" +
                "SPAM: the sender is trying to get something from the person: money, votes, signatures, " +
                "survey answers, a purchase, a click on a promotion, or a job application. This includes " +
                "political and advocacy campaigns, fundraising, petitions, polls, marketing, recruiting and scams.\n" +
                "WANTED: account, service and transaction messages: welcome, activation, sign-up or " +
                "subscription confirmations, alert setup, login or verification codes, appointments and health " +
                "care, bank or card notices, deliveries, receipts, order updates, waitlists and reservations, " +
                "and service status. These are WANTED even when they mention STOP, HELP or message and data " +
                "rates, because that boilerplate appears in all texts from short codes.\n" +
                "UNSURE: you cannot tell. Also use UNSURE, not SPAM, for feedback requests, review requests, " +
                "satisfaction surveys, coupons and promotions that come from a business the person has visited " +
                "or bought from, since those are mildly annoying but not unwanted spam.\n" +
                "Reply with only SPAM, WANTED or UNSURE."
    }

    override suspend fun judge(sender: String, body: String): SpamVerdict =
        try {
            judgeOrThrow(sender, body)
        } catch (e: Exception) {
            Log.w(TAG, "Judge failed, treating as UNSURE: ${e.message}")
            SpamVerdict.UNSURE
        }

    /** Like [judge] but reports failures, so the settings screen can show what went wrong. */
    suspend fun judgeOrThrow(sender: String, body: String): SpamVerdict = withContext(Dispatchers.IO) {
        val messages = JsonArray().apply {
            add(JsonObject().apply { addProperty("role", "system"); addProperty("content", SYSTEM_PROMPT) })
            add(JsonObject().apply {
                addProperty("role", "user")
                addProperty(
                    "content",
                    "Sender: ${MessageRedactor.senderKind(sender)}\nMessage: ${MessageRedactor.redact(body)}"
                )
            })
        }
        val request = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", 0)
            addProperty("max_tokens", 10)
            add("messages", messages)
        }

        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Title", "Junkboy")
            connection.outputStream.use { it.write(request.toString().toByteArray()) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code ${errorMessage(detail)}".trim())
            }
            val reply = connection.inputStream.bufferedReader().use { it.readText() }
            parseVerdict(extractContent(reply))
        } finally {
            connection.disconnect()
        }
    }

    private fun extractContent(json: String): String =
        JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("message")?.get("content")?.takeIf { !it.isJsonNull }?.asString
            ?: throw IOException("Reply had no content")

    private fun errorMessage(json: String): String =
        try {
            JsonParser.parseString(json).asJsonObject.getAsJsonObject("error")?.get("message")?.asString.orEmpty()
        } catch (e: Exception) {
            ""
        }
}

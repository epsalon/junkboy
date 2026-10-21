package com.ovehbe.junkboy.filters

/**
 * Detects commercial opt-out directives ("Reply STOP to end", "STOP2END", ...).
 *
 * Deliberately stricter than a bare `stop` match so that ordinary messages
 * ("the bus stop is closed") never qualify for an auto-reply.
 */
object Stop2EndDetector {

    private val VERBS = "reply|respond|text|txt|send"
    private val ACTIONS = "end|cancel|quit|unsubscribe"

    val PATTERN = Regex(
        "\\b(?:(?:$VERBS)\\s+[\"'“‘]?stop\\b|stop\\s*(?:2|to)\\s*(?:$ACTIONS)\\b)",
        RegexOption.IGNORE_CASE
    )

    fun matches(body: String): Boolean = PATTERN.containsMatchIn(body)
}

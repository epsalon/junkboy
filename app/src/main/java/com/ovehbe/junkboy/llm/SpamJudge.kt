package com.ovehbe.junkboy.llm

enum class SpamVerdict { SPAM, WANTED, UNSURE }

/** A second opinion on whether a text from an unknown sender is unsolicited bulk messaging. */
interface SpamJudge {
    /** Never throws: anything that goes wrong is [SpamVerdict.UNSURE]. */
    suspend fun judge(sender: String, body: String): SpamVerdict
}

/** Reads the first verdict word in a model reply; anything unrecognizable is UNSURE. */
fun parseVerdict(reply: String): SpamVerdict {
    val upper = reply.uppercase()
    return listOf("SPAM" to SpamVerdict.SPAM, "WANTED" to SpamVerdict.WANTED, "UNSURE" to SpamVerdict.UNSURE)
        .map { (word, verdict) -> upper.indexOf(word) to verdict }
        .filter { (index, _) -> index >= 0 }
        .minByOrNull { (index, _) -> index }
        ?.second ?: SpamVerdict.UNSURE
}

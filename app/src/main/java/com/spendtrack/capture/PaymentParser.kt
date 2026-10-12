package com.spendtrack.capture

data class ParsedPayment(val amount: Double, val merchant: String)

/**
 * Turns a bank SMS or UPI app notification into a payment, or null if the
 * text is not a debit (credits, OTPs, refunds and promos are ignored).
 */
object PaymentParser {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitWords = listOf(
        "debited", "spent", "paid", "sent", "purchase", "withdrawn",
        "payment of", "txn of", "transaction of"
    )

    private val ignoreWords = listOf(
        "credited", "received", "refund", "otp", "cashback",
        "request", "requested", "will be debited", "failed", "declined"
    )

    private val merchantRegexes = listOf(
        Regex("""\bto\s+(?:vpa\s+)?([A-Za-z0-9@._& \-]{2,40}?)(?:\s+on\b|\s+ref\b|\s+upi\b|\.|,|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+([A-Za-z0-9@._& \-]{2,40}?)(?:\s+on\b|\s+ref\b|\.|,|$)""", RegexOption.IGNORE_CASE),
        Regex("""\bvpa\s+([A-Za-z0-9@._\-]{3,40})""", RegexOption.IGNORE_CASE)
    )

    fun parse(text: String): ParsedPayment? {
        val lower = text.lowercase()
        if (ignoreWords.any { lower.contains(it) }) return null
        if (debitWords.none { lower.contains(it) }) return null

        val amount = amountRegex.find(text)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null
        if (amount <= 0.0) return null

        val merchant = merchantRegexes
            .firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.trim() }
            ?.takeIf { it.isNotBlank() }
            ?: "Unknown"

        return ParsedPayment(amount, merchant)
    }
}

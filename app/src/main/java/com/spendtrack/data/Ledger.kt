package com.spendtrack.data

/** One line in a friend's account: a share they owe, or a payback they made. */
data class LedgerEntry(
    val timestamp: Long,
    val title: String,
    val amount: Double,
    val isPayback: Boolean
)

data class FriendBalance(
    val friend: Friend,
    val owed: Double,
    val paidBack: Double
) {
    val pending: Double get() = owed - paidBack
    val settled: Boolean get() = pending < 0.5
}

object Ledger {

    fun balances(
        friends: List<Friend>,
        shares: List<SplitShare>,
        paybacks: List<Payback>
    ): List<FriendBalance> = friends.map { f ->
        FriendBalance(
            friend = f,
            owed = shares.filter { it.friendId == f.id }.sumOf { it.amount },
            paidBack = paybacks.filter { it.friendId == f.id }.sumOf { it.amount }
        )
    }

    fun entries(
        friendId: Long,
        txs: List<Transaction>,
        shares: List<SplitShare>,
        paybacks: List<Payback>
    ): List<LedgerEntry> {
        val byId = txs.associateBy { it.id }
        val owed = shares.filter { it.friendId == friendId }.mapNotNull { sh ->
            byId[sh.transactionId]?.let { LedgerEntry(it.timestamp, it.merchant, sh.amount, false) }
        }
        val back = paybacks.filter { it.friendId == friendId }.map {
            LedgerEntry(it.timestamp, it.note.ifBlank { "Paid back" }, it.amount, true)
        }
        return (owed + back).sortedByDescending { it.timestamp }
    }

    /** Message the user can send to a friend: full records plus what is pending. */
    fun reminderText(
        friendName: String,
        myName: String,
        upiId: String,
        entries: List<LedgerEntry>,
        pending: Double
    ): String {
        val sb = StringBuilder()
        sb.append("Hi ").append(friendName).append("! 👋\n")
        if (pending >= 0.5) {
            sb.append("Quick reminder for the shared expenses I paid for:\n\n")
        } else {
            sb.append("Here is our shared expense record:\n\n")
        }
        entries.filter { !it.isPayback }.sortedBy { it.timestamp }.forEach {
            sb.append("• ").append(Fmt.shortDate(it.timestamp)).append(" · ")
                .append(it.title).append(": ").append(Fmt.money(it.amount)).append("\n")
        }
        val paid = entries.filter { it.isPayback }.sumOf { it.amount }
        if (paid > 0) {
            sb.append("\nPaid back so far: ").append(Fmt.money(paid)).append("\n")
        }
        if (pending >= 0.5) {
            sb.append("Pending: ").append(Fmt.money(pending)).append("\n")
            if (upiId.isNotBlank()) sb.append("Pay me on UPI: ").append(upiId).append("\n")
            sb.append("\nThanks! 🙏")
        } else {
            sb.append("\nAll settled ✅ Thanks!")
        }
        if (myName.isNotBlank()) sb.append("\n– ").append(myName)
        return sb.toString()
    }
}

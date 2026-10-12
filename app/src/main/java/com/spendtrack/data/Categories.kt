package com.spendtrack.data

object Categories {
    const val FOOD = "Food"
    const val TRAVEL = "Travel"
    const val STUDY = "Study"
    const val HOSTEL = "Rent & Hostel"
    const val FUN = "Fun"
    const val SHOPPING = "Shopping"
    const val BILLS = "Bills & Recharge"
    const val HEALTH = "Health"
    const val OTHERS = "Others"
    const val INCOME = "Income"

    /** Key used in the budgets table for the overall monthly budget. */
    const val ALL_BUDGET = "ALL"

    data class Info(
        val name: String,
        val emoji: String,
        val color: Long,
        val keywords: List<String> = emptyList()
    )

    val expense: List<Info> = listOf(
        Info(
            FOOD, "🍔", 0xFFFF7675,
            listOf(
                "zomato", "swiggy", "canteen", "cafe", "restaurant", "pizza", "dominos",
                "kfc", "mcdonald", "burger", "chai", "tea", "coffee", "starbucks", "bakery",
                "mess", "food", "blinkit", "zepto", "bigbasket", "instamart", "grocer",
                "juice", "biryani", "sweets", "dhaba"
            )
        ),
        Info(
            TRAVEL, "🚌", 0xFF0984E3,
            listOf(
                "uber", "ola", "rapido", "irctc", "redbus", "metro", "bus", "fuel", "petrol",
                "diesel", "indigo", "railway", "train", "cab", "auto", "yulu", "abhibus",
                "makemytrip", "fastag", "parking"
            )
        ),
        Info(
            STUDY, "📚", 0xFF6C5CE7,
            listOf(
                "book", "stationery", "xerox", "print", "college", "school", "university",
                "fee", "tuition", "exam", "coursera", "udemy", "unacademy", "byju",
                "library", "notebook", "course"
            )
        ),
        Info(
            HOSTEL, "🏠", 0xFF00B894,
            listOf("rent", "hostel", "pg", "laundry", "electricity", "landlord", "maintenance")
        ),
        Info(
            FUN, "🎬", 0xFFE84393,
            listOf(
                "netflix", "spotify", "bookmyshow", "movie", "pvr", "inox", "game", "steam",
                "hotstar", "prime", "youtube", "bgmi", "gaming", "club", "party", "trip"
            )
        ),
        Info(
            SHOPPING, "🛍️", 0xFFF0A500,
            listOf(
                "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "decathlon",
                "store", "mart", "mall", "shop"
            )
        ),
        Info(
            BILLS, "📱", 0xFF00CEC9,
            listOf(
                "recharge", "jio", "airtel", "vodafone", "bsnl", "broadband", "wifi", "dth",
                "bill", "postpaid", "vi"
            )
        ),
        Info(
            HEALTH, "💊", 0xFFD63031,
            listOf(
                "pharmacy", "medical", "apollo", "medplus", "hospital", "clinic", "doctor",
                "pharmeasy", "1mg", "gym", "cult"
            )
        ),
        Info(OTHERS, "💸", 0xFF636E72)
    )

    val incomeInfo = Info(INCOME, "💰", 0xFF00B894)

    fun info(name: String): Info =
        expense.firstOrNull { it.name == name }
            ?: if (name == INCOME) incomeInfo else expense.last()

    /** Best-guess category for a merchant name. */
    fun guess(merchant: String): String {
        val lower = merchant.lowercase()
        val tokens = lower.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        for (info in expense) {
            for (k in info.keywords) {
                val hit = if (k.length >= 4) lower.contains(k) else tokens.contains(k)
                if (hit) return info.name
            }
        }
        return OTHERS
    }
}

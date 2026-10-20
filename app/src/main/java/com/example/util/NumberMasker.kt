package com.example.util

object NumberMasker {

    fun mask(number: String): String {
        val trimmed = number.trim()
        if (trimmed.length <= 4) {
            return "*".repeat(trimmed.length)
        }

        val prefixLen = if (trimmed.startsWith("+")) 5 else 3
        val suffixLen = 4

        if (trimmed.length <= prefixLen + suffixLen) {
            val half = trimmed.length / 2
            return trimmed.take(half / 2) + "****" + trimmed.takeLast(half / 2)
        }

        val prefix = trimmed.take(prefixLen)
        val suffix = trimmed.takeLast(suffixLen)
        val maskCount = (trimmed.length - prefixLen - suffixLen).coerceIn(3, 6)

        return prefix + "*".repeat(maskCount) + suffix
    }

    fun maskInText(text: String): String {
        val phonePattern = Regex("""(\+?\d{7,15})""")
        return phonePattern.replace(text) { matchResult ->
            mask(matchResult.value)
        }
    }
}

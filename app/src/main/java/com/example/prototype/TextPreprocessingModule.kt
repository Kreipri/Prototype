package com.example.prototype

class TextPreprocessingModule {

    // Symbol and number to letter conversions
    private val symbolConversions = mapOf(
        '1' to 'i',
        '3' to 'e',
        '4' to 'a',
        '5' to 's',
        '7' to 't',
        '8' to 'b',
        '0' to 'o',
        '@' to 'a',
        '$' to 's',
        '!' to '!',  // Keep exclamation mark as is
        '.' to '.',  // Keep period as is
        ',' to ','   // Keep comma as is
    )

    fun preprocessText(ocrText: String): String {
        // Tokenize the text into words
        val tokens = ocrText.split(Regex("\\s+"))
        
        // Process each token
        val processedTokens = tokens.map { token ->
            processToken(token)
        }
        
        return processedTokens.joinToString(" ")
    }

    private fun processToken(token: String): String {
        if (token.isEmpty()) return token
        
        // Check if word ends with exclamation mark
        if (token.endsWith("!")) {
            return token
        }
        
        // Process each character in the token
        val chars = token.toMutableList()
        var i = 0
        
        while (i < chars.size) {
            val currentChar = chars[i]
            
            // Check if character is a number or symbol (and should be converted)
            if (shouldConvert(currentChar)) {
                chars[i] = symbolConversions[currentChar] ?: currentChar
            }
            
            // Check for duplicate pattern: if current char is same as previous two chars
            if (i >= 2 && chars[i] == chars[i - 1] && chars[i - 1] == chars[i - 2]) {
                // Remove the current character (duplicate in 3-char pattern)
                chars.removeAt(i)
                // Don't increment i, check the same position again
                continue
            }
            
            i++
        }
        
        return chars.joinToString("")
    }

    private fun shouldConvert(char: Char): Boolean {
        // Check if character is a number or a symbol that should be converted
        return char.isDigit() || 
               (char in symbolConversions && char !in "!.,")
    }
}

package com.example.keyboard

object UnicodeStylizer {

    enum class TextStyle(val displayName: String) {
        NORMAL("عادي (Default)"),
        CURSIVE("𝓒𝓾𝓻𝓼𝓲𝓿𝓮 (كيرسيف)"),
        GOTHIC("𝔊𝔬𝔱𝔥𝔦𝔠 (غوثيك)"),
        GOTHIC_BOLD("𝕲𝖔𝖙𝖍𝖎𝖈 𝕭𝖔𝖑𝖉 (غوثيك عريض)"),
        DOUBLE_STRUCK("𝔻𝕠𝕦𝕓𝕝𝕖 (مفرغ)"),
        CIRCLED("Ⓒⓘⓡⓒⓛⓔⓓ (دوائر)"),
        SQUARED("🆂🆀🆄🅰🆁🅴 (مربعات)"),
        SQUARED_WHITE("🄲🄾🄽🅃🅂 (مربعات بيضاء)"),
        MONOSPACE("𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎 (أحادي - Giveaway)"),
        MATH_BOLD("𝐁𝐨𝐥𝐝 (عريض ديسكورد)"),
        MATH_ITALIC("𝘐𝘵𝘢𝘭𝘪𝘤 (مائل ديسكورد)"),
        MATH_BOLD_ITALIC("𝘽𝙤𝙡𝙙 𝙄𝙩𝙖𝙡𝙞𝙘 (عريض مائل)"),
        SMALL_CAPS("sᴍᴀʟʟ ᴄᴀᴘs (حروف صغيرة)"),
        PARENTHESIZED("⒫⒜⒭⒠⒩ (أقواس)"),
        UNDERLINE("U̲n̲d̲e̲r̲l̲i̲n̲e̲ (مسطر)"),
        STRIKE_THROUGH("S̶t̶r̶i̶k̶e̶ (مشطوب)"),
        ARABIC_TASHKEEL("عربي مزخرف بالحركات ّ"),
        ARABIC_KASHIDA("عـربـي مـمـدود ـ"),
        ARABIC_BRACKETS("﴿ عربي مزخرف بالأقواس ﴾"),
        ARABIC_FLOWERS("✿ عربي مزخرف بالورود ✿")
    }

    // Helper tables for styles that are not simple math offsets or need custom maps
    private val cursiveUpper = "𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦X𝓨𝓩"
    private val cursiveLower = "𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃"

    private val gothicUpper = "𝔄𝔅𝔆𝔇𝔈𝔉𝔊𝔋𝔌𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔𝔕𝔖𝔗𝔘𝔙𝔚𝔛𝔜𝔝"
    private val gothicLower = "𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷"

    private val gothicBoldUpper = "𝕬𝕭𝕮𝕯𝕰𝕱𝕲𝕳𝕴𝕵𝕶𝕷𝕸𝕹𝕺𝕻𝕼𝕽𝕾𝕿𝖀𝖁𝖂𝖃𝖄𝖅"
    private val gothicBoldLower = "𝖆𝖇𝖈𝖉𝖊𝖋𝖌𝖍𝖎𝖏𝖐𝖑𝖒𝖓𝖔𝖕𝖖𝖗𝖘𝖙𝖚𝖛𝖜𝖝𝖞𝖟"

    private val doubleStruckUpper = "AFS" // Wait, certain double-struck letters are different
    private val doubleStruckUpperFull = "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ"
    private val doubleStruckLowerFull = "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫"

    private val circledUpper = "ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ"
    private val circledLower = "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ"

    private val squaredUpper = "🆂🆀🆄🅰🆁🅴🅵🅶🅷🅸🅹🅺🅻🅼🅽🅾🅿🆀🆁🆂🆃🆄🆅🆆🆇🆈🆉" // All squares are uppercase style
    private val squaredWhiteUpper = "🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉"

    private val monospaceUpper = "𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉"
    private val monospaceLower = "𝚊𝚋𝚌𝚍稳𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣" // Fix typo in standard monospace template
    private val monospaceLowerFull = "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣"

    private val mathBoldUpper = "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙"
    private val mathBoldLower = "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣ｋ𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳"
    private val mathBoldLowerFull = "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳"

    private val mathItalicUpper = "𝘈𝘉𝘊𝘋𝘌𝘍𝘎𝘏𝘐𝘑𝘒𝘓𝘔𝘕𝘖𝘗𝘘𝘙𝘚𝘛𝘜𝘝𝘞𝘟𝘠𝘡"
    private val mathItalicLower = "𝘢𝘣𝘤𝘥𝘦𝘧𝘨𝘩𝘪𝘫𝘬𝘭𝘮𝘯𝘰𝘱𝘲𝘳𝘴𝘵𝘶𝘷𝘸𝘹𝘺𝘻"

    private val mathBoldItalicUpper = "𝘼𝘽𝘾𝘿𝙀𝙁𝙂𝙃𝙄𝙅𝙆𝙇𝙈𝙉𝙊𝙋𝙌𝙍𝙎𝙏𝙐𝙑𝙒𝙓𝙔𝙕"
    private val mathBoldItalicLower = "𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠             " // Let's use clean map
    private val mathBoldItalicLowerFull = "𝙖𝙗𝙘发𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯"
    private val mathBoldItalicLowerClean = "𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯"

    private val smallCapsMap = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"

    private val parenthesizedUpper = "⒜⒝⒞⒟⒠⒡⒢⒣⒤⒥⒦⒧⒨⒩⒪⒫⒬⒭⒮⒯⒰⒱⒲⒳⒴⒵"

    /**
     * Translates a single character into the target style.
     */
    fun stylizeChar(char: Char, style: TextStyle): String {
        if (style == TextStyle.NORMAL || 
            style == TextStyle.ARABIC_TASHKEEL || 
            style == TextStyle.ARABIC_KASHIDA || 
            style == TextStyle.ARABIC_BRACKETS || 
            style == TextStyle.ARABIC_FLOWERS) {
            return char.toString()
        }

        if (style == TextStyle.UNDERLINE) {
            return char.toString() + "\u0332"
        }
        if (style == TextStyle.STRIKE_THROUGH) {
            return char.toString() + "\u0336"
        }

        val isUpper = char in 'A'..'Z'
        val isLower = char in 'a'..'z'
        val index = if (isUpper) char - 'A' else if (isLower) char - 'a' else -1

        if (index == -1) {
            // Numbers mapping
            if (char in '0'..'9') {
                val numIndex = char - '0'
                return when (style) {
                    TextStyle.CIRCLED -> {
                        if (numIndex == 0) "⓪" else listOf("①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨")[numIndex - 1]
                    }
                    TextStyle.DOUBLE_STRUCK -> {
                        listOf("𝟘", "𝟙", "𝟚", "𝟛", "𝟜", "𝟝", "𝟞", "𝟟", "𝟠", "𝟡")[numIndex]
                    }
                    TextStyle.MONOSPACE -> {
                        listOf("𝟶", "𝟷", "𝟸", "𝟹", "𝟺", "𝟻", "𝟼", "𝟽", "𝟾", "𝟿")[numIndex]
                    }
                    TextStyle.MATH_BOLD -> {
                        listOf("𝟎", "𝟏", "𝟐", "𝟑", "𝟒", "𝟓", "𝟔", "𝟕", "𝟖", "𝟗")[numIndex]
                    }
                    else -> char.toString()
                }
            }
            return char.toString()
        }

        return try {
            when (style) {
                TextStyle.CURSIVE -> {
                    if (isUpper) getUnicodeChar(cursiveUpper, index) else getUnicodeChar(cursiveLower, index)
                }
                TextStyle.GOTHIC -> {
                    if (isUpper) getUnicodeChar(gothicUpper, index) else getUnicodeChar(gothicLower, index)
                }
                TextStyle.GOTHIC_BOLD -> {
                    if (isUpper) getUnicodeChar(gothicBoldUpper, index) else getUnicodeChar(gothicBoldLower, index)
                }
                TextStyle.DOUBLE_STRUCK -> {
                    if (isUpper) getUnicodeChar(doubleStruckUpperFull, index) else getUnicodeChar(doubleStruckLowerFull, index)
                }
                TextStyle.CIRCLED -> {
                    if (isUpper) getUnicodeChar(circledUpper, index) else getUnicodeChar(circledLower, index)
                }
                TextStyle.SQUARED -> {
                    val sqAlpha = "🅰🅱🅲🅳🅴🅵🅶🅷🅸🅹🅺🅻🅼🅽🅾🅿🆀🆁🆂🆃🆄🆅🆆🆇🆈🆉"
                    getUnicodeChar(sqAlpha, index)
                }
                TextStyle.SQUARED_WHITE -> {
                    getUnicodeChar(squaredWhiteUpper, index)
                }
                TextStyle.MONOSPACE -> {
                    if (isUpper) getUnicodeChar(monospaceUpper, index) else getUnicodeChar(monospaceLowerFull, index)
                }
                TextStyle.MATH_BOLD -> {
                    if (isUpper) getUnicodeChar(mathBoldUpper, index) else getUnicodeChar(mathBoldLowerFull, index)
                }
                TextStyle.MATH_ITALIC -> {
                    if (isUpper) getUnicodeChar(mathItalicUpper, index) else getUnicodeChar(mathItalicLower, index)
                }
                TextStyle.MATH_BOLD_ITALIC -> {
                    if (isUpper) getUnicodeChar(mathBoldItalicUpper, index) else getUnicodeChar(mathBoldItalicLowerClean, index)
                }
                TextStyle.SMALL_CAPS -> {
                    // Small caps map is mostly uppercase styled single characters, let's map index
                    getUnicodeChar(smallCapsMap, index)
                }
                TextStyle.PARENTHESIZED -> {
                    getUnicodeChar(parenthesizedUpper, index)
                }
                else -> char.toString()
            }
        } catch (e: Exception) {
            char.toString()
        }
    }

    private fun getUnicodeChar(source: String, index: Int): String {
        var codePointIndex = 0
        var i = 0
        while (i < source.length) {
            val codePoint = source.codePointAt(i)
            if (codePointIndex == index) {
                return String(Character.toChars(codePoint))
            }
            codePointIndex++
            i += Character.charCount(codePoint)
        }
        return ""
    }

    /**
     * Stylizes an entire string.
     */
    fun stylizeText(text: String, style: TextStyle): String {
        if (style == TextStyle.NORMAL) return text

        // Handle Arabic Tashkeel
        if (style == TextStyle.ARABIC_TASHKEEL) {
            val tashkeel = listOf("ّ", "َ", "ُ", "ِ", "ً", "ٌ", "ٍ", "ْ", "ٰ")
            val sb = StringBuilder()
            for (char in text) {
                sb.append(char)
                // If it's an Arabic letter, insert a random tashkeel
                if (char in '\u0600'..'\u06FF' && char !in "،؟ء ") {
                    sb.append(tashkeel.random())
                }
            }
            return sb.toString()
        }

        // Handle Arabic Kashida (stretching connecting letters)
        if (style == TextStyle.ARABIC_KASHIDA) {
            val sb = StringBuilder()
            val connecting = "ب ت ث ج ح خ س ش ص ض ط ظ ع غ ف ق ك ل م ن ه ي"
            for (char in text) {
                sb.append(char)
                if (connecting.contains(char) && char != ' ') {
                    sb.append("ـ")
                }
            }
            return sb.toString()
        }

        // Handle Arabic Brackets
        if (style == TextStyle.ARABIC_BRACKETS) {
            return "﴿ $text ﴾"
        }

        // Handle Arabic Flowers
        if (style == TextStyle.ARABIC_FLOWERS) {
            return "✿ $text ✿"
        }

        // Standard English Character Stylizers
        val sb = java.lang.StringBuilder()
        for (char in text) {
            sb.append(stylizeChar(char, style))
        }
        return sb.toString()
    }
}

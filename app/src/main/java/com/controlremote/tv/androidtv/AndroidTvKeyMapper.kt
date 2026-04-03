package com.controlremote.tv.androidtv

import com.controlremote.androidtv.proto.RemoteKeyCode

/**
 * Mapea caracteres a [RemoteKeyCode] para enviar texto al Android TV / Google TV.
 */
object AndroidTvKeyMapper {

    fun charToKeyCode(c: Char): RemoteKeyCode? {
        val ch = c.lowercaseChar()
        return when {
            ch in 'a'..'z' -> letterToKey(ch)
            ch in '0'..'9' -> digitToKey(ch)
            else -> symbolToKey(c)
        }
    }

    private fun letterToKey(c: Char): RemoteKeyCode = when (c) {
        'a' -> RemoteKeyCode.KEYCODE_A
        'b' -> RemoteKeyCode.KEYCODE_B
        'c' -> RemoteKeyCode.KEYCODE_C
        'd' -> RemoteKeyCode.KEYCODE_D
        'e' -> RemoteKeyCode.KEYCODE_E
        'f' -> RemoteKeyCode.KEYCODE_F
        'g' -> RemoteKeyCode.KEYCODE_G
        'h' -> RemoteKeyCode.KEYCODE_H
        'i' -> RemoteKeyCode.KEYCODE_I
        'j' -> RemoteKeyCode.KEYCODE_J
        'k' -> RemoteKeyCode.KEYCODE_K
        'l' -> RemoteKeyCode.KEYCODE_L
        'm' -> RemoteKeyCode.KEYCODE_M
        'n' -> RemoteKeyCode.KEYCODE_N
        'o' -> RemoteKeyCode.KEYCODE_O
        'p' -> RemoteKeyCode.KEYCODE_P
        'q' -> RemoteKeyCode.KEYCODE_Q
        'r' -> RemoteKeyCode.KEYCODE_R
        's' -> RemoteKeyCode.KEYCODE_S
        't' -> RemoteKeyCode.KEYCODE_T
        'u' -> RemoteKeyCode.KEYCODE_U
        'v' -> RemoteKeyCode.KEYCODE_V
        'w' -> RemoteKeyCode.KEYCODE_W
        'x' -> RemoteKeyCode.KEYCODE_X
        'y' -> RemoteKeyCode.KEYCODE_Y
        'z' -> RemoteKeyCode.KEYCODE_Z
        else -> RemoteKeyCode.KEYCODE_UNKNOWN
    }

    private fun digitToKey(c: Char): RemoteKeyCode = when (c) {
        '0' -> RemoteKeyCode.KEYCODE_0
        '1' -> RemoteKeyCode.KEYCODE_1
        '2' -> RemoteKeyCode.KEYCODE_2
        '3' -> RemoteKeyCode.KEYCODE_3
        '4' -> RemoteKeyCode.KEYCODE_4
        '5' -> RemoteKeyCode.KEYCODE_5
        '6' -> RemoteKeyCode.KEYCODE_6
        '7' -> RemoteKeyCode.KEYCODE_7
        '8' -> RemoteKeyCode.KEYCODE_8
        '9' -> RemoteKeyCode.KEYCODE_9
        else -> RemoteKeyCode.KEYCODE_UNKNOWN
    }

    private fun symbolToKey(c: Char): RemoteKeyCode? = when (c) {
        ' ' -> RemoteKeyCode.KEYCODE_SPACE
        '\n' -> RemoteKeyCode.KEYCODE_ENTER
        '\t' -> RemoteKeyCode.KEYCODE_TAB
        ',' -> RemoteKeyCode.KEYCODE_COMMA
        '.' -> RemoteKeyCode.KEYCODE_PERIOD
        '-' -> RemoteKeyCode.KEYCODE_MINUS
        '=' -> RemoteKeyCode.KEYCODE_EQUALS
        '/' -> RemoteKeyCode.KEYCODE_SLASH
        '\\' -> RemoteKeyCode.KEYCODE_BACKSLASH
        ';' -> RemoteKeyCode.KEYCODE_SEMICOLON
        '\'' -> RemoteKeyCode.KEYCODE_APOSTROPHE
        '`' -> RemoteKeyCode.KEYCODE_GRAVE
        '[' -> RemoteKeyCode.KEYCODE_LEFT_BRACKET
        ']' -> RemoteKeyCode.KEYCODE_RIGHT_BRACKET
        '@' -> RemoteKeyCode.KEYCODE_AT
        '#' -> RemoteKeyCode.KEYCODE_POUND
        '*' -> RemoteKeyCode.KEYCODE_STAR
        '+' -> RemoteKeyCode.KEYCODE_PLUS
        else -> null
    }
}

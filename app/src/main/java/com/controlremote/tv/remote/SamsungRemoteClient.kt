package com.controlremote.tv.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP para la API de control remoto de muchas TVs Samsung (puerto 8001).
 * La TV y el teléfono deben estar en la misma red Wi‑Fi.
 *
 * Nota: modelos recientes pueden exigir emparejamiento en la TV la primera vez.
 */
class SamsungRemoteClient(
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun sendKey(ip: String, key: SamsungKey): Result<Unit> = withContext(Dispatchers.IO) {
        val base = ip.trim().removePrefix("http://").removePrefix("https://")
        val url = "http://$base:8001/api/v2/channels/samsung.remote.control"
        val json = """
            {
              "method": "ms.remote.control",
              "params": {
                "Cmd": "Click",
                "DataOfCmd": "${key.code}",
                "Option": "false",
                "TypeOfRemote": "SendRemoteKey"
              }
            }
        """.trimIndent()
        val body = json.toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }
            }
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }
}

enum class SamsungKey(val code: String) {
    DIGIT_0("KEY_0"),
    DIGIT_1("KEY_1"),
    DIGIT_2("KEY_2"),
    DIGIT_3("KEY_3"),
    DIGIT_4("KEY_4"),
    DIGIT_5("KEY_5"),
    DIGIT_6("KEY_6"),
    DIGIT_7("KEY_7"),
    DIGIT_8("KEY_8"),
    DIGIT_9("KEY_9"),
    POWER("KEY_POWER"),
    UP("KEY_UP"),
    DOWN("KEY_DOWN"),
    LEFT("KEY_LEFT"),
    RIGHT("KEY_RIGHT"),
    ENTER("KEY_ENTER"),
    BACK("KEY_RETURN"),
    HOME("KEY_HOME"),
    MENU("KEY_MENU"),
    VOL_UP("KEY_VOLUP"),
    VOL_DOWN("KEY_VOLDOWN"),
    MUTE("KEY_MUTE"),
    CH_UP("KEY_CHUP"),
    CH_DOWN("KEY_CHDOWN"),
    PLAY("KEY_PLAY"),
    PAUSE("KEY_PAUSE"),
    SOURCE("KEY_HDMI"),
    /** Búsqueda / asistente de voz en muchos modelos */
    SEARCH("KEY_SEARCH")
}

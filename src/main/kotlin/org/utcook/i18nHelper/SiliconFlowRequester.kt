package org.utcook.i18nHelper

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SiliconFlowRequester(private val token: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60 * 10, TimeUnit.SECONDS)
        .readTimeout(60 * 10, TimeUnit.SECONDS)
        .writeTimeout(60 * 10, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "https://api.siliconflow.cn/v1/chat/completions"

    /**
     * 发送请求到 SiliconFlow API。
     *
     * @param prompt 用户输入的内容
     */
    fun sendRequest(prompt: String, callback: () -> Unit) {
        // 构建请求体
        val jsonBody = """
            {
                "model": "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B",
                "messages": [
                    {"role": "user", "content": "$prompt"}
                ],
                "stream": false,
                "max_tokens": 4096,
                "min_p": 0.05,
                "stop": null,
                "temperature": 0.1,
                "top_p": 0.7,
                "top_k": 50,
                "frequency_penalty": 0.5,
                "n": 1,
                "response_format": {"type": "text"}
            }
        """.trimIndent()

        val requestBody = jsonBody.toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        // 构建请求
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 发送异步请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 处理请求失败的情况
                e.printStackTrace()
                println("请求失败: ${e.message}")
                callback()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("来自服务器的响应: $responseBody")
                } else {
                    // 处理响应失败的情况
                    println("响应失败: ${response.code}")
                    println("${response.body?.string()}")
                }
                callback()
            }
        })
    }

    fun sendRequestStream(prompt: String, onMessage: (String) -> Unit, onComplete: () -> Unit) {
        // 构建请求体
        val jsonBody = """
            {
                "model": "deepseek-ai/DeepSeek-R1-0528-Qwen3-8B",
                "messages": [
                    {"role": "user", "content": "$prompt"}
                ],
                "stream": true,
                "max_tokens": 4096,
                "min_p": 0.05,
                "stop": null,
                "temperature": 0.1,
                "top_p": 0.7,
                "top_k": 50,
                "frequency_penalty": 0.5,
                "n": 1,
                "response_format": {"type": "text"}
            }
        """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        // 构建请求
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 发送异步请求并处理流式响应
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 处理请求失败的情况
                e.printStackTrace()
                println("请求失败: ${e.message}")
                onComplete()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    // 处理响应失败的情况
                    println("响应失败: ${response.code}")
                    println("${response.body?.string()}")
                    onComplete()
                    return
                }

                // 读取流式响应
                response.body?.source()?.use { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                // 流式返回结束
                                break
                            }
                            try {
                                // 提取消息内容并回调
                                val message = JSONObject(data).getJSONArray("choices")
                                    .getJSONObject(0).getJSONObject("delta").optString("content", "")
                                
                                // 过滤掉仅包含换行符的消息
                                if (message.trim().isNotEmpty()) {
                                    onMessage(message)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                onComplete()
            }
        })
    }
}
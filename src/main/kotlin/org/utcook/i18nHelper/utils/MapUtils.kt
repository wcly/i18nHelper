package org.utcook.i18nHelper.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MapUtils {

    /**
     * 返回两个 Map 中键的差异，即存在于 map1 但不存在于 map2 的键值对。
     *
     * @param map1 第一个 Map，用于比较的基准
     * @param map2 第二个 Map，用于比较的目标
     * @return 包含 map1 中存在但 map2 中不存在的键值对的新 Map
     */
    fun <K, V> getDifferenceByKey(map1: Map<K, V>, map2: Map<K, V>): Map<K, V> {
        return map1.filterKeys { key -> key !in map2 }
    }

    /**
     * 将 JSON 字符串转换为 Map。
     *
     * @param jsonString 输入的 JSON 字符串。
     * @return 转换后的 Map。
     */
    fun jsonToMap(jsonString: String): Map<String, Any?> {
        val gson = Gson()
        val typeToken = object : TypeToken<Map<String, Any?>>() {}.type
        return gson.fromJson(
            jsonString.trim().removeSurrounding("```json", "```"),
            typeToken
        )
    }

    /**
     * 将 Map 转换为 JSON 字符串。
     *
     * @param map 输入的 Map。
     * @return 转换后的 JSON 字符串。
     */
    fun mapToJsonString(map: Map<*, *>): String {
        val gson = Gson()
        return gson.toJson(map)
    }
}
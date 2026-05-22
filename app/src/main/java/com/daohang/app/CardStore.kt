package com.daohang.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object CardStore {

    private const val PREFS_NAME = "daohang_cards"
    private const val KEY_CARDS = "cards"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCards(context: Context): MutableList<CardItem> {
        val json = getPrefs(context).getString(KEY_CARDS, null)
        if (json.isNullOrEmpty()) {
            return getDefaultCards().toMutableList()
        }
        val arr = JSONArray(json)
        val list = mutableListOf<CardItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(CardItem(
                title = obj.getString("title"),
                url = obj.getString("url"),
                iconName = obj.optString("iconName", ""),
                customImagePath = obj.optString("customImagePath", ""),
                isCustom = obj.optBoolean("isCustom", false),
                bgColor = obj.optString("bgColor", "")
            ))
        }
        return list
    }

    fun saveCards(context: Context, cards: List<CardItem>) {
        val arr = JSONArray()
        for (card in cards) {
            val obj = JSONObject()
            obj.put("title", card.title)
            obj.put("url", card.url)
            obj.put("iconName", card.iconName)
            obj.put("customImagePath", card.customImagePath)
            obj.put("isCustom", card.isCustom)
            obj.put("bgColor", card.bgColor)
            arr.put(obj)
        }
        getPrefs(context).edit().putString(KEY_CARDS, arr.toString()).apply()
    }

    fun addCard(context: Context, card: CardItem) {
        val cards = getCards(context)
        cards.add(card)
        saveCards(context, cards)
    }

    fun removeCard(context: Context, index: Int) {
        val cards = getCards(context)
        if (index in cards.indices) {
            cards.removeAt(index)
            saveCards(context, cards)
        }
    }

    private fun getDefaultCards(): List<CardItem> {
        return listOf(
            CardItem("房产", "https://pl.ytcbd.com", "house", "", false, "#DBEAFE"),
            CardItem("旅游", "https://ka.ytcbd.com", "lvyou", "", false, "#D1FAE5"),
            CardItem("大学", "https://dx.ytcbd.com", "daxue", "", false, "#FEE7C3"),
            CardItem("虫洞", "https://cd.ytcbd.com", "cdong", "", false, "#E9D5FF"),
            CardItem("乐消消", "https://xx.ytcbd.com", "lexiaoxiao", "", false, "#FCE7F3")
        )
    }
}
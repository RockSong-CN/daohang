package com.daohang.app

import java.io.Serializable

data class CardItem(
    val title: String,
    val url: String,
    val iconName: String = "",      // 内置图标名（house/lvyou/daxue/cdong）
    val customImagePath: String = "", // 用户自定义图片路径
    val isCustom: Boolean = false,
    val bgColor: String = ""
) : Serializable
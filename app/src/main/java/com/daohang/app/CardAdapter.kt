package com.daohang.app

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private val cards: MutableList<CardItem>,
    private val onCardClick: (CardItem) -> Unit,
    private val onCardDelete: (Int) -> Unit,
    private val onCardEdit: (Int) -> Unit
) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {

    private var editMode = false

    fun setEditMode(enabled: Boolean) {
        editMode = enabled
        notifyDataSetChanged()
    }

    fun isInEditMode(): Boolean = editMode

    fun hasCustomCards(): Boolean = cards.any { it.isCustom }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvTitle: TextView = view.findViewById(R.id.tvCardTitle)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val cardRoot: com.google.android.material.card.MaterialCardView = view.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = cards[position]

        // 设置标题
        holder.tvTitle.text = card.title

        // 设置背景色
        if (card.bgColor.isNotEmpty()) {
            try {
                holder.cardRoot.setCardBackgroundColor(Color.parseColor(card.bgColor))
            } catch (_: Exception) {}
        } else {
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#FFF1F5F9"))
        }

        // 设置图标
        if (card.customImagePath.isNotEmpty()) {
            try {
                val bmp = BitmapFactory.decodeFile(card.customImagePath)
                holder.ivIcon.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.ivIcon.setImageResource(R.drawable.ic_launcher_full)
            }
        } else if (card.iconName.isNotEmpty()) {
            val resId = holder.itemView.context.resources.getIdentifier(
                "ic_${card.iconName}", "drawable",
                holder.itemView.context.packageName
            )
            if (resId != 0) {
                holder.ivIcon.setImageResource(resId)
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_launcher_full)
            }
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_launcher_full)
        }

        // 编辑/删除按钮：只有自定义卡片才显示
        val canEdit = card.isCustom
        holder.btnEdit.visibility = if (editMode && canEdit) View.VISIBLE else View.GONE
        holder.btnDelete.visibility = if (editMode && canEdit) View.VISIBLE else View.GONE

        holder.btnEdit.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onCardEdit(pos)
            }
        }
        holder.btnDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onCardDelete(pos)
            }
        }

        // 点击事件
        holder.itemView.setOnClickListener {
            if (!editMode) {
                onCardClick(card)
            }
        }

        // 长按进入编辑模式（仅当有自定义卡片时）
        holder.itemView.setOnLongClickListener {
            if (!editMode && hasCustomCards()) {
                setEditMode(true)
            }
            true
        }
    }

    override fun getItemCount(): Int = cards.size
}
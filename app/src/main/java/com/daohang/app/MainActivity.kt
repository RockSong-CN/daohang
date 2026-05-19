package com.daohang.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private lateinit var cards: MutableList<CardItem>
    private var pendingImagePath: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingImagePath = copyImageToInternal(uri)
        }
    }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            exportBackup(uri)
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importBackup(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cards = CardStore.getCards(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        adapter = CardAdapter(cards,
            onCardClick = { card -> openWebView(card.title, card.url) },
            onCardDelete = { pos -> showDeleteDialog(pos) },
            onCardEdit = { pos -> showEditDialog(pos) }
        )
        recyclerView.adapter = adapter

        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                if (adapter.isInEditMode()) {
                    val child = rv.findChildViewUnder(e.x, e.y)
                    if (child == null) {
                        adapter.setEditMode(false)
                        return true
                    }
                }
                return false
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddDialog()
        }

        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            showSettingsMenu()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isInEditMode()) {
                    adapter.setEditMode(false)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        checkBackupTip()
    }

    private fun openWebView(title: String, url: String) {
        val intent = Intent(this, WebViewActivity::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_URL, url)
        }
        startActivity(intent)
    }

    private fun showAddDialog() {
        pendingImagePath = ""
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_card, null, false)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etUrl = view.findViewById<TextInputEditText>(R.id.etUrl)
        val btnPickImage = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickImage)

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)
            .setOnClickListener {
                val name = etName.text?.toString()?.trim() ?: ""
                val url = etUrl.text?.toString()?.trim() ?: ""

                if (name.isEmpty() || url.isEmpty()) {
                    return@setOnClickListener
                }

                val finalUrl = if (url.startsWith("http")) url else "https://$url"

                val card = CardItem(
                    title = name,
                    url = finalUrl,
                    customImagePath = pendingImagePath,
                    isCustom = true,
                    bgColor = ""
                )
                CardStore.addCard(this, card)
                cards.add(card)
                adapter.notifyItemInserted(cards.size - 1)
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun showEditDialog(pos: Int) {
        val card = cards[pos]
        pendingImagePath = card.customImagePath

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_card, null, false)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etUrl = view.findViewById<TextInputEditText>(R.id.etUrl)
        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnPickImage = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPickImage)

        etName.setText(card.title)
        etUrl.setText(card.url)
        if (card.customImagePath.isNotEmpty()) {
            try {
                val bmp = BitmapFactory.decodeFile(card.customImagePath)
                ivPreview.setImageBitmap(bmp)
            } catch (_: Exception) {}
        }

        view.findViewById<android.widget.TextView>(R.id.tvDialogTitle)?.text = "编辑导航"
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)?.text = "保存"

        btnPickImage.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)
            .setOnClickListener {
                val name = etName.text?.toString()?.trim() ?: ""
                val url = etUrl.text?.toString()?.trim() ?: ""

                if (name.isEmpty() || url.isEmpty()) {
                    return@setOnClickListener
                }

                val finalUrl = if (url.startsWith("http")) url else "https://$url"

                val updatedCard = card.copy(
                    title = name,
                    url = finalUrl,
                    customImagePath = pendingImagePath
                )
                cards[pos] = updatedCard
                CardStore.saveCards(this, cards)
                adapter.notifyItemChanged(pos)
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun showDeleteDialog(pos: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除导航")
            .setMessage("确定删除「${cards[pos].title}」吗？")
            .setPositiveButton("删除") { _, _ ->
                CardStore.removeCard(this, pos)
                cards.removeAt(pos)
                adapter.notifyItemRemoved(pos)
                adapter.notifyItemRangeChanged(pos, cards.size - pos)
                if (!adapter.hasCustomCards()) {
                    adapter.setEditMode(false)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun copyImageToInternal(uri: Uri): String {
        val dir = File(filesDir, "icons")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${System.currentTimeMillis()}.png")
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    // ========== 设置菜单 ==========
    private fun showSettingsMenu() {
        val items = arrayOf("导出备份", "导入恢复")
        AlertDialog.Builder(this)
            .setTitle("数据管理")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> doExport()
                    1 -> doImport()
                }
            }
            .show()
    }

    // ========== 导出备份 ==========
    private fun doExport() {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        exportLauncher.launch("导航备份_$date.json")
    }

    private fun exportBackup(uri: Uri) {
        try {
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
            val root = JSONObject()
            root.put("version", 1)
            root.put("cards", arr)
            root.put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()))

            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(root.toString().toByteArray())
            }

            getSharedPreferences("daohang_prefs", Context.MODE_PRIVATE)
                .edit().putLong("last_backup", Date().time).apply()

            Toast.makeText(this, "已导出备份", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== 导入恢复 ==========
    private fun doImport() {
        importLauncher.launch(arrayOf("application/json"))
    }

    private fun importBackup(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (json.isNullOrEmpty()) {
                Toast.makeText(this, "文件为空", Toast.LENGTH_SHORT).show()
                return
            }
            val root = JSONObject(json)
            val arr = root.optJSONArray("cards")
            if (arr == null) {
                Toast.makeText(this, "文件格式不正确", Toast.LENGTH_SHORT).show()
                return
            }

            val importCards = mutableListOf<CardItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                importCards.add(CardItem(
                    title = obj.getString("title"),
                    url = obj.getString("url"),
                    iconName = obj.optString("iconName", ""),
                    customImagePath = obj.optString("customImagePath", ""),
                    isCustom = obj.optBoolean("isCustom", false),
                    bgColor = obj.optString("bgColor", "")
                ))
            }

            val customCards = importCards.filter { it.isCustom }
            if (customCards.isEmpty()) {
                Toast.makeText(this, "文件中没有自定义导航", Toast.LENGTH_SHORT).show()
                return
            }

            // 智能合并：URL + 名称双重判重
            val existUrls = cards.map { it.url }.toMutableSet()
            val existTitles = cards.map { it.title }.toMutableSet()
            var addCount = 0
            for (c in customCards) {
                if (!existUrls.contains(c.url) && !existTitles.contains(c.title)) {
                    val newCard = if (c.customImagePath.isNotEmpty()) {
                        val srcFile = File(c.customImagePath)
                        if (srcFile.exists()) c else c.copy(customImagePath = "")
                    } else {
                        c
                    }
                    cards.add(newCard)
                    existUrls.add(c.url)
                    existTitles.add(c.title)
                    addCount++
                }
            }

            if (addCount > 0) {
                CardStore.saveCards(this, cards)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "已恢复 $addCount 个导航", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "所有导航已存在，无需恢复", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败：文件格式错误", Toast.LENGTH_SHORT).show()
        }
    }

    // ========== 备份提醒 ==========
    private fun checkBackupTip() {
        val customCount = cards.count { it.isCustom }
        val lastBackup = getSharedPreferences("daohang_prefs", Context.MODE_PRIVATE)
            .getLong("last_backup", 0)
        if (customCount >= 3 && lastBackup == 0L) {
            Toast.makeText(this, "您已添加多个导航，建议导出备份防止数据丢失", Toast.LENGTH_LONG).show()
        }
    }
}
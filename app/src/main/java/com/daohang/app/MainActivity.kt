package com.daohang.app

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cards = CardStore.getCards(this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

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

        // 填充现有数据
        etName.setText(card.title)
        etUrl.setText(card.url)
        if (card.customImagePath.isNotEmpty()) {
            try {
                val bmp = BitmapFactory.decodeFile(card.customImagePath)
                ivPreview.setImageBitmap(bmp)
            } catch (_: Exception) {}
        }

        // 修改标题
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
}
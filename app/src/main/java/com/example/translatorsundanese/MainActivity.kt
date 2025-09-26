package com.example.translatorsundanese

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener   // ⬅️ PENTING: import ekstensi KTX
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope

class MainActivity : AppCompatActivity() {

    private lateinit var api: TranslateApi

    // cache untuk tombol Ulang
    private var lastQuery: String? = null
    private var lastTo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        api = provideApi()

        val spDirection = findViewById<Spinner>(R.id.spDirection)
        val etInput = findViewById<EditText>(R.id.etInput)
        val btnTranslate = findViewById<Button>(R.id.btnTranslate)
        val btnRetry = findViewById<Button>(R.id.btnRetry)
        val btnSwap = findViewById<Button>(R.id.btnSwap)
        val btnPaste = findViewById<Button>(R.id.btnPaste)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)
        val btnShare = findViewById<Button>(R.id.btnShare)
        val tv = findViewById<TextView>(R.id.tvResult)
        val tvCounter = findViewById<TextView>(R.id.tvCounter)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val clock = findViewById<TextView>(R.id.tvClock)

        // isi spinner
        val dirs = arrayOf("Indonesia → Sunda", "Sunda → Indonesia")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dirs).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spDirection.adapter = adapter

        // counter karakter
        etInput.addTextChangedListener {
            val n = it?.length ?: 0
            tvCounter.text = "$n chars"
        }

        // paste
        btnPaste.setOnClickListener {
            val pasted = pasteFromClipboard()?.trim()
            if (!pasted.isNullOrEmpty()) {
                etInput.setText(pasted)
                etInput.setSelection(pasted.length)
                toast("Pasted dari clipboard")
            } else toast("Clipboard kosong")
        }

        // clear
        btnClear.setOnClickListener {
            etInput.setText("")
            tv.text = ""
        }

        // swap arah
        btnSwap.setOnClickListener {
            spDirection.setSelection(if (spDirection.selectedItemPosition == 0) 1 else 0)
        }

        // copy hasil
        btnCopy.setOnClickListener {
            val out = tv.text.toString().trim()
            if (out.isEmpty()) toast("Belum ada hasil") else {
                copyToClipboard("Terjemahan", out)
                toast("Hasil disalin")
            }
        }

        // share hasil
        btnShare.setOnClickListener {
            val out = tv.text.toString().trim()
            if (out.isEmpty()) toast("Belum ada hasil") else shareText(out)
        }

        // translate
        btnTranslate.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) {
                tv.text = "Teks masih kosong."
                return@setOnClickListener
            }
            val to = if (spDirection.selectedItemPosition == 0) "su" else "id"
            lastQuery = text
            lastTo = to
            doTranslate(text, to, progress, btnTranslate, tv)
        }

        // ulang request terakhir
        btnRetry.setOnClickListener {
            val q = lastQuery?.trim()
            val t = lastTo?.trim()
            if (q.isNullOrEmpty() || t.isNullOrEmpty()) toast("Belum ada request sebelumnya")
            else doTranslate(q, t, progress, btnTranslate, tv)
        }

        // clock (lifecycle-aware, tidak bocor)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                while (true) {
                    clock.text = sdf.format(Date())
                    delay(1000)
                }
            }
        }
    }

    private fun doTranslate(
        text: String,
        to: String,
        progress: ProgressBar,
        btnTranslate: Button,
        tv: TextView
    ) {
        setLoading(true, progress, btnTranslate)
        tv.text = ""

        lifecycleScope.launch {
            try {
                val res = api.translate(text = text, to = to) // engine default google
                if (!res.isSuccessful) {
                    tv.text = "Gagal: HTTP ${res.code()}"
                    return@launch
                }
                val translated = res.body()?.data?.result
                tv.text = if (translated.isNullOrEmpty()) "Tidak ada hasil." else translated
            } catch (e: Exception) {
                tv.text = "Error: ${e.message ?: "tidak diketahui"}"
            } finally {
                setLoading(false, progress, btnTranslate)
            }
        }
    }

    private fun setLoading(isLoading: Boolean, progress: ProgressBar, btnTranslate: Button) {
        progress.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnTranslate.isEnabled = !isLoading
        btnTranslate.text = if (isLoading) "Menerjemahkan…" else "Terjemahkan"
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun pasteFromClipboard(): String? {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(this)?.toString()
    }

    private fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Bagikan melalui"))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

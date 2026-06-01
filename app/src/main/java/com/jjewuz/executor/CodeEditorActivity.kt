package com.jjewuz.executor

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jjewuz.executor.databinding.ActivityCodeEditorBinding
import com.jjewuz.executor.service.ExecutorService
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.File
import androidx.core.net.toUri

class CodeEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_URI = "file_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_FOLDER_URI = "folder_uri"
        const val EXTRA_FOLDER_DOC_ID = "folder_doc_id"
    }

    private lateinit var binding: ActivityCodeEditorBinding
    private var fileUri: Uri? = null
    private var fileName: String = "untitled.py"
    private var folderUri: Uri? = null
    private var folderDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodeEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updatePadding(bottom = maxOf(ime, nav))
            insets
        }

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fileUri = intent.getStringExtra(EXTRA_FILE_URI)?.toUri()
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "untitled.py"
        folderUri = intent.getStringExtra(EXTRA_FOLDER_URI)?.toUri()
        folderDocId = intent.getStringExtra(EXTRA_FOLDER_DOC_ID)

        binding.topAppBar.title = fileName
        setupEditor()
        setupSymbolBar()
        loadFile()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { confirmExit() }
        })
    }

    private fun setupEditor() {
        val editor = binding.codeEditor

        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))

        val themeRegistry = ThemeRegistry.getInstance()
        try {
            val inputStream = FileProviderRegistry.getInstance().tryGetInputStream("textmate/darcula.json")
            val themeSource = IThemeSource.fromInputStream(inputStream, "darcula.json", null)
            themeRegistry.loadTheme(ThemeModel(themeSource, "darcula"))
        } catch (_: Throwable) {}
        themeRegistry.setTheme("darcula")

        try {
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        } catch (_: Throwable) {}

        editor.colorScheme = TextMateColorScheme.create(themeRegistry)
        editor.setEditorLanguage(TextMateLanguage.create("source.python", true))

        editor.isWordwrap = false
        editor.props.deleteEmptyLineFast = false
        editor.setTextSize(14f)
        editor.setPinLineNumber(true)
    }

    private fun setupSymbolBar() {
        val symbols = listOf(
            "⇥" to "\t",
            ":" to ":",
            "(" to "(",
            ")" to ")",
            "[" to "[",
            "]" to "]",
            "{" to "{",
            "}" to "}",
            "\"" to "\"",
            "'" to "'",
            "#" to "#",
            "=" to "=",
            "," to ",",
            "." to ".",
            "→" to "->",
            "!=" to "!=",
            "==" to "==",
            "_" to "_",
            "+" to "+",
            "-" to "-",
            "*" to "*",
            "**" to "**",
            "/" to "/",
            "%" to "%",
            "\\" to "\\"
        )

        val bar = binding.symbolBar
        val dp = resources.displayMetrics.density

        symbols.forEach { (label, insert) ->
            val btn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                textSize = 13f
                minWidth = 0
                minimumWidth = 0
                minimumHeight = 0
                minHeight = 0
                setPadding((10 * dp).toInt(), 0, (10 * dp).toInt(), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).apply { setMargins((2 * dp).toInt(), (4 * dp).toInt(), (2 * dp).toInt(), (4 * dp).toInt()) }
                setOnClickListener { insertSymbol(insert) }
            }
            bar.addView(btn)
        }
    }

    private fun insertSymbol(text: String) {
        val editor = binding.codeEditor
        val cursor = editor.cursor
        editor.text.insert(cursor.leftLine, cursor.leftColumn, text)
    }

    private fun loadFile() {
        val uri = fileUri ?: return // new file — start empty
        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                try {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                } catch (e: Exception) {
                    ""
                }
            }
            binding.codeEditor.setText(content)
        }
    }

    private fun saveFile() {
        val code = binding.codeEditor.text.toString()
        lifecycleScope.launch {
            val error = withContext(Dispatchers.IO) {
                try {
                    val fUri = folderUri ?: return@withContext getString(R.string.no_scripts_dir)
                    val fDocId = folderDocId ?: return@withContext "No folder"

                    val targetUri = fileUri ?: run {
                        // create new document in folder
                        val parentUri = DocumentsContract.buildDocumentUriUsingTree(fUri, fDocId)
                        DocumentsContract.createDocument(
                            contentResolver, parentUri, "text/x-python", fileName
                        ) ?: return@withContext getString(R.string.editor_save_error)
                    }

                    // write to SAF
                    contentResolver.openOutputStream(targetUri, "wt")?.use { it.write(code.toByteArray()) }
                    fileUri = targetUri

                    // copy to internal scripts dir so the service picks it up
                    val dest = File(filesDir, "scripts/$fileName")
                    dest.parentFile?.mkdirs()
                    dest.writeText(code)

                    null
                } catch (e: Exception) {
                    e.message ?: e.toString()
                }
            }
            if (error == null) {
                try { ExecutorService.reloadScripts() } catch (_: Throwable) {}
                Snackbar.make(binding.root, getString(R.string.editor_saved), Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { confirmExit(); true }
            R.id.action_save -> { saveFile(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmExit() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.editor_exit_title)
            .setMessage(R.string.editor_exit_message)
            .setPositiveButton(R.string.editor_save_exit) { _, _ -> saveFile(); finish() }
            .setNegativeButton(R.string.editor_discard) { _, _ -> finish() }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.codeEditor.release()
    }
}

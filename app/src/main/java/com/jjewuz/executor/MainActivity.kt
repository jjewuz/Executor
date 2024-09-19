package com.jjewuz.executor

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jjewuz.executor.databinding.ActivityMainBinding
import com.jjewuz.executor.service.CommandModule
import com.jjewuz.executor.service.ExecutorService
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        const val REQUEST_CODE_OPEN_DIRECTORY = 42
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
        }
        setSupportActionBar(binding.topAppBar)


        createNotificationChannel()

        binding.accessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        binding.settings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri = Uri.fromParts("package", applicationContext.packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        binding.scriptLoad.setOnClickListener {
            openDirectory()
        }
    }

    private fun openDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == Activity.RESULT_OK) {
            val directoryUri: Uri? = data?.data
            if (directoryUri != null) {
                // Сохранение разрешений на доступ к папке
                contentResolver.takePersistableUriPermission(
                    directoryUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                // Теперь можем загрузить скрипты
                loadUserScripts(directoryUri)
                Log.d("MainActivity", "Started")
            }
        }
    }

    private fun loadUserScripts(directoryUri: Uri) {

        val scriptDir = File(filesDir, "scripts")
        if (scriptDir.exists()) {
            scriptDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "py") {
                    file.delete()
                    Log.d("MainActivity", "Deleted old script: ${file.name}")
                }
            }
        }

        // Добавляем путь к пользовательским скриптам в sys.path
        contentResolver.takePersistableUriPermission(
            directoryUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        Log.d("MainActivity", directoryUri.path.toString())

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            directoryUri,
            DocumentsContract.getTreeDocumentId(directoryUri)
        )

        contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val displayName = cursor.getString(1)
                val mimeType = cursor.getString(2)
                Log.d("MainActivity", "File: $displayName, MIME Type: $mimeType")

                if (mimeType == "application/x-python-code" || displayName.endsWith(".py")) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                    try {
                        val inputStream = contentResolver.openInputStream(fileUri)

                        inputStream?.use {
                            val internalFile = File(filesDir, "scripts/$displayName")
                            internalFile.parentFile?.mkdirs()
                            internalFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        ExecutorService().loadInternalScripts()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Failed to load script $displayName: ${e.message}")
                    }
                }
            }
        }
    }


    private fun createNotificationChannel() {
        val name = resources.getString(R.string.notificationName)
        val descriptionText = resources.getString(R.string.notificationDesc)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("123", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.nav_menu, menu)

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.info -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.information)
                    .setIcon(R.drawable.info)
                    .setMessage("Executor v${BuildConfig.VERSION_NAME}")
                    .setPositiveButton("OK") {_, _ ->
                    }
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
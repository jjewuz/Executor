package com.jjewuz.executor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jjewuz.executor.databinding.FragmentHomeBinding
import com.jjewuz.executor.service.ExecutorService
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences

    private val openDirLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            sharedPreferences.edit().putString("scripts_dir_uri", it.toString()).apply()
            loadUserScripts(it)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedPreferences = requireActivity().getSharedPreferences("keys", Context.MODE_PRIVATE)

        binding.accessibility.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.acces_service)
                .setIcon(R.drawable.info)
                .setCancelable(false)
                .setMessage(R.string.acces_desc)
                .setPositiveButton("OK") { _, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }

        binding.settings.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        binding.scriptLoad.setOnClickListener {
            openDirLauncher.launch(null)
        }

        binding.reloadModules.setOnClickListener {
            if (ExecutorService.getInstance() == null) {
                Snackbar.make(binding.root, R.string.service_not_running, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val savedUri = sharedPreferences.getString("scripts_dir_uri", null)?.let { Uri.parse(it) }
            if (savedUri != null) {
                loadUserScripts(savedUri)
                Snackbar.make(binding.root, R.string.modules_reloaded, Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(binding.root, R.string.no_scripts_dir, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateReloadButtonVisibility()
    }

    private fun updateReloadButtonVisibility() {
        val visibility = if (ExecutorService.getInstance() != null) View.VISIBLE else View.GONE
        binding.reloadModules.visibility = visibility
        binding.reloadModulesDesc.visibility = visibility
    }

    private fun loadUserScripts(directoryUri: Uri) {
        val scriptDir = File(requireContext().filesDir, "scripts")
        if (scriptDir.exists()) {
            scriptDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "py") {
                    file.delete()
                }
            }
        }

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            directoryUri,
            DocumentsContract.getTreeDocumentId(directoryUri)
        )

        requireContext().contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val displayName = cursor.getString(1)
                val mimeType = cursor.getString(2)

                if (mimeType == "application/x-python-code" || displayName.endsWith(".py")) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId)
                    try {
                        requireContext().contentResolver.openInputStream(fileUri)?.use { input ->
                            val internalFile = File(requireContext().filesDir, "scripts/$displayName")
                            internalFile.parentFile?.mkdirs()
                            internalFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Failed to load script $displayName: ${e.message}")
                    }
                }
            }
        }
        // reload once after all files are copied
        ExecutorService.reloadScripts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

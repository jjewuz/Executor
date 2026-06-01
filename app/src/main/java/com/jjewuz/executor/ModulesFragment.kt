package com.jjewuz.executor

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.jjewuz.executor.databinding.FragmentModulesBinding
import com.jjewuz.executor.service.ExecutorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri

class ModulesFragment : Fragment() {

    private var _binding: FragmentModulesBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val LIST_URL = "https://executor.jjewuz.com/list.json"
        private const val FILES_BASE_URL = "https://executor.jjewuz.com/modules/"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentModulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        loadModules()
    }

    private fun loadModules() {
        binding.progress.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { fetchModules() }
            binding.progress.visibility = View.GONE
            result.onSuccess { modules ->
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.adapter = ModuleAdapter(
                    modules,
                    isInstalled = { isInstalled(it) },
                    onDownload = { downloadModule(it) }
                )
            }.onFailure { e ->
                binding.errorText.visibility = View.VISIBLE
                binding.errorText.text = getString(R.string.modules_load_error, e.message)
            }
        }
    }

    private fun fetchModules(): Result<List<Module>> {
        return try {
            val conn = URL(LIST_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            val json = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val arr = JSONArray(json)
            val list = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Module(
                    name = obj.optString("name"),
                    author = obj.optString("author"),
                    description = obj.optString("description"),
                    versions = obj.optString("versions"),
                    updated = obj.optString("updated"),
                    file = obj.optString("file")
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isInstalled(module: Module): Boolean {
        return File(requireContext().filesDir, "scripts/${module.file}").exists()
    }

    private fun downloadModule(module: Module) {
        val prefs = requireActivity().getSharedPreferences("keys", Context.MODE_PRIVATE)
        val folderUri = prefs.getString("scripts_dir_uri", null)?.let { it.toUri() }

        if (folderUri == null) {
            Snackbar.make(binding.root, R.string.no_scripts_dir, Snackbar.LENGTH_LONG).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val error = withContext(Dispatchers.IO) {
                try {
                    val conn = URL("$FILES_BASE_URL${module.file}").openConnection() as HttpURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 30_000
                    val bytes = conn.inputStream.readBytes()
                    conn.disconnect()

                    saveToSafFolder(folderUri, module.file, bytes)

                    val dest = File(requireContext().filesDir, "scripts/${module.file}")
                    dest.parentFile?.mkdirs()
                    dest.writeBytes(bytes)
                    null
                } catch (e: Exception) {
                    e.message ?: e.toString()
                }
            }
            if (error == null) {
                try { ExecutorService.reloadScripts() } catch (_: Throwable) {}
                Snackbar.make(binding.root, getString(R.string.module_downloaded, module.name), Snackbar.LENGTH_SHORT).show()
                binding.recyclerView.adapter?.notifyDataSetChanged()
            } else {
                Snackbar.make(binding.root, getString(R.string.module_download_error, error), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToSafFolder(folderUri: Uri, filename: String, bytes: ByteArray) {
        val cr = requireContext().contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, treeDocId)

        var existingDocId: String? = null
        cr.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == filename) {
                    existingDocId = cursor.getString(0)
                    break
                }
            }
        }

        val fileUri = if (existingDocId != null) {
            DocumentsContract.buildDocumentUriUsingTree(folderUri, existingDocId!!)
        } else {
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, treeDocId)
            DocumentsContract.createDocument(cr, parentUri, "text/x-python", filename)
                ?: throw Exception("Failed to create document in selected folder")
        }

        cr.openOutputStream(fileUri, "wt")?.use { it.write(bytes) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

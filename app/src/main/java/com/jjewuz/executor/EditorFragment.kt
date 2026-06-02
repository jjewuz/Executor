package com.jjewuz.executor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.jjewuz.executor.databinding.FragmentEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!

    // navigation stack: list of (folderUri, folderId, displayName)
    private data class FolderEntry(val folderUri: Uri, val docId: String, val name: String)
    private val folderStack = mutableListOf<FolderEntry>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            if (folderStack.size > 1) {
                folderStack.removeAt(folderStack.lastIndex)
                loadCurrentFolder()
            }
        }

        binding.fab.setOnClickListener { showNewFileDialog() }

        loadRootFolder()
    }

    override fun onResume() {
        super.onResume()
        // refresh in case files changed
        if (folderStack.isNotEmpty()) loadCurrentFolder()
    }

    private fun loadRootFolder() {
        val prefs = requireActivity().getSharedPreferences("keys", Context.MODE_PRIVATE)
        val uriString = prefs.getString("scripts_dir_uri", null)
        if (uriString == null) {
            binding.emptyText.visibility = View.VISIBLE
            binding.emptyText.text = getString(R.string.no_scripts_dir)
            binding.recyclerView.visibility = View.GONE
            return
        }
        val folderUri = Uri.parse(uriString)
        val rootDocId = DocumentsContract.getTreeDocumentId(folderUri)
        folderStack.clear()
        folderStack.add(FolderEntry(folderUri, rootDocId, getString(R.string.editor_scripts_folder)))
        loadCurrentFolder()
    }

    private fun loadCurrentFolder() {
        val current = folderStack.last()
        binding.currentPath.text = folderStack.joinToString(" / ") { it.name }
        binding.btnBack.visibility = if (folderStack.size > 1) View.VISIBLE else View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { listFolder(current.folderUri, current.docId) }
            if (items.isEmpty()) {
                binding.emptyText.visibility = View.VISIBLE
                binding.emptyText.text = getString(R.string.editor_empty_folder)
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyText.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerView.adapter = FileAdapter(items) { item -> onItemClick(item) }
            }
        }
    }

    private fun listFolder(folderUri: Uri, docId: String): List<FileItem> {
        val cr = requireContext().contentResolver
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folderUri, docId)
        val items = mutableListOf<FileItem>()

        cr.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                val mime = cursor.getString(2)
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                if (isDir || name.endsWith(".py")) {
                    items.add(FileItem(name, id, isDir))
                }
            }
        }
        return items.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    private fun onItemClick(item: FileItem) {
        val current = folderStack.last()
        if (item.isDirectory) {
            folderStack.add(FolderEntry(current.folderUri, item.documentId, item.name))
            loadCurrentFolder()
        } else {
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(current.folderUri, item.documentId)
            openEditor(fileUri, item.name, current.folderUri, current.docId)
        }
    }

    private fun openEditor(fileUri: Uri?, fileName: String, folderUri: Uri, folderDocId: String) {
        val intent = Intent(requireContext(), CodeEditorActivity::class.java).apply {
            putExtra(CodeEditorActivity.EXTRA_FILE_URI, fileUri?.toString())
            putExtra(CodeEditorActivity.EXTRA_FILE_NAME, fileName)
            putExtra(CodeEditorActivity.EXTRA_FOLDER_URI, folderUri.toString())
            putExtra(CodeEditorActivity.EXTRA_FOLDER_DOC_ID, folderDocId)
        }
        startActivity(intent)
    }

    private fun showNewFileDialog() {
        val current = folderStack.lastOrNull() ?: return

        val layout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.editor_new_file_hint)
            setPadding(48, 16, 48, 0)
        }
        val input = TextInputEditText(requireContext())
        layout.addView(input)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.editor_new_file)
            .setView(layout)
            .setPositiveButton(R.string.editor_create) { _, _ ->
                var name = input.text?.toString()?.trim() ?: return@setPositiveButton
                if (!name.endsWith(".py")) name += ".py"
                openEditor(null, name, current.folderUri, current.docId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

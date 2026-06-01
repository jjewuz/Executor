package com.jjewuz.executor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jjewuz.executor.databinding.ItemModuleBinding

class ModuleAdapter(
    private val modules: List<Module>,
    private val isInstalled: (Module) -> Boolean,
    private val onDownload: (Module) -> Unit
) : RecyclerView.Adapter<ModuleAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemModuleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemModuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val module = modules[position]
        with(holder.binding) {
            moduleName.text = module.name
            moduleAuthor.text = module.author
            moduleDesc.text = module.description
            moduleUpdated.text = module.updated
            moduleVersions.text = root.context.getString(R.string.module_versions, module.versions)
            updateDownloadBtn(holder, module)
            downloadBtn.setOnClickListener {
                onDownload(module)
                updateDownloadBtn(holder, module)
            }
        }
    }

    private fun updateDownloadBtn(holder: ViewHolder, module: Module) {
        val installed = isInstalled(module)
        holder.binding.downloadBtn.text = holder.binding.root.context.getString(
            if (installed) R.string.module_update else R.string.module_download
        )
    }

    override fun getItemCount() = modules.size
}

package com.jjewuz.executor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileAdapter(
    private val items: List<FileItem>,
    private val onClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.fileIcon)
        val name: TextView = view.findViewById(R.id.fileName)
        val chevron: ImageView = view.findViewById(R.id.chevron)
        val root: View = view.findViewById(R.id.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.icon.text = if (item.isDirectory) "📁" else "🐍"
        holder.name.text = item.name
        holder.chevron.visibility = if (item.isDirectory) View.VISIBLE else View.GONE
        holder.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}

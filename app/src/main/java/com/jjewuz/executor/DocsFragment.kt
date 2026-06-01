package com.jjewuz.executor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jjewuz.executor.databinding.ItemDocBinding
import com.jjewuz.executor.databinding.FragmentDocsBinding

class DocsFragment : Fragment() {

    private var _binding: FragmentDocsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDocsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = DocListAdapter(DocContent.pages) { item ->
            val intent = Intent(requireContext(), DocDetailActivity::class.java)
            intent.putExtra(DocDetailActivity.EXTRA_DOC_ID, item.id)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DocListAdapter(
    private val items: List<DocItem>,
    private val onClick: (DocItem) -> Unit
) : RecyclerView.Adapter<DocListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDocBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDocBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.docTitle.text = holder.binding.root.context.getString(item.titleRes)
        holder.binding.card.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}

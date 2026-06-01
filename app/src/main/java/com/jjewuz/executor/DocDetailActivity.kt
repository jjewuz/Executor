package com.jjewuz.executor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jjewuz.executor.databinding.ActivityDocDetailBinding

class DocDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DOC_ID = "doc_id"
    }

    private lateinit var binding: ActivityDocDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val docId = intent.getStringExtra(EXTRA_DOC_ID) ?: return
        val page = DocContent.findById(docId) ?: return

        binding.topAppBar.title = getString(page.titleRes)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = DocSectionAdapter(page.sections)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}

class DocSectionAdapter(private val sections: List<DocSection>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_CODE = 1
    }

    override fun getItemViewType(position: Int) =
        if (sections[position] is DocSection.Text) TYPE_TEXT else TYPE_CODE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TEXT) {
            val view = inflater.inflate(R.layout.item_doc_section_text, parent, false)
            TextHolder(view as TextView)
        } else {
            val root = inflater.inflate(R.layout.item_doc_section_code, parent, false)
            CodeHolder(root, root.findViewById(R.id.code))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val section = sections[position]) {
            is DocSection.Text -> (holder as TextHolder).tv.setText(section.textRes)
            is DocSection.Code -> (holder as CodeHolder).tv.text = section.code
        }
    }

    override fun getItemCount() = sections.size

    class TextHolder(val tv: TextView) : RecyclerView.ViewHolder(tv)
    class CodeHolder(root: android.view.View, val tv: TextView) : RecyclerView.ViewHolder(root)
}

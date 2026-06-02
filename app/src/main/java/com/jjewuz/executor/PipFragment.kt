package com.jjewuz.executor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.jjewuz.executor.databinding.FragmentPipBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class PipFragment : Fragment() {

    private var _binding: FragmentPipBinding? = null
    private val binding get() = _binding!!

    data class Package(val name: String, val version: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.installedList.layoutManager = LinearLayoutManager(requireContext())

        binding.installBtn.setOnClickListener {
            val packageName = binding.packageInput.text?.toString()?.trim()
            if (!packageName.isNullOrEmpty()) {
                installPackage(packageName)
            }
        }

        loadInstalled()
    }

    private fun installPackage(packageName: String) {
        val targetDir = File(requireContext().filesDir, "pip-packages").also { it.mkdirs() }

        binding.installBtn.isEnabled = false
        binding.outputLabel.visibility = View.VISIBLE
        binding.outputText.visibility = View.VISIBLE
        binding.outputText.text = getString(R.string.pip_installing, packageName)

        viewLifecycleOwner.lifecycleScope.launch {
            val output = withContext(Dispatchers.IO) {
                runPip("install", "--target", targetDir.absolutePath, packageName)
            }
            binding.outputText.text = output
            binding.installBtn.isEnabled = true
            loadInstalled()
        }
    }

    private fun loadInstalled() {
        binding.installedProgress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val packages = withContext(Dispatchers.IO) { fetchInstalled() }
            binding.installedProgress.visibility = View.GONE
            binding.installedHeader.text = getString(R.string.pip_installed_title_count, packages.size)
            binding.installedList.adapter = PackageAdapter(packages)
        }
    }

    private fun fetchInstalled(): List<Package> {
        return try {
            val pipDir = File(requireContext().filesDir, "pip-packages").absolutePath
            val py = Python.getInstance()
            val json = py.getModule("pip_runner").callAttr("list_installed", pipDir).toString()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Package(obj.optString("name"), obj.optString("version"))
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private fun runPip(vararg args: String): String {
        return try {
            val py = Python.getInstance()
            val runner = py.getModule("pip_runner")
            val pyArgs = args.map { PyObject.fromJava(it) }.toTypedArray()
            runner.callAttr("run", *pyArgs).toString()
        } catch (e: Throwable) {
            e.message ?: e.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class PackageAdapter(private val items: List<Package>) :
        RecyclerView.Adapter<PackageAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.pkgName)
            val version: TextView = view.findViewById(R.id.pkgVersion)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_package, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.name.text = items[position].name
            holder.version.text = items[position].version
        }

        override fun getItemCount() = items.size
    }
}

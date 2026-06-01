package com.jjewuz.executor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.jjewuz.executor.databinding.FragmentPipBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PipFragment : Fragment() {

    private var _binding: FragmentPipBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPipBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.installBtn.setOnClickListener {
            val packageName = binding.packageInput.text?.toString()?.trim()
            if (!packageName.isNullOrEmpty()) {
                installPackage(packageName)
            }
        }
    }

    private fun installPackage(packageName: String) {
        // writable directory on the real filesystem — pip can install here
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
}

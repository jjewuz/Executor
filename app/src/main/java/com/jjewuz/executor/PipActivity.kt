package com.jjewuz.executor

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.jjewuz.executor.databinding.ActivityPipBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PipActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPipBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.installBtn.setOnClickListener {
            val packageName = binding.packageInput.text?.toString()?.trim()
            if (!packageName.isNullOrEmpty()) {
                installPackage(packageName)
            }
        }
    }

    private fun installPackage(packageName: String) {
        binding.installBtn.isEnabled = false
        binding.outputLabel.visibility = View.VISIBLE
        binding.outputText.visibility = View.VISIBLE
        binding.outputText.text = getString(R.string.pip_installing, packageName)

        lifecycleScope.launch {
            val output = withContext(Dispatchers.IO) {
                runPip("install", packageName)
            }
            binding.outputText.text = output
            binding.installBtn.isEnabled = true
        }
    }

    private fun runPip(vararg args: String): String {
        return try {
            val py = Python.getInstance()
            val runner = py.getModule("pip_runner")
            runner.callAttr("run", args.toList()).toString()
        } catch (e: Throwable) {
            e.message ?: e.toString()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

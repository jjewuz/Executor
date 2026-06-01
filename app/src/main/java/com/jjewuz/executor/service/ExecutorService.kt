package com.jjewuz.executor.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.jjewuz.executor.R
import java.io.File
import java.util.Locale

data class CommandInfo(
    val func: PyObject,
    val desc: String
)

data class CommandModule(
    val name: String,
    val author: String,
    val description: String,
    val commands: Map<String, CommandInfo>
)

class ExecutorService : AccessibilityService() {

    private val py = Python.getInstance()
    private var userModules: MutableList<CommandModule> = mutableListOf()
    private lateinit var builtInModule: CommandModule

    private var isSetting = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                event.source?.let { nodeInfo ->
                    processNode(nodeInfo)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        sendNotification(applicationContext, resources.getString(R.string.serviceStarted), "")
        addPipPackagesToPath()
        val commandsModule = py.getModule("commands")
        val loadedModule = loadModuleFromPyObject(commandsModule, resources.getString(R.string.built_in))
        // merge Kotlin-native commands into the built-in module so they appear in {help}
        val kotlinCommands = mapOf(
            "notify" to CommandInfo(
                func = PyObject.fromJava("__kotlin__"),
                desc = resources.getString(R.string.cmd_notify_desc)
            )
        )
        builtInModule = loadedModule.copy(commands = loadedModule.commands + kotlinCommands)
        loadInternalScripts()
    }

    private fun loadModuleFromPyObject(pyModule: PyObject, fallbackName: String): CommandModule {
        val name = pyModule["NAME"]?.toString() ?: fallbackName
        val author = pyModule["AUTHOR"]?.toString() ?: resources.getString(R.string.not_specified)
        val description = pyModule["DESCRIPTION"]?.toString() ?: resources.getString(R.string.no_description)

        val commandsPy = pyModule["COMMANDS"]
        if (commandsPy == null) {
            Log.e("Executor", "Module '$name' has no COMMANDS dict")
            return CommandModule(name, author, description, emptyMap())
        }
        val commandsMap = mutableMapOf<String, CommandInfo>()

        val commandsDict = try {
            commandsPy.asMap()
        } catch (e: Throwable) {
            Log.e("Executor", "Module '$name': failed to read COMMANDS map: ${e.message}")
            return CommandModule(name, author, description, emptyMap())
        }

        for ((keyPy, valuePy) in commandsDict) {
            val cmdName = keyPy.toString()
            try {
                // use Python's dict.get() — avoids PyObject Java key-equality issues
                val funcObj = valuePy.callAttr("get", "func")
                if (funcObj != null) {
                    val descObj = valuePy.callAttr("get", "desc")
                    val desc = descObj?.toString() ?: resources.getString(R.string.no_description)
                    commandsMap[cmdName] = CommandInfo(funcObj, desc)
                    continue
                }
            } catch (e: Throwable) {
                // value is a direct function reference, not {"func": ..., "desc": ...}
            }
            // fallback: value itself is the callable
            try {
                commandsMap[cmdName] = CommandInfo(valuePy, resources.getString(R.string.no_description))
            } catch (e: Throwable) {
                Log.e("Executor", "Skipping command $cmdName: ${e.message}")
            }
        }

        return CommandModule(name, author, description, commandsMap)
    }



    private fun addPipPackagesToPath() {
        try {
            val pipDir = File(filesDir, "pip-packages")
            if (pipDir.exists()) {
                val sysPath = py.getModule("sys")["path"]!!
                val dirPath = pipDir.absolutePath
                try { sysPath.callAttr("remove", dirPath) } catch (_: Throwable) {}
                sysPath.callAttr("insert", 0, dirPath)
            }
        } catch (e: Throwable) {
            Log.e("Executor", "Failed to add pip-packages to path", e)
        }
    }

    private fun loadInternalScripts() {
        val scriptDir = File(filesDir, "scripts").apply { mkdirs() }
        userModules.clear()

        if (scriptDir.listFiles().isNullOrEmpty()) return

        val py = Python.getInstance()
        val sys = py.getModule("sys")
        val sysPath = sys["path"]!!
        val sysModules = sys["modules"]!!
        val dirPath = scriptDir.absolutePath
        try { sysPath.callAttr("remove", dirPath) } catch (e: Throwable) {}
        sysPath.callAttr("append", dirPath)

        // make the import system aware of new / changed files on disk
        try { py.getModule("importlib").callAttr("invalidate_caches") } catch (e: Throwable) {}

        scriptDir.listFiles()
            ?.filter { it.extension.equals("py", ignoreCase = true) }
            ?.forEach { file ->
                try {
                    val moduleName = file.nameWithoutExtension
                    // drop cached version so edited file content is re-read
                    try { sysModules.callAttr("pop", moduleName, null) } catch (e: Throwable) {}
                    val module = py.getModule(moduleName)
                    val mod = loadModuleFromPyObject(module,
                        moduleName.replace("_", " ")
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
                    if (mod.commands.isNotEmpty()) {
                        userModules.add(mod)
                        Log.d("Executor module", "module ${mod.name} loaded with ${mod.commands.size} commands")
                    } else {
                        Log.w("Executor module", "module ${mod.name} (${file.name}) has no commands — skipped")
                        sendNotification(
                            applicationContext,
                            resources.getString(R.string.module_no_commands_title, file.name),
                            resources.getString(R.string.module_no_commands_desc)
                        )
                    }
                } catch (e: Throwable) {
                    Log.e("Executor", "Error loading module ${file.name}", e)
                    val errorMsg = e.message?.substringAfterLast(": ")?.takeIf { it.isNotBlank() }
                        ?: e.message
                        ?: e.toString()
                    sendNotification(
                        applicationContext,
                        resources.getString(R.string.module_load_error_title, file.name),
                        errorMsg
                    )
                }
            }
    }


    override fun onInterrupt() {
        sendNotification(applicationContext, resources.getString(R.string.serviceStopped), "")
    }

    private fun processNode(root: AccessibilityNodeInfo) {
        if (isSetting) return

        if (root.className == "android.widget.EditText" && root.isEditable) {
            root.text?.toString()?.let { text ->
                processText(text)?.let { newText ->
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    }
                    isSetting = true
                    root.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    handler.postDelayed({ isSetting = false }, 300)
                }
            }
        }

        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { processNode(it) }
        }
    }

    private fun processText(fullText: String): String? {
        val commandPattern = Regex("""\{([^}]+)\}>""")
        val lastMatch = commandPattern.findAll(fullText).lastOrNull() ?: return null

        val commandRange = lastMatch.range
        val textBefore = fullText.substring(0, commandRange.first)
        val commandText = lastMatch.value

        val inside = lastMatch.groupValues[1].trim()
        val parts = inside.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null

        val cmdName = parts[0].lowercase()
        val userArgs = parts.drop(1)

        return when (cmdName) {
            "erase" -> ""
            "help"  -> getHelpText(userArgs)
            "notify" -> {
                val message = if (userArgs.isNotEmpty()) {
                    userArgs.joinToString(" ")
                } else {
                    textBefore.trim().takeIf { it.isNotEmpty() } ?: return null
                }
                sendNotification(applicationContext, message, "")
                textBefore
            }

            else -> {
                val cmdInfo = findCommand(cmdName) ?: run {
                    val aliasValue = lookupAlias(cmdName) ?: return null
                    return textBefore + aliasValue
                }

                var usedLeftText = false  // know if cmd takes left text
                val resultPy: PyObject?

                try {
                    resultPy = if (userArgs.isNotEmpty()) {
                        // with args
                        val args = userArgs.map { PyObject.fromJava(it) }.toTypedArray()
                        cmdInfo.func.call(*args)
                    } else {
                        try {
                            cmdInfo.func.call()
                        } catch (e: Throwable) {
                            val cleanText = textBefore.trimEnd()
                            if (cleanText.isEmpty()) return null
                            usedLeftText = true
                            cmdInfo.func.call(PyObject.fromJava(cleanText))
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("Executor", "Command error: $cmdName", e)
                    val errorMsg = e.message?.substringAfterLast(": ")?.takeIf { it.isNotBlank() }
                        ?: e.message
                        ?: e.toString()
                    sendNotification(
                        applicationContext,
                        resources.getString(R.string.cmd_error_title, cmdName),
                        errorMsg
                    )
                    return textBefore + "Error"
                }

                val result = resultPy?.toString() ?: ""

                if (usedLeftText) {
                    val textToReplace = textBefore
                    textBefore.dropLast(textToReplace.length) + result
                } else {
                    // no left text
                    textBefore + result
                }
            }
        }
    }

    private fun getHelpText(args: List<String> = emptyList()): String {
        val sb = StringBuilder()

        if (args.isEmpty()) {
            sb.append(getString(R.string.help_loaded_modules) + "\n\n")

            val allModules = listOf(builtInModule) + userModules

            for (module in allModules) {
                sb.append("•")
                sb.append(getString(R.string.help_module_title, module.name) + "\n")
                sb.append(getString(R.string.help_module_desc, module.description) + "\n")
                sb.append(getString(R.string.help_module_commands_count, module.commands.size) + "\n\n")

            }

            sb.append(resources.getString(R.string.help_module))
            return sb.toString()
        } else {
            val query = args.joinToString(" ").lowercase()
            val allModules = listOf(builtInModule) + userModules

            val module = allModules.find {
                it.name.lowercase().contains(query) || query in it.name.lowercase()
            }

            if (module == null) {
                return getString(R.string.help_module_not_found, query)
            }

            sb.append(getString(R.string.help_module_title, module.name) + "\n")
            sb.append(getString(R.string.help_module_author, module.author) + "\n")
            sb.append(getString(R.string.help_module_desc, module.description) + "\n")
            sb.append("—".repeat(30) + "\n")

            for ((cmdName, info) in module.commands) {
                sb.append("$cmdName — ${info.desc}\n")
            }

            return sb.toString()
        }
    }

    private fun lookupAlias(name: String): String? {
        return try {
            val aliases = py.getModule("commands")["aliases"] ?: return null
            aliases.callAttr("get", name)?.toString()
        } catch (e: Throwable) {
            null
        }
    }

    private fun findCommand(commandName: String): CommandInfo? {
        builtInModule.commands[commandName]?.let { return it }

        for (module in userModules) {
            module.commands[commandName]?.let { return it }
        }
        return null
    }

    private fun sendNotification(context: Context, title: String, desc: String) {
        val builder = NotificationCompat.Builder(context, "123")
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (desc.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(desc))
        }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    this@ExecutorService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, builder.build())
        }
    }

    companion object {
        private var instance: ExecutorService? = null

        fun getInstance(): ExecutorService? = instance

        fun reloadScripts() {
            instance?.loadInternalScripts()
        }
    }
}
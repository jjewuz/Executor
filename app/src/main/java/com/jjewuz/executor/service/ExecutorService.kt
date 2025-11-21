package com.jjewuz.executor.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
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
        val commandsModule = py.getModule("commands")
        builtInModule = loadModuleFromPyObject(commandsModule, resources.getString(R.string.built_in))
        loadInternalScripts()
    }

    private fun loadModuleFromPyObject(pyModule: PyObject, fallbackName: String): CommandModule {
        val name = pyModule["NAME"]?.toString() ?: fallbackName
        val author = pyModule["AUTHOR"]?.toString() ?: resources.getString(R.string.not_specified)
        val description = pyModule["DESCRIPTION"]?.toString() ?: resources.getString(R.string.no_description)

        val commandsPy = pyModule["COMMANDS"] ?: return CommandModule(name, author, description, emptyMap())
        val commandsMap = mutableMapOf<String, CommandInfo>()

        val commandsDict = commandsPy.asMap()

        for ((keyPy, valuePy) in commandsDict) {
            val cmdName = keyPy.toString()

            if (valuePy is PyObject) {
                try {
                    val innerDict = valuePy.asMap()
                    val funcKey = PyObject.fromJava("func")
                    val descKey = PyObject.fromJava("desc")

                    val funcObj = innerDict[funcKey]
                    val descObj = innerDict[descKey]

                    if (funcObj != null) {
                        val desc = descObj?.toString() ?: resources.getString(R.string.no_description)
                        commandsMap[cmdName] = CommandInfo(funcObj, desc)
                        continue
                    }
                } catch (e: Throwable) {
                    Log.e("Executor", e.toString())
                }
                commandsMap[cmdName] = CommandInfo(valuePy, resources.getString(R.string.no_description))
            }
        }

        return CommandModule(name, author, description, commandsMap)
    }



    private fun loadInternalScripts() {
        val scriptDir = File(filesDir, "scripts").apply { mkdirs() }
        userModules.clear()

        if (scriptDir.listFiles().isNullOrEmpty()) return

        val py = Python.getInstance()
        val sysPath = py.getModule("sys")["path"]!!
        val dirPath = scriptDir.absolutePath
        try { sysPath.callAttr("remove", dirPath) } catch (e: Throwable) {}
        sysPath.callAttr("append", dirPath)

        scriptDir.listFiles()
            ?.filter { it.extension.equals("py", ignoreCase = true) }
            ?.forEach { file ->
                try {
                    val moduleName = file.nameWithoutExtension
                    val module = py.getModule(moduleName)
                    val mod = loadModuleFromPyObject(module,
                        moduleName.replace("_", " ")
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() })
                    if (mod.commands.isNotEmpty()) {
                        userModules.add(mod)
                    }
                    Log.d("Executor module", "module ${mod.name} loaded")
                } catch (e: Throwable) {
                    Log.e("Executor", "Error loading module ${file.name}", e)
                }
            }
    }


    override fun onInterrupt() {
        sendNotification(applicationContext, resources.getString(R.string.serviceStopped), "")
    }

    private fun processNode(root: AccessibilityNodeInfo) {
        if (root.className == "android.widget.EditText" && root.isEditable) {
            root.text?.toString()?.let { text ->
                processText(text)?.let { newText ->
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    }
                    root.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
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

            else -> {
                val cmdInfo = findCommand(cmdName) ?: return null

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
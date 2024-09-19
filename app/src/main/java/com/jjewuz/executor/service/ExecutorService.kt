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

data class CommandModule(val name: String, val author: String, val commands: Map<String, PyObject>)

class ExecutorService : AccessibilityService() {

    private val py = Python.getInstance()
    private val builtInModule = CommandModule(
        "Built-in",
        "jjewuz",
        getCommandsMap(py.getModule("commands"))
    )

    private var userModules: MutableList<CommandModule> = mutableListOf()

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
        sendNotification(applicationContext, resources.getString(R.string.serviceStarted), "")
        loadInternalScripts()
    }

    fun loadInternalScripts() {
        val scriptDir = File(filesDir, "scripts")

        if (scriptDir.exists()) {
            val python = Python.getInstance()
            val pySys = python.getModule("sys")

            // Добавляем новый путь в sys.path
            pySys["path"]?.callAttr("append", scriptDir.path)

            scriptDir.listFiles()?.forEach { file ->
                if (file.extension == "py") {
                    try {
                        val moduleName = file.nameWithoutExtension
                        val module = python.getModule(moduleName)
                        val author = module["AUTHOR"]?.toString() ?: "Unknown"
                        val commands = getCommandsMap(module)

                        val commandModule =
                            CommandModule(name = moduleName, author = author, commands = commands)
                        userModules.add(commandModule)


                        Log.d(
                            "ScriptService",
                            "Loaded module: $moduleName with commands: ${commands.keys}"
                        )
                    } catch (e: Exception) {
                        Log.e("ScriptService", "Failed to load script ${file.name}: ${e.message}")
                    }
                }
            }
        } else {
            Log.e("ScriptService", "No scripts found in internal storage.")
        }
    }

    override fun onInterrupt() {
        sendNotification(applicationContext, resources.getString(R.string.serviceStopped), "")
    }

    private fun processNode(nodeInfo: AccessibilityNodeInfo) {
        if (nodeInfo.className == "android.widget.EditText") {
            nodeInfo.text?.let { text ->
                val processedText = processText(text.toString())
                if (processedText != null) {
                    val arguments = Bundle()
                    arguments.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        processedText
                    )
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                }
            }
        }
    }

    private fun processText(text: String): String? {
        if (text.isBlank()) return null

        val commandPattern = Regex("""\{(.+?)\}>""")
        val matchResult = commandPattern.find(text)

        return if (matchResult != null) {
            val commandString = matchResult.value
            Log.d("MyAccessibilityService", "Found command string: $commandString")
            val parts = commandString.trim().removeSuffix(">").removeSurrounding("{", "}").split(" ")
            val commandName = parts.firstOrNull() ?: return null
            val args = parts.drop(1)

            Log.d("MyAccessibilityService", "Command name: $commandName, Args: $args")
            val result = when (commandName) {
                "help" -> getHelpText()
                "erase" -> {
                    return "" // Очищаем текстовое поле
                }
                else -> {
                    val commandFunc = findCommand(commandName)
                    if (commandFunc != null) {
                        val result = if (args.isEmpty()) {
                            commandFunc.call() // Вызываем без аргументов
                        } else {
                            commandFunc.call(*args.toTypedArray()) // Вызываем с аргументами
                        }
                        Log.d("MyAccessibilityService", "Command result: $result")
                        result?.toString()
                    } else {
                        Log.d("MyAccessibilityService", "Command function not found: $commandName")
                        null
                    }
                }
            }
            if (result != null) {
                return text.replace(commandString, result)
            }
            null
        } else {
            null
        }
    }

    private fun getHelpText(): String {
        val helpText = StringBuilder()

        helpText.append("${resources.getString(R.string.available_commands)}:\n\n")

        helpText.append("${resources.getString(R.string.module)}: ${builtInModule.name}, ${resources.getString(R.string.author)}: ${builtInModule.author}\n")
        builtInModule.commands.keys.forEach { command ->
            helpText.append("$command, ")
        }

        userModules.forEach { module ->
            helpText.append("\n${resources.getString(R.string.module)}: ${module.name}, ${resources.getString(R.string.author)}: ${module.author}\n")
            module.commands.keys.forEach { command ->
                helpText.append("$command, ")
            }
        }

        return helpText.toString()
    }

    private fun findCommand(commandName: String): PyObject? {
        builtInModule.commands[commandName]?.let { return it }

        for (module in userModules) {
            module.commands[commandName]?.let { return it }
        }
        return null
    }

    private fun sendNotification(context: Context, title: String, desc: String) {
        val builder = NotificationCompat.Builder(context, "123")
            .setSmallIcon(R.mipmap.ic_launcher_monochrome) // Замените на ваш иконку
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

    private fun getCommandsMap(module: PyObject): Map<String, PyObject> {
        val commandsPyDict = module["COMMANDS"]?.asMap()
        val commandsMap = mutableMapOf<String, PyObject>()
        if (commandsPyDict != null) {
            for ((key, value) in commandsPyDict) {
                commandsMap[key.toString()] = value
            }
        }
        return commandsMap
    }
}
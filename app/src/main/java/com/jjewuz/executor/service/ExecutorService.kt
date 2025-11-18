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
        instance = this
        sendNotification(applicationContext, resources.getString(R.string.serviceStarted), "")
        loadInternalScripts()
    }

    fun loadInternalScripts() {
        val scriptDir = File(filesDir, "scripts").apply { mkdirs() }
        userModules.clear()

        if (scriptDir.listFiles().isNullOrEmpty()) {
            Log.d("ExecutorService", "No user scripts")
            return
        }

        val py = Python.getInstance()
        val sys = py.getModule("sys")
        val path = sys["path"]!!

        val scriptPath = scriptDir.absolutePath

        try {
            path.callAttr("remove", scriptPath)
        } catch (e: Throwable) {
        }
        path.callAttr("append", scriptPath)

        scriptDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("py", ignoreCase = true) }
            ?.forEach { file ->
                try {
                    val moduleName = file.nameWithoutExtension
                    val module = py.getModule(moduleName)

                    val author = module["AUTHOR"]?.toString() ?: "Unknown"
                    val commands = getCommandsMap(module)

                    userModules.add(CommandModule(moduleName, author, commands))
                    Log.d("ExecutorService", "Loaded $moduleName by $author")
                } catch (e: Throwable) {
                    Log.e("ExecutorService", "Failed ${file.name}: ${e.message}", e)
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
        val textBefore = fullText.substring(0, commandRange.first).trimEnd()

        val inside = lastMatch.groupValues[1].trim()
        val parts = inside.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null

        val cmdName = parts[0].lowercase()
        val userArgs = parts.drop(1)

        Log.d("Executor", "Команда: $cmdName | Аргументы: $userArgs | Текст слева: '$textBefore'")

        return when (cmdName) {
            "erase" -> ""
            "help"  -> getHelpText()

            else -> {
                val func = findCommand(cmdName) ?: return null

                try {
                    val resultPy = if (userArgs.isNotEmpty()) {
                        val args = userArgs.map { PyObject.fromJava(it) }.toTypedArray()
                        func.call(*args)
                    } else {
                        // Пробуем вызвать без аргументов — если упадёт, значит, нужен text
                        try {
                            func.call()  // ← сначала без аргументов (для ip, info, date и т.д.)
                        } catch (e: Throwable) {
                            func.call(PyObject.fromJava(textBefore))  // ← если не вышло — с текстом
                        }
                    }

                    val result = resultPy?.toString() ?: ""

                    Log.d("Executor", "Результат $cmdName: '$result'")
                    result

                } catch (e: Throwable) {
                    Log.e("ExecutorService", "Ошибка команды '$cmdName'", e)
                    "Ошибка"
                }
            }
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
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
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

    companion object {
        private var instance: ExecutorService? = null

        fun getInstance(): ExecutorService? = instance

        fun reloadScripts() {
            instance?.loadInternalScripts()
        }
    }
}
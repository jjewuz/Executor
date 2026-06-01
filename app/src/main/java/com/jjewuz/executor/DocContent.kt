package com.jjewuz.executor

object DocContent {

    val pages: List<DocItem> = listOf(

        DocItem(
            id = "syntax",
            titleRes = R.string.doc_title_syntax,
            sections = listOf(
                DocSection.Text(R.string.doc_syntax_1),
                DocSection.Code("{command_name argument1 argument2}>"),
                DocSection.Text(R.string.doc_syntax_2),
                DocSection.Text(R.string.doc_syntax_3),
                DocSection.Code("Hello {upper}>"),
                DocSection.Text(R.string.doc_syntax_4),
                DocSection.Code("Hello HELLO"),
                DocSection.Text(R.string.doc_syntax_5),
                DocSection.Code("some text {upper}>"),
                DocSection.Text(R.string.doc_syntax_6),
                DocSection.Code("SOME TEXT"),
            )
        ),

        DocItem(
            id = "builtin",
            titleRes = R.string.doc_title_builtin,
            sections = listOf(
                DocSection.Text(R.string.doc_builtin_1),
                DocSection.Code(
                    "uppercase / lowercase  — change text registry\n" +
                    "reverse                — reverse text\n" +
                    "count                  — count characters\n" +
                    "mock                   — random ReGiSTry\n" +
                    "repeat text n          — print text n times\n" +
                    "randomize a b          — random number between a, b\n" +
                    "summarize n1 n2 ...    — numbers sum\n" +
                    "ip                     — return IP\n" +
                    "info                   — app information\n" +
                    "notify text            — send notification with text\n" +
                    "erase                  — clear input field\n" +
                    "help / help module     — help text"
                ),
                DocSection.Text(R.string.doc_builtin_examples_title),
                DocSection.Code(
                    "привет мир {uppercase}>\n" +
                    "→ ПРИВЕТ МИР\n\n" +
                    "{repeat hello 3}>\n" +
                    "→ hello hello hello\n\n" +
                    "{randomize 10 50}>\n" +
                    "→ 36\n\n" +
                    "{summarize 12 1 4 1}>\n" +
                    "→ 18.0\n\n" +
                    "hello world {count}>\n" +
                    "→ 11"
                ),
                DocSection.Text(R.string.doc_builtin_help_title),
                DocSection.Code("{help}>\n{help built-in}>"),
                DocSection.Text(R.string.doc_builtin_help_desc),
            )
        ),

        DocItem(
            id = "aliases",
            titleRes = R.string.doc_title_aliases,
            sections = listOf(
                DocSection.Text(R.string.doc_aliases_1),
                DocSection.Code("{save hi Hello world!}>\n→ Alias 'hi' saved!"),
                DocSection.Text(R.string.doc_aliases_2),
                DocSection.Code("{al hi}>\n→ Hello world!"),
                DocSection.Text(R.string.doc_aliases_3),
                DocSection.Code(
                    "{save sig Happy BDay!}>\n\n" +
                    "{sig}>   →   Happy BDay!"
                ),
                DocSection.Text(R.string.doc_aliases_4),
                DocSection.Code(
                    "{aliases}>          — all aliases list\n" +
                    "{clearalias name}>  — delete alias\n" +
                    "{clearalias}>       — delete all aliases"
                ),
            )
        ),

        DocItem(
            id = "modules",
            titleRes = R.string.doc_title_modules,
            sections = listOf(
                DocSection.Text(R.string.doc_modules_1),
                DocSection.Text(R.string.doc_modules_2),
                DocSection.Code("""import urllib.request
import json

def ip():
    try:
        url = "https://api.myip.com"
        with urllib.request.urlopen(url) as r:
            data = json.loads(r.read().decode())
            return f"IP: {data['ip']}, {data['country']}"
    except Exception as e:
        return str(e)

def lowercase(text):
    return text.lower()

AUTHOR = "yourname"
NAME = "My Module"
DESCRIPTION = "Example module"

COMMANDS = {
    "ip": {
        "func": ip,
        "desc": "Shows your external IP and country."
    },
    "lower": {
        "func": lowercase,
        "desc": "Lowercase. text {lower}>"
    }
}"""),
                DocSection.Text(R.string.doc_modules_3),
                DocSection.Text(R.string.doc_modules_4),
                DocSection.Code("{ip}>\n→ IP: 1.2.3.4, Russia"),
                DocSection.Text(R.string.doc_modules_5),
            )
        ),

        DocItem(
            id = "commands_format",
            titleRes = R.string.doc_title_commands_format,
            sections = listOf(
                DocSection.Text(R.string.doc_fmt_1),
                DocSection.Text(R.string.doc_fmt_full_title),
                DocSection.Code("""COMMANDS = {
    "cmd_name": {
        "func": my_function,
        "desc": "Short description shown in help"
    }
}"""),
                DocSection.Text(R.string.doc_fmt_short_title),
                DocSection.Code("""COMMANDS = {
    "cmd_name": my_function
}"""),
                DocSection.Text(R.string.doc_fmt_2),
                DocSection.Text(R.string.doc_fmt_args_title),
                DocSection.Text(R.string.doc_fmt_3),
                DocSection.Code("{cmd arg1 arg2}>  →  my_function(\"arg1\", \"arg2\")"),
                DocSection.Text(R.string.doc_fmt_4),
                DocSection.Code("some text {cmd}>  →  my_function(\"some text\")"),
            )
        ),

        DocItem(
            id = "packages",
            titleRes = R.string.doc_title_packages,
            sections = listOf(
                DocSection.Text(R.string.doc_pkg_1),
                DocSection.Text(R.string.doc_pkg_2),
                DocSection.Code("requests\nbeautifulsoup4\npydantic\npython-dateutil\npytz"),
                DocSection.Text(R.string.doc_pkg_3),
                DocSection.Code("numpy\npandas\nPillow\nscipy"),
                DocSection.Text(R.string.doc_pkg_4),
            )
        ),

        DocItem(
            id = "tips",
            titleRes = R.string.doc_title_tips,
            sections = listOf(
                DocSection.Text(R.string.doc_tips_chain_title),
                DocSection.Text(R.string.doc_tips_chain),
                DocSection.Text(R.string.doc_tips_naming_title),
                DocSection.Text(R.string.doc_tips_naming),
                DocSection.Code("my_module.py   ✓\nmy-module.py   ✗"),
                DocSection.Text(R.string.doc_tips_debug_title),
                DocSection.Text(R.string.doc_tips_debug),
                DocSection.Text(R.string.doc_tips_service_title),
                DocSection.Text(R.string.doc_tips_service),
            )
        ),
    )

    fun findById(id: String): DocItem? = pages.find { it.id == id }
}

import random
import urllib.request
import json
import os

ALIASES_FILE = os.path.join(os.path.dirname(__file__), "aliases.json") if "__file__" in globals() else "aliases.json"

try:
    with open(ALIASES_FILE, "r", encoding="utf-8") as f:
        aliases = json.load(f)
except:
    aliases = {}

def _save_aliases():
    try:
        with open(ALIASES_FILE, "w", encoding="utf-8") as f:
            json.dump(aliases, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"Ошибка сохранения алиасов: {e}")

def uppercase(text):
    return text.upper()

def lowercase(text):
    return text.lower()

def reverse(text):
    return text[::-1]

def count(text):
    return str(len(text))

def repeat(text, n="2"):
    return text * int(n)

def info():
    return "Executor is real-time text editor, made by jjewuz."

def erase():
    return ""

def randomize(arg1, arg2):
    try:
        num1 = int(arg1)
        num2 = int(arg2)
        return str(random.randint(num1, num2))
    except ValueError:
        return "Invalid arguments. Please provide numbers."


def summarize(*args):
    total = sum(float(arg) for arg in args)
    return total

def mock(text):
    return ''.join(c.upper() if random.randint(0,1) else c.lower() for c in text)

def ip():
    try:
        url = "https://api.myip.com"
        with urllib.request.urlopen(url) as response:
            data = response.read().decode()
            json_data = json.loads(data)
            ip = json_data.get("ip", "not found")
            country = json_data.get("country", "not found")
            return f"IP: {ip}, {country}"
    except Exception as e:
        return str(e)

def save_alias(name: str, *args):
    global aliases
    if not name:
        return "Error: specify name for alias"
    text = ' '.join(args)
    clean_text = (text or "").strip()
    if not clean_text:
        return "Error: no alias text"

    aliases[name.lower()] = clean_text
    _save_aliases()
    return f"Alias '{name}' saved!"

def alias(name: str):
    global aliases
    name = name.lower().strip()
    if name in aliases:
        return aliases[name]
    return f"Alias '{name}' not found"

def aliases_list():
    global aliases
    if not aliases:
        return "No aliases"
    lines = ["Aliases:"]
    for name, text in aliases.items():
        preview = text.replace("\n", "\\n")[:40]
        if len(text) > 40: preview += "..."
        lines.append(f"  {name} → {preview}")
    return "\n".join(lines)

def clear_alias(name: str = None):
    global aliases
    if name is None:
        count = len(aliases)
        aliases.clear()
        _save_aliases()
        return f"Aliased deleted: {count}"

    name = name.lower().strip()
    if name in aliases:
        del aliases[name]
        _save_aliases()
        return f"Alias '{name}' deleted"
    return f"Alias '{name}' not found"



COMMANDS = {

    "uppercase": {"func": uppercase, "desc": "All capital letters, accepts text to the left of the command"},
    "lowercase": {"func": lowercase, "desc": "All lowercase, accepts text to the left of the command"},
    "reverse":   {"func": reverse,   "desc": "Flips the text to the left of the command"},
    "count":     {"func": count,     "desc": "Number of characters to the left of the command"},
    "repeat":    {"func": repeat,    "desc": "Repeats text n times (default 2). Syntax: {repeat text n}"},
    "mock":      {"func": mock,      "desc": "Turns the text to the left of the command into a meme."},
    "erase": {"func": erase, "desc": "Cleans all text field"},

    "randomize": {"func": randomize, "desc": "Random number from a to b. Example: {randomize a b}"},
    "summarize": {"func": summarize, "desc": "Summarize numbers. Example: {summarize n1 n2 n3...}"},

    "al":      {"func": alias,       "desc": "Execute saved alias: {al name}"},
    "save":       {"func": save_alias,  "desc": "Save text as alias. Example: {save name text}"},
    "aliases":    {"func": aliases_list,  "desc": "Show all aliases"},
    "clearalias": {"func": clear_alias,"desc": "Delete one alias or all. {clearalias name} or {clearalias}"},

    "ip":    {"func": ip,    "desc": "Your external IP and country"},
    "info":  {"func": info,  "desc": "Info about app"},
}

NAME = "Base"
AUTHOR = "jjewuz"
DESCRIPTION = "Built-in Executor commands"

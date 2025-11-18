import random
import urllib.request
import json


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
    return "Executor BETA by jjewuz"

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
            ip = json_data.get("ip", "не найден")
            country = json_data.get("country", "не найдена")
            return f"IP: {ip}, {country}"
    except Exception as e:
        return str(e)

COMMANDS = {
    "repeat": repeat,
    "randomize": randomize,
    "summarize": summarize,
    "uppercase": uppercase,
    "reverse": reverse,
    "erase": erase,
    "count": count,
    "mock": mock,
    "ip": ip,
    "info": info,
}

AUTHOR = "jjewuz"

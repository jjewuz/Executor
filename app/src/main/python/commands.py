import random
import urllib.request
import json


def repeat(*args):
    number_of_copies = int(args[0])
    text_to_copy = ' '.join(args[1:])
    return text_to_copy * number_of_copies


def uppercase(*args):
    combined_text = ' '.join(args)
    return combined_text.upper()


def info():
    return "Executor ALPHA v0.0.1 by jjewuz"


def randomize(arg1, arg2):
    try:
        num1 = int(arg1)
        num2 = int(arg2)
        return random.randint(num1, num2)
    except ValueError:
        return "Invalid arguments. Please provide numbers."


def summarize(*args):
    total = sum(float(arg) for arg in args)
    return total


def count(*args):
    combined_text = ' '.join(args)
    return len(combined_text.split())


def erase():
    return ""


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
    "erase": erase,
    "count": count,
    "ip": ip,
    "info": info,
}

AUTHOR = "jjewuz"

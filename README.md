# Executor
Real-time text formatting tool. Use `{command *arguments}>`. Example: `{ip}>` (without args), `{randomize 2 9}>` (2 args). Type `{help}>` to see all commands.

# Built-in commands
`repeat <n> <text>` - repeat text n times
`randomize <num1> <num2>` - random between 2 numbers
`summarize <num1 num2 num3....>` - sum numbers
`uppercase <text>` - make CAPS
`erase` - cleans text field
`count <text>` - count words in text
`info` - information about programm
`ip` - get your ip adress
`help` - liss of all commands include module commands


# Writing custom scripts
Write function what you need. Do not forget about errors handling. Function must always return string.

```python
def ip():
    try:
        url = "https://api.myip.com"
        with urllib.request.urlopen(url) as response:
            data = response.read().decode()
            json_data = json.loads(data)
            ip = json_data.get("ip", "Not found")
            country = json_data.get("country", "Not found")
            return f"IP: {ip}, {country}"
    except Exception as e:
        return str(e)
```

At the end of the file, include the commands in the script and add the author's name (optional):

```python
COMMANDS = {
    "ip": ip,
}

AUTHOR = "name"
```
Your module will be named like file.

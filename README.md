# Executor

### Actual version: 0.4 BETA

#### Developed by jjewuz

Executor is a next-generation Android application that combines technological sophistication and speed. The core mechanic is based on commands executed directly during input. The user doesn't need to open additional menus, press buttons, or switch between functions - just write a command in a special format, and the app will execute it instantly. Works in any text field on Android system.

[Get it on Google Play](https://play.google.com/store/apps/details?id=com.jjewuz.executor)

## Module Library

Official library of additional modules for the application.

[Go to Library](https://executor.jjewuz.com/en/modules.html)

## App Purpose

Executor is designed for those who work with text, data, and quick formatting. It allows you to perform various operations directly while typing - from everyday actions to calculations and retrieving system information.

The app supports loading additional Python modules, making it infinitely expandable. Any developer or advanced user can write their own commands and connect them.

## Who will find it useful?

- Those who work extensively with text
- Programmers and technical specialists
- Copywriters and authors
- Automation enthusiasts

## App Uniqueness

- Command execution directly during input
- Simple syntax like `{command arguments}>`
- Support for Python modules to extend functionality
- Flexibility and scalability

# List of Built-in Basic Commands

### repeat

Repeats text n-times. If no repetition count is specified, defaults to 2.

**Syntax:**

```text
{repeat <text> <count>}>
```

**Example:**

```text
{repeat hello 3}>
```

**Result:** hello hello hello

### randomize

Outputs a random number between values.

**Syntax:**

```text
{randomize <number1> <number2>}>
```

**Example:**

```text
{randomize 10 50}>
```

**Result:** 36

### summarize

Sums up the provided numbers.

**Syntax:**

```text
{summarize <number1> <number2> ...}>
```

**Example:**

```text
{summarize 12 1 4 1}>
```

**Result:** 18.0

### uppercase

Converts text to uppercase.

**Syntax:**

```text
<text> {uppercase}>
```

**Example:**

```text
hello world {uppercase}>
```

**Result:** HELLO WORLD

### lowercase

Converts text to lowercase.

**Syntax:**

```text
<text> {lowercase}>
```

**Example:**

```text
HELLO WORLD {lowercase}>
```

**Result:** hello world

### count

Counts the number of characters in text.

**Syntax:**

```text
<text> {count}>
```

**Example:**

```text
hello world this is test {count}>
```

**Result:** 24

### erase

**Syntax:**

```text
{erase}>
```

**Result:** clears the input field.

### mock

Returns text with random case.

**Syntax:**

```text
<text> {mock}>
```

**Example:**

```text
hello world this is test {mock}>
```

**Result:** HeLlO WoRlD tHIs Is TeST

### reverse

Returns text in reverse order.

**Syntax:**

```text
<text> {reverse}>
```

**Example:**

```text
hello world this is test {reverse}>
```

**Result:** tset si siht dlrow olleh

### info

**Syntax:**

```text
{info}>
```

**Result:** displays program information.

### ip

**Syntax:**

```text
{ip}>
```

**Result:** shows the device's current IP address.

### help

**Syntax:**

```text
{help}>
```

**Result:** outputs a list of all modules, their descriptions, and command count.

```text
{help <module_name>}>
```

**Result:** outputs a list of all module commands and their descriptions.

Executor monitors input in real-time. When a user enters text in this format, the app identifies the command, processes it, and replaces it with the result. Regular text can be written simultaneously - the app works like a "smart notepad".

## Writing Custom Modules

Write a function in Python language. Don't forget about possible errors. The function must always return string type:

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
If your team assumes the use of the text on the left outside the command, specify `text` in the arguments. Example:

```python
def lowercase(text):
    return text.lower()
```

Using many args:

```python
def summarize(*args):
    total = sum(float(arg) for arg in args)
    return total
```

## Module and commands registration

To make commands available, you need to register them. Commands must have descriptions.

```python
COMMANDS = {
    "ip": {
        "func": ip,
        "desc": "Shows your current IP address."
    }
}
```


Specify the necessary module data for the application. Module name is better to be short.

```text
AUTHOR = "jjewuz"
NAME = "IPCheck"
DESCRIPTION = "This module has 1 command to show your IP."
```

Old modules are compatible with the current version, but it's better to adapt them to the new version.

## Other

[Privacy Policy](https://executor.jjewuz.com/en/privacy-policy.html)

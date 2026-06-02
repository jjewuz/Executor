# Executor

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

## Features

The app is organized into tabs accessible from the bottom navigation bar:

### 🏠 Home

Set up permissions, select your scripts folder, and reload modules on the fly —
no need to toggle the accessibility service off and on anymore.

### 📦 Package Manager

Install pure-Python packages from PyPI directly inside the app to use them in your
modules (e.g. `requests`, `beautifulsoup4`, `python-dateutil`). The screen also
shows a live list of all currently installed packages, including the ones bundled
with the app (`requests`, `numpy`, `Pillow`, `regex`, `lxml`).

> Packages with native C extensions can't be installed at runtime — the most useful
> ones are bundled with the app out of the box.

### ✏️ Code Editor

A built-in code editor with **Python syntax highlighting** (powered by
[sora-editor](https://github.com/Rosemoe/sora-editor)). Browse your scripts folder,
open and edit existing modules, or create new ones — all without leaving the app.
Includes a symbol toolbar with tab and common Python characters for comfortable
typing on mobile, and saves changes straight back to your folder with an automatic
module reload.

### 🧩 Module Library

Browse the official [module library](https://executor.jjewuz.com/en/modules.html)
right inside the app. Each module shows its description, author, compatible versions
and update date. Tap **Download** to install a module straight into your scripts
folder — already-installed modules show an **Update** button instead.

### 📖 Documentation

Built-in, fully localized documentation covering command syntax, built-in commands,
aliases, writing modules, the `COMMANDS` format, installing packages and handy tips.
Each topic opens in a dedicated screen with formatted text and copyable code examples.

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

### notify

Sends an Android notification with the given text (or the text to the left of the
command if no argument is provided).

**Syntax:**

```text
{notify <text>}>
```

**Example:**

```text
{notify Don't forget the meeting}>
```

**Result:** a notification with the text is shown; the command is removed from the field.

### Aliases

Save any text under a short name and insert it later with a single command — handy
for signatures, templates and frequently typed phrases.

**Save an alias:**

```text
{save <name> <text>}>
```

**Insert it** (both forms work):

```text
{al <name>}>
{<name>}>
```

**Manage aliases:**

```text
{aliases}>            # list all saved aliases
{clearalias <name>}>  # delete one alias
{clearalias}>         # delete all aliases
```

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

## Building from Source

### Requirements

- **Android Studio** (latest stable) or the Android SDK + Gradle
- **JDK 17**
- **Python 3.13** installed locally — required by [Chaquopy](https://chaquo.com/chaquopy/) to bundle Python packages at build time (`requests`, `numpy`, `Pillow`, `regex`, `lxml`)

### Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/jjewuz/Executor.git
   ```

2. Make sure **Python 3.13** is available. The build looks for it on your `PATH`
   (via the `py` launcher on Windows, or `python3.13`).

   If your interpreter is in a non-standard location, point Chaquopy to it by
   adding this line to `local.properties` (this file is **not** committed to git):

   ```properties
   chaquopy.buildPython=C:/Users/<you>/AppData/Local/Programs/Python/Python313/python.exe
   ```

   On macOS / Linux:

   ```properties
   chaquopy.buildPython=/usr/bin/python3.13
   ```

3. Build a debug APK:

   ```bash
   ./gradlew assembleDebug
   ```

   Or install directly to a connected device / emulator:

   ```bash
   ./gradlew installDebug
   ```

The output APK is located at `app/build/outputs/apk/debug/app-debug.apk`.

> **Note:** The first build downloads and bundles native Python packages, so it
> may take a few minutes. Subsequent builds are cached and much faster.

## Writing Custom Modules

Write a function in Python language. Don't forget about possible errors. The function must always return string type:

```python
import urllib.request
import json

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

> You can use any pure-Python package from PyPI — install it from the **Package
> Manager** tab and `import` it in your module. Several popular packages
> (`requests`, `numpy`, `Pillow`, `regex`, `lxml`) are bundled and ready to use.
If your command assumes the use of the text on the left outside the command, specify `text` in the arguments. Example:

```python
def lowercase(text):
    return text.lower()
```

Using many args:

```python
def summarize(*args):
    total = sum(float(arg) for arg in args)
    return str(total)
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

The code and program are licensed under the Apache 2.0 License.

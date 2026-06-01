import io
import json
import os
import re
import sys
import urllib.request
import urllib.parse
import zipfile

PYPI_JSON = "https://pypi.org/pypi/{}/json"


def run(*args):
    args_list = list(args)

    if not args_list or args_list[0] != "install":
        return "Only 'install' command is supported"

    package_name = None
    target_dir = None
    i = 1
    while i < len(args_list):
        if args_list[i] == "--target" and i + 1 < len(args_list):
            target_dir = args_list[i + 1]
            i += 2
        else:
            package_name = args_list[i]
            i += 1

    if not package_name:
        return "No package name specified"
    if not target_dir:
        return "No --target specified"

    os.makedirs(target_dir, exist_ok=True)

    installed = set()
    results = []
    _install_with_deps(package_name, target_dir, installed, results)

    if target_dir not in sys.path:
        sys.path.insert(0, target_dir)

    return "\n".join(results)


def _install_with_deps(package_name, target_dir, installed, results):
    key = re.split(r'[>=<!;\s\[]', package_name)[0].strip().lower().replace("-", "_")
    if key in installed:
        return
    installed.add(key)

    result, deps = _install(package_name, target_dir)
    results.append(result)

    for dep in deps:
        _install_with_deps(dep, target_dir, installed, results)


def _install(package_name, target_dir):
    """Returns (result_str, list_of_dependency_names)."""
    try:
        url = PYPI_JSON.format(urllib.parse.quote(package_name))
        with urllib.request.urlopen(url, timeout=15) as r:
            data = json.loads(r.read().decode())

        version = data["info"]["version"]
        name = data["info"]["name"]
        files = data["urls"]

        # check if already extracted
        norm = name.lower().replace("-", "_")
        try:
            for item in os.listdir(target_dir):
                if item.lower().replace("-", "_").startswith(norm):
                    return f"Already installed: {name}=={version}", _parse_deps(data)
        except Exception:
            pass

        # find pure-Python wheel (platform == any)
        wheel = None
        for f in files:
            fn = f["filename"]
            if fn.endswith(".whl"):
                parts = fn[:-4].split("-")
                if len(parts) == 5 and parts[4] == "any":
                    wheel = f
                    break

        if not wheel:
            return (
                f"⚠ No pure-Python wheel for {name} {version} — skipped",
                _parse_deps(data)
            )

        with urllib.request.urlopen(wheel["url"], timeout=30) as r:
            data_bytes = r.read()

        with zipfile.ZipFile(io.BytesIO(data_bytes)) as z:
            z.extractall(target_dir)

        return f"✓ Installed {name}=={version}", _parse_deps(data)

    except urllib.error.HTTPError as e:
        msg = f"✗ '{package_name}' not found on PyPI" if e.code == 404 else f"✗ HTTP {e.code} for '{package_name}'"
        return msg, []
    except Exception as e:
        return f"✗ Error installing '{package_name}': {e}", []


def _parse_deps(pypi_data):
    """Extract required (non-optional, non-platform-specific) dependencies."""
    requires = pypi_data["info"].get("requires_dist") or []
    deps = []
    for req in requires:
        # skip optional extras
        if "extra ==" in req:
            continue
        # skip Windows / macOS / CPython-only deps
        if any(s in req for s in ('sys_platform == "win32"', 'sys_platform == "darwin"', 'platform_system == "Windows"')):
            continue
        # extract bare package name
        dep_name = re.split(r'[>=<!;\s\[\(]', req)[0].strip()
        if dep_name:
            deps.append(dep_name)
    return deps

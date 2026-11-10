"""Verify the packaged bridge against a new fictional vault, then let smoke mode exit."""
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import tempfile
import time
import urllib.request


def main():
    executable = str(Path(sys.argv[1]).resolve())
    with tempfile.TemporaryDirectory(prefix="billvault-import-smoke-") as directory:
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]
        token = secrets.token_hex(32)
        env = {**os.environ, "BILLVAULT_MCP_TOKEN": token}
        options = {}
        if os.name == "nt":
            startup = subprocess.STARTUPINFO()
            startup.dwFlags |= subprocess.STARTF_USESHOWWINDOW
            startup.wShowWindow = subprocess.SW_HIDE
            options["startupinfo"] = startup
        process = subprocess.Popen([executable, "--data-dir=" + directory,
            "--mcp-port=" + str(port), "--smoke-test=true"], env=env,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, **options)
        def request(path, body=None):
            headers = {"Authorization": "Bearer " + token}
            data = None
            if body is not None:
                headers["Content-Type"] = "application/json"
                data = json.dumps(body).encode()
            req = urllib.request.Request("http://127.0.0.1:" + str(port) + path,
                data=data, headers=headers)
            with urllib.request.urlopen(req, timeout=5) as response:
                return json.load(response)
        try:
            deadline = time.monotonic() + 75
            while True:
                if process.poll() is not None:
                    raise RuntimeError("Packaged application exited before its bridge became ready")
                try:
                    categories = request("/api/categories")
                    break
                except (OSError, ValueError):
                    if time.monotonic() >= deadline:
                        raise RuntimeError("Packaged bridge startup timed out") from None
                    time.sleep(0.1)
            expense = {"name": "Packaged fictional stream", "kind": "SUBSCRIPTION",
                "categoryId": categories[0]["id"], "amount": "14.99", "currency": "USD",
                "frequency": "MONTHLY", "startDate": "2026-11-01",
                "nextDueDate": "2026-12-01", "status": "ACTIVE", "variableAmount": False}
            preview = request("/api/imports/preview", {"source": "Fictional package smoke",
                "expenses": [expense, expense]})
            assert [row["action"] for row in preview["rows"]] == ["CREATE", "SKIP"]
            path = "/api/imports/" + preview["previewId"] + "/commit"
            result = request(path, {"confirmed": True})
            assert len(result["createdIds"]) == 1 and result["skipped"] == 1
            assert request(path, {"confirmed": True}) == result
            assert request("/api/expenses")["totalElements"] == 1
            assert process.wait(timeout=30) == 0
            assert Path(directory, "billvault.mv.db").is_file()
            print("Packaged bridge import, duplicate skip, retry, and window smoke passed.")
        finally:
            if process.poll() is None:
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()


if __name__ == "__main__":
    main()

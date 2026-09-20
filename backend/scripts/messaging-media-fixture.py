"""Local acceptance-only replacement for Worker/R2, using the backend's real authorization.

Reads the isolated shared storage volume. Never used by production Compose.
"""
import json
import mimetypes
import os
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlencode, urlsplit
from urllib.request import Request, urlopen
from urllib.error import HTTPError


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # Private URLs contain short access tokens.

    def do_GET(self):
        parsed = urlsplit(self.path)
        mode, separator, key = parsed.path.lstrip("/").partition("/")
        path = Path("/data/images", key).resolve()
        if mode not in ("public", "private") or not key.startswith("images/") or not path.is_relative_to("/data/images/images"):
            self.send_error(404)
            return
        query = parse_qs(parsed.query)
        headers = {"X-Internal-Token": os.environ["BACKEND_INTERNAL_TOKEN"]}
        try:
            if mode == "public":
                request = Request("http://backend:8088/internal/media/meta?" + urlencode({"storageKey": key}), headers=headers)
                with urlopen(request, timeout=10) as response:
                    meta = json.load(response)
                if meta["visibility"] != "public" or str(meta["version"]) != query.get("v", [""])[0]:
                    self.send_error(403)
                    return
            else:
                request = Request("http://backend:8088/internal/media/authorize?" + urlencode({
                    "storageKey": key, "token": query.get("auth", [""])[0]}), headers=headers)
                with urlopen(request, timeout=10):
                    pass
            data = path.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", mimetypes.guess_type(path)[0] or "application/octet-stream")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(data)
        except HTTPError as error:
            self.send_error(error.code)
        except (OSError, KeyError, ValueError):
            self.send_error(404)


ThreadingHTTPServer(("0.0.0.0", 8080), Handler).serve_forever()

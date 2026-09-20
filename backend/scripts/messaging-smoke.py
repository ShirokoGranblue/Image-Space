"""Isolated Compose acceptance. Never targets the normal Compose project or production.

Run from the repository root after starting docker-compose.messaging-test.yaml.
Credentials/session artifacts are generated under ignored frontend/output/messaging only.
"""
import json
import os
from pathlib import Path
import random
import socket
import statistics
import struct
import subprocess
import time
import urllib.request
import uuid
import zlib

ROOT = Path(__file__).resolve().parents[2]
ARTIFACTS = ROOT / "frontend/output/messaging"
COMPOSE = ["docker", "compose", "--env-file", str(ARTIFACTS / "messaging-test.env"),
           "-f", str(ROOT / "docker-compose.messaging-test.yaml")]
BASE = "http://127.0.0.1:18088"


def compose(*args, enabled=None):
    env = os.environ.copy()
    if enabled is not None:
        env["MQ_TEST_ENABLED"] = str(enabled).lower()
    result = subprocess.run(COMPOSE + list(args), env=env, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError("Isolated Compose operation failed: " + " ".join(args[:2]))
    return result.stdout


def sql(statement):
    result = subprocess.run(COMPOSE + ["exec", "-T", "mysql", "sh", "-c",
                            'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -N -B picture_management'],
                            input=statement, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError("Test SQL operation failed")
    return result.stdout.strip()


def redis(*args):
    with socket.create_connection(("127.0.0.1", 16379), timeout=5) as sock:
        parts = [str(arg).encode() for arg in args]
        sock.sendall(b"*" + str(len(parts)).encode() + b"\r\n" + b"".join(
            b"$" + str(len(p)).encode() + b"\r\n" + p + b"\r\n" for p in parts))
        stream = sock.makefile("rb")
        line = stream.readline()
        if line.startswith(b"$"):
            size = int(line[1:])
            return None if size < 0 else stream.read(size).decode()
        return line[1:].strip().decode()


def api(path, data=None, token=None, body=None, content_type=None, method=None):
    headers = {}
    if token:
        headers["satoken"] = token
    if data is not None:
        body = json.dumps(data).encode()
        content_type = "application/json"
    if content_type:
        headers["Content-Type"] = content_type
    request = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=40) as response:
        result = json.load(response)
    if result.get("code") != 200:
        raise RuntimeError("Test API failed: " + path + " code=" + str(result.get("code")))
    return result.get("data")


def wait_for(test, timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            value = test()
            if value:
                return value
        except (OSError, RuntimeError):
            pass
        time.sleep(0.5)
    raise AssertionError("Acceptance condition timed out")


def ready():
    return wait_for(lambda: api("/image/square"))


def captcha():
    result = api("/user/captcha")
    raw = json.loads(redis("GET", "captcha:" + result["captchaId"]))
    return result["captchaId"], raw["data"]


def login(account):
    captcha_id, code = captcha()
    return api("/user/login", {"username": account["username"], "password": account["password"],
                              "captchaId": captcha_id, "captchaCode": code})


def create_account():
    name = "mq_" + uuid.uuid4().hex[:12]
    password = uuid.uuid4().hex
    email = name + "@example.invalid"
    code = str(random.SystemRandom().randint(100000, 999999))
    redis("SET", "code:register:" + email, json.dumps(code), "EX", 120)
    captcha_id, captcha_code = captcha()
    account = {"username": name, "password": password}
    user = api("/user/register", {**account, "confirmPassword": password, "email": email,
               "code": code, "captchaId": captcha_id, "captchaCode": captcha_code})
    sql(f"INSERT IGNORE INTO user_roles(user_id,role_id) SELECT id,1 FROM users WHERE username='{name}';")
    sql(f"UPDATE users SET role='admin' WHERE username='{name}';")
    account["user"] = user
    return account


def sample_png():
    width, height = 2400, 1600
    rng = random.Random(42)
    scanlines = b"".join(b"\0" + rng.randbytes(width * 3) for _ in range(height))
    def chunk(kind, content):
        return struct.pack(">I", len(content)) + kind + content + struct.pack(">I", zlib.crc32(kind + content))
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(scanlines)) + chunk(b"IEND", b"")
    (ARTIFACTS / "messaging-sample.png").write_bytes(data)
    return data


def upload(token, sample):
    boundary = "mq" + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="visibility"\r\n\r\nPUBLIC\r\n'
            f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="mq-sample.png"\r\n'
            'Content-Type: image/png\r\n\r\n').encode() + sample + f"\r\n--{boundary}--\r\n".encode()
    start = time.monotonic()
    result = api("/image/upload", token=token, body=body, content_type="multipart/form-data; boundary=" + boundary)
    elapsed = time.monotonic() - start
    return result, round(elapsed, 3), start


def image_done(image_id):
    return sql(f"SELECT COUNT(*) FROM images WHERE uuid='{image_id}' AND thumb_key IS NOT NULL AND medium_key IS NOT NULL;") == "1"


def save_session(account, token):
    (ARTIFACTS / "messaging-session.json").write_text(json.dumps({**account, "token": token}), encoding="utf-8")


def verify_replay():
    event_id = str(uuid.uuid4())
    payload = json.dumps({"eventId": event_id, "version": 1, "type": "AUDIT", "audit": {
        "module": "TEST", "result": "FAIL", "status": "FAILED", "riskLevel": "HIGH",
        "createTime": "2026-09-19T00:00:00", "costTime": 0}})
    # Missing required action deliberately forces bounded retries in the isolated database.
    sql(f"INSERT INTO message_outbox(event_id,event_type,payload) VALUES ('{event_id}','AUDIT','{payload}');")
    wait_for(lambda: sql(f"SELECT state FROM message_outbox WHERE event_id='{event_id}';") == "DEAD", 120)
    sql(f"UPDATE message_outbox SET payload=JSON_SET(payload,'$.audit.action','MQ_REPLAY_ACCEPTANCE') WHERE event_id='{event_id}';")
    replay = subprocess.run(["pwsh", "-NoProfile", "-File", str(ROOT / "backend/scripts/replay-message.ps1"),
                             "-EventId", event_id, "-ComposeFile", str(ROOT / "docker-compose.messaging-test.yaml"),
                             "-EnvFile", str(ARTIFACTS / "messaging-test.env")], capture_output=True, text=True)
    assert replay.returncode == 0 and replay.stdout.strip() == "1", "Replay operator script failed"
    wait_for(lambda: sql(f"SELECT state FROM message_outbox WHERE event_id='{event_id}';") == "DONE")
    assert sql(f"SELECT COUNT(*) FROM audit_log WHERE event_id='{event_id}';") == "1"
    return {"event_id": event_id, "dead_letter_replayed": True, "audit_rows": 1}


def main():
    ready()
    account = create_account()
    sample = sample_png()
    measurements = {"sample_bytes": len(sample), "dimensions": "2400x1600", "sync_seconds": [],
                    "async_response_seconds": [], "async_complete_seconds": []}
    # Warm each mode once, then measure three identical inputs.
    for enabled in [False, True]:
        compose("up", "-d", "--no-deps", "backend", enabled=enabled)
        ready()
        token = login(account)
        for index in range(4):
            image, elapsed, started = upload(token, sample)
            if enabled:
                assert image.get("thumbUrl") is None and image.get("mediumUrl") is None
                wait_for(lambda: image_done(image["uuid"]))
                if index:
                    measurements["async_response_seconds"].append(elapsed)
                    measurements["async_complete_seconds"].append(round(time.monotonic() - started, 3))
            else:
                assert image.get("thumbUrl") and image.get("mediumUrl")
                if index:
                    measurements["sync_seconds"].append(elapsed)
    # Broker outage: successful original + durable audit/task; deletion and visibility changes before recovery.
    compose("stop", "rabbitmq")
    try:
        original, outage_seconds, _ = upload(token, sample)
        deleted, _, _ = upload(token, sample)
        restricted, _, _ = upload(token, sample)
        api("/image/" + deleted["uuid"], token=token, method="DELETE")
        api("/image/" + restricted["uuid"], token=token, data={"visibility": "PRIVATE"}, method="PUT")
        assert not image_done(original["uuid"])
        assert int(sql("SELECT COUNT(*) FROM message_outbox WHERE state='PENDING';")) >= 6
        # Original download remains usable while RabbitMQ is down.
        with urllib.request.urlopen(BASE + "/image/download/" + original["uuid"], timeout=20) as response:
            assert response.read() == sample
        measurements["outage_upload_seconds"] = outage_seconds
    finally:
        compose("start", "rabbitmq")
    wait_for(lambda: image_done(original["uuid"]), 180)
    wait_for(lambda: image_done(restricted["uuid"]), 180)
    assert sql(f"SELECT COUNT(*) FROM images WHERE uuid='{deleted['uuid']}';") == "0"
    assert sql(f"SELECT visibility FROM images WHERE uuid='{restricted['uuid']}';") == "PRIVATE"
    wait_for(lambda: sql("SELECT COUNT(*) FROM message_outbox WHERE state<>'DONE';") == "0", 180)
    # Restart both services, retaining volumes; the same Flyway migration must not run twice.
    compose("restart", "rabbitmq", "backend")
    ready()
    token = login(account)
    assert image_done(original["uuid"])
    wait_for(lambda: sql("SELECT COUNT(*) FROM message_outbox WHERE state<>'DONE';") == "0", 180)
    measurements["checks"] = ["original readable during outage", "durable catch-up", "deleted task skipped",
                               "visibility retained", "broker/backend restart", "audit catch-up"]
    measurements["replay"] = verify_replay()
    save_session(account, token)
    (ARTIFACTS / "messaging-acceptance.json").write_text(json.dumps(measurements, indent=2), encoding="utf-8")
    print(json.dumps(measurements, indent=2))


if __name__ == "__main__":
    main()

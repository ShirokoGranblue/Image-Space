---
name: credentials-in-env-vars
description: Sensitive credentials moved from application.yml to environment variables
metadata: 
  node_type: memory
  type: project
  originSessionId: 162a19e9-bc83-4fe9-9df7-b8a3b1820b93
---

All sensitive credentials (database passwords, OAuth client secrets, SMS keys) were moved from `application.yml` to environment variables. The file is now in `.gitignore`. An `application.example.yml` template with placeholder values is provided as reference.

**Why:** Prevent accidental credential leakage through version control.
**How to apply:** Never hardcode credentials in config files. Use env vars with defaults, and reference `application.example.yml` for required keys.

---
name: domain-https-migration
description: "Migrated from IP 4.230.10.11 to domain image-space.app with Let's Encrypt HTTPS"
metadata: 
  node_type: memory
  type: project
  originSessionId: 4c7f230e-5d9e-4453-ac75-d087daafd71c
---

Migrated all configs from `http://4.230.10.11` to `https://image-space.app`. Domain registered at name.com, DNS resolves to 4.230.10.11.

**SSL:** Let's Encrypt via certbot standalone mode on Ubuntu 24.04. Cert covers `image-space.app` + `www.image-space.app`, expires 2026-08-24, auto-renews. Cert path: `/etc/letsencrypt/live/image-space.app/`. Docker mounts `/etc/letsencrypt:/etc/letsencrypt:ro` into nginx.

**Nginx:** `deploy/nginx/default.conf` has 4 server blocks: www→bare (80+443), HTTP→HTTPS (80), HTTPS main (443). If HTTPS breaks, check this file wasn't overwritten back to HTTP-only version — happened once already.

**Credentials:** Google OAuth updated to new client ID `...-ah505c173...` and secret. `.env` on server has real values. `application.yml` and `docker-compose.yml` are in .gitignore (contain real secrets for local dev).

**Why:** Domain was newly registered, took ~48h but actually propagated in minutes. Modern browsers auto-upgrade HTTP→HTTPS, so SSL was mandatory for OAuth redirects to work.
**How to apply:** All default values in config files now use `https://image-space.app`. Server `.env` overrides with real values. When touching OAuth, always use HTTPS protocol.

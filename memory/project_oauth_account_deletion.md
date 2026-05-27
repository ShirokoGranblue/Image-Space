---
name: oauth-and-account-deletion
description: OAuth login (GitHub/Google) and soft account deletion features added
metadata: 
  node_type: memory
  type: project
  originSessionId: 162a19e9-bc83-4fe9-9df7-b8a3b1820b93
---

Added JustAuth-based GitHub and Google OAuth login with email-first binding strategy. Added soft account deletion via DELETE /user/account with confirmation. SMS login (SmsService, endpoints, UI tab) was removed in the same cycle.

**Why:** Expand login options beyond traditional methods and comply with account data regulations.
**How to apply:** When touching auth or user account code, OAuth flow uses email-first matching; account deletion is soft-delete, not hard-delete.

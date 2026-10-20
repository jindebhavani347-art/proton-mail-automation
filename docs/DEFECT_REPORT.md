# Defect Report

> Only add genuine findings from your own exploratory testing. Do not invent bugs. Keep **defects** (behaviour that
> contradicts documentation or reasonable expectation) separate from **assumptions/limitations** (things you could
> not verify or that are by design). Copy the template below for each finding.

## Findings summary
| ID | Title | Type | Severity | Status |
|----|-------|------|----------|--------|
| AUTO-RISK-01 | Saved/re-loaded browser session is rejected with `reason=session-expired` | Automation risk (not a product defect) | Medium for automation, none for users | Mitigated: one logged-in tab per account per run |
| NOTE-01 | New accounts show 2 unread welcome mails, welcome dialogs and inbox category tabs | Observation / assumption | Low | Handled in test design |

No product defects have been confirmed yet. The two items above are limitations of automating a live service, not
bugs: Proton's session handling is a security design, and the welcome content is expected for new accounts.

## AUTO-RISK-01 - Saved browser session rejected (session-expired)
**Type:** Automation risk / limitation (not claimed as a defect)
**Environment:** Playwright Chromium (Java 1.49), Windows 11 host, viewport 1440x900, mail.proton.me / account.proton.me, 2026-10-20, free accounts
**Severity / priority + reasoning:** Medium for the test suite (it blocked every test until redesigned); no user impact, because a real
user who signs in normally stays signed in. Not a Proton bug: part of the session key is bound to the original tab, so a
copied cookie/storage state is correctly refused.
**Preconditions:** a valid account that logged in through the UI; its cookies/local storage saved (Playwright storageState) and loaded into a new browser context.
**Steps to reproduce:**
1. Log in through the UI in context A and save `storageState`.
2. Create context B from that state and open `https://mail.proton.me/u/0/inbox`.
3. Observe the redirect.
   **Expected:** mailbox opens with the saved session.
   **Actual:** redirected to `account.proton.me/authorize?app=proton-mail&...&reason=session-expired`; compose button never appears.
   **Evidence:** Playwright call logs ("navigated to ... reason=session-expired") from repeated runs; screenshots/diagnostics in `reports/`.
   **Reproducibility:** Always with the cached-state design (observed in every run before the redesign).
   **Mitigation:** log in once per account through the UI (ticking "Keep me signed in") and reuse that same tab for the whole run;
   use explicit extra logins only where an independent session is the thing under test. To be re-checked after the change:
   a full page reload of the second account's tab also redirected to session-expired in one earlier run, so in-app
   navigation is used instead of full reloads except in the tests that deliberately test refresh behaviour.

## NOTE-01 - New-account state affects test design
Observed on the two free test accounts: 2 unread Proton welcome mails, first-login dialogs, a desktop-notification banner,
and inbox category tabs (Primary / Social / Promotions / Newsletters). Assumption: mail from the other test account lands in
Primary. Consequence: tests use unique subjects and delta-based counters, and welcome dialogs are dismissed automatically.

## Template
**ID / Title:**
**Type:** Defect | Usability issue | Automation risk | Assumption/limitation
**Environment:** Browser + version, OS, viewport, date/build (e.g. Chrome 1xx, Windows 11, 1440x900, 2026-10-xx)
**Severity / priority + reasoning:** (impact on users, workaround available?, frequency)
**Preconditions:** account type, data, state
**Steps to reproduce:** 1. ... 2. ... 3. ...
**Expected:**
**Actual:**
**Evidence:** screenshot / video / trace path / console or network log
**Reproducibility:** Always | Often (x/y runs) | Intermittent (observations)

## Automation-relevant risks (already known, document what you actually observe)
- Login may present CAPTCHA/human verification, which blocks headless runs.
- Search results depend on background indexing, so freshly delivered mail may not be searchable immediately.
- Free-plan limits (custom filters, folders, labels) affect repeatable setup.
- The inbox category tabs (Primary/Social/Promotions/Newsletters) can hide a delivered message from the list that
  the test is looking at.
- Brand-new accounts show welcome dialogs and 2 unread welcome mails, so absolute counts are unreliable.
- Auto-save "Saved" indicators can be stale after a second edit, so draft edits are verified by reopening the saved draft.
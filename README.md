# Proton Mail Web - Automation QA Assessment

Java 17 + Playwright + JUnit 5 + Maven. 21 automated scenarios (one is parameterized) across all required categories,
two-account end-to-end flows (Sender -> Receiver), failure evidence (screenshots + traces), delivery-latency metrics
and an HTML report. All test cases (62+) with priority, automation class and rationale: `docs/TEST_CASES.csv`.

## Current status / known issues (read first)
Honest snapshot at submission time (2026-10-20):
- **Done:** strategy, 65 test cases with priority/automation class/rationale (`docs/TEST_CASES.csv`), architecture note,
  framework (config, page objects, single locator file, failure screenshots/traces/diagnostics, metrics, CI workflow),
  21 automated scenarios written, README quality review below.
- **Verified working:** the project compiles; the UI login succeeds for the test accounts (the mailbox loads and the
  compose button is found and clicked); a failure produces a screenshot and a diagnostics file in `reports/`.
- **Not yet green:** the last recorded run (`ComposeTests`, 8 executions incl. 5 parameterized) finished with
  **0 passed / 8 failed**. Failures are in the selector/synchronisation layer against the live Proton UI
  (e.g. composer locator, session handling), not in test design. The remaining classes have not been run to completion.
- **Next step (if more time):** run `ComposeTests#sendMailReachesReceiver` alone, open
  `reports/diagnostics/*.txt` (lists the real `data-testid` values and button labels on the failing page), correct the
  matching line in `Locators.java`, then widen to the other classes.
- **Key finding:** Proton rejects re-loaded saved browser sessions (`reason=session-expired`), so the suite keeps one
  logged-in tab per account instead of caching cookies. See `docs/DEFECT_REPORT.md` (AUTO-RISK-01) and `docs/TEST_REPORT.md`.

## 1. Prerequisites
- JDK 17+, Maven 3.9+
- Two **dedicated, throw-away** Proton accounts (free plan is fine): a *sender* and a *receiver*.
  Do not use a personal mailbox: tests send, archive, trash and delete mail.
- Make sure 2FA is **off** on both test accounts (tests log in with email + password).

## 2. Setup
```bash
git clone <repo> && cd proton-mail-automation
cp .env.example .env                    # fill SENDER_* and RECEIVER_* (never commit .env)
mvn -q test-compile
mvn exec:java -e -Dexec.classpathScope=test \
    -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## 2b. Delivery time and metrics
COMP-01 records send-to-arrival latency in `reports/metrics.csv` (also printed as `[metric] delivery_latency_ms=...`).
Use it in the test report to state typical delivery time.

## 3. First run (important)
Proton may show a CAPTCHA / human verification on a new login. Do the first login **headed** so you can solve it;
each account is logged in once and that same browser tab is reused by every test (Proton rejects re-loaded saved sessions with `reason=session-expired`, so cookies/storage are not cached).
```bash
mvn test -Dtest=AuthenticationTests#validLoginOpensMailbox -DHEADLESS=false
mvn test -DHEADLESS=false -Dtest=ComposeTests#sendMailReachesReceiver   # creates sender + receiver sessions
```

## 4. Commands
| Goal | Command |
|---|---|
| Whole suite | `mvn test` |
| PR smoke set (5 tests) | `mvn test -Dgroups=smoke` |
| One class / test | `mvn test -Dtest=InboxActionTests` / `-Dtest=ComposeTests#invalidRecipientBlocksSend` |
| Headed / slow debug | `mvn test -DHEADLESS=false` |
| Other browser | `mvn test -DBROWSER=firefox` (chromium, firefox, webkit) |
| HTML report | `mvn surefire-report:report-only` -> `target/site/surefire-report.html` |

Raw XML results: `target/surefire-reports/`. Failure evidence: `reports/screenshots/*.png`, `reports/traces/*.zip`.
Open a trace: `mvn exec:java -e -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="show-trace reports/traces/<file>.zip"`.

## 5. Project structure
```
src/test/java/com/qa/proton/
  config/Config.java        env / .env / -D resolution, no hard-coded secrets
  core/BaseTest.java        browser lifecycle, per-account contexts, tracing, failure evidence
  core/SessionManager.java  login once via UI, cache storage state in ./auth
  pages/                    Locators (all selectors), LoginPage, MailboxPage, ComposerPage, SearchPanel, FiltersPage
  utils/TestData.java       unique subjects/bodies, attachment fixtures
  pages/FoldersPage.java    custom-folder helper (idempotent fixture folder "QA-Folder")
  utils/Metrics.java        writes reports/metrics.csv (delivery latency)
  tests/                    9 classes, 21 tests (COMP-07 runs 5 inputs)
docs/                       TEST_STRATEGY.md, TEST_CASES.csv, DEFECT_REPORT.md, ARCHITECTURE.md
.github/workflows/ci.yml    CI (secrets from repo settings)
```

## 6. Assumptions and test data
- Every test creates uniquely named data (`QA-<tag>-<uuid>`), so tests are independent and re-runnable.
- Inbox/search/filter tests act on the **receiver** mailbox; mail is seeded from the sender through the real UI
  (no private APIs are used). Proton has no public mail API for setup, so UI seeding is the safe option.
- Free plans limit custom filters; the filter test always deletes its filter in `finally`.
- Selectors are centralised in `Locators.java`. Verify once with codegen if any differ in your build:
  `mvn exec:java -e -Dexec.classpathScope=test -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="codegen https://mail.proton.me"`.

## 6b. Observed UI facts that shaped the design (new free accounts)
- New accounts already contain 2 unread Proton welcome mails -> tests never assume an empty inbox or an absolute
  unread count; every assertion targets a unique subject or a delta.
- The inbox has category tabs (Primary / Social / Promotions / Newsletters). Mail from the other test account is
  expected in Primary; the list helper only sees the open tab (documented automation risk).
- Search is an always-visible bar ("Search messages") that opens an advanced panel; the compose button reads "New message".
- A "desktop notifications" banner and first-login welcome dialogs appear; `MailboxPage.dismissOverlays()` closes
  dialogs that have a clearly dismissive button. Notification permission itself is manual-only (INBOX-08).
- Custom folders/labels and filters are limited on free plans; the folder test reuses one fixture folder and the
  filter test always deletes its filter.

## 7. Troubleshooting
| Symptom | Cause / fix |
|---|---|
| Login times out headless | CAPTCHA. Run with `-DHEADLESS=false` and solve it in the opened window (you have 2 minutes) |
| `reason=session-expired` | Do not reuse saved browser state; the suite keeps one logged-in tab per account for the run |
| "did not arrive within ... ms" | Slow delivery. Raise `MAIL_DELIVERY_TIMEOUT_MS` |
| Locator not found | UI changed. Fix the one line in `Locators.java` |
| Search returns nothing for new mail | Proton indexes in the background; rerun, check the search assertion timeout |

## 8. Automation Quality Review

**1. Deliberately NOT automated**
- CAPTCHA / human-verification and brute-force lockout: bot detection is designed to block automation; manual only.
- Scheduled send waiting for the real time: slow and time-dependent. Automate only creating and cancelling a scheduled
  message; verify delivery manually or with a clock-controlled/mock approach.
- Password-protected and encrypted-to-outside email, and pixel/layout/visual checks: need a human eye or an external
  recipient; high maintenance for low regression value.
- Rich-text formatting, drag-and-drop labels, mobile viewport: exploratory first, automate later if they stabilise.

**2. PR vs nightly**
- Every PR (`-Dgroups=smoke`): AUTH-01, COMP-01, COMP-02, DRAFT-01, INBOX-03. They guard login, send/receive,
  validation, persistence and message state, the P0/P1 journeys that block users.
- Nightly: the full 21, plus Firefox/WebKit. Slower (undo-send, attachment download, filters, search indexing),
  mail-delivery dependent, lower per-change risk.
- Trade-off: fast feedback on PRs vs completeness; nightly catches what PRs skip without blocking developers.

**3. Passes locally, flaky in CI**
1. Download the trace + screenshot artifacts of the failing run and find the first failing action.
2. Compare environment: headless vs headed, viewport, timezone/locale, CPU speed, network and mail-delivery latency.
3. Look for state leakage: shared mailbox data, stale cached session, parallel runs on one account.
4. Reproduce with `--repeat`/loop and CPU throttling; fix the cause (missing wait on a state, not an element) and only
   then consider a single, logged retry.

**4. Reducing flakiness when the UI changes often**
- Role/test-id/text locators, never positional CSS or XPath; all selectors in one file.
- Wait on state (toast gone, saved indicator, row present), never `sleep`.
- Unique data per test; no inter-test dependencies; cached login.
- Retries only for known infrastructure noise, reported separately so they never hide real defects.
- A scheduled smoke run catches selector breaks within a day, and a single-file fix repairs them.

**5. Scaling from 20 to 200+ tests**
- Keep the layering (tests -> page/component objects -> locators); add feature-area packages and shared fixtures.
- One account pair per parallel worker (account pool) so tests can run concurrently; shard across CI jobs.
- Move setup/cleanup to faster paths (cached sessions, bulk-delete helpers) and keep UI only for what is under test.
- Tag by risk (smoke / regression / nightly) and review flaky-test reports weekly; quarantine, fix, or delete.
- Contract: every new test needs a priority, an owner and a reason it must be automated.
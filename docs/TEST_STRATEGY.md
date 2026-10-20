# Test Strategy - Proton Mail Web

## Scope
In: authentication/session, compose and send, drafts, inbox actions, folders and labels, search, filters, attachments,
asynchronous flows (undo send, scheduled send). Web app only (Chrome primary; Firefox/WebKit via config).
Out: mobile apps, Bridge/IMAP, billing, Proton Calendar/Drive/VPN, load/performance, real encryption internals.

## Assumptions
- Two dedicated free test accounts, 2FA off. Live application is the system under test (no sandbox exists).
- Mail delivery between Proton accounts is near-instant but not guaranteed, so tests wait on state, never fixed time.
- UI is the only safe interface for data setup (no public mail API), so helpers drive the UI.

## Observations from the live app (free accounts)
New accounts hold 2 unread Proton welcome mails; the inbox is split into category tabs; search is a persistent bar with
an advanced panel; folders, labels and filters are plan-limited. These facts drive: unique-subject assertions,
delta-based unread checks, a single reusable fixture folder, and clean-up of filters.

## Risks and how they shape the suite
| Risk | Impact | Mitigation |
|---|---|---|
| Bot detection / CAPTCHA on login | Blocks every test | Login once headed, cache session; keep login tests few |
| Shared mailboxes, parallel runs | Data collisions | Unique data per test; sequential by default; account pool to scale |
| Async delivery and background search indexing | Flaky timing | State-based polling with refresh, bounded timeouts |
| Frequent UI changes | Maintenance cost | Central `Locators.java`, role/test-id selectors |
| Mail is hard to delete (free plan limits) | Mailbox noise | Namespaced subjects `QA-<tag>-<id>`, clean-up in `finally` |
| Live production service | Unsafe actions | No destructive account actions, no bulk traffic, low volume |

## Test levels and approach
- UI end-to-end only (that is what is in scope); critical flows validated across two accounts (sender -> receiver).
- Assertions check state (row present/absent in a folder, unread class, field values, downloaded content), not
  just visibility.
- Exploratory sessions drive defect discovery and feed new automated cases.

## Priorities
P0 login, send/receive, validation. P1 drafts, inbox actions, attachments, undo send. P2 search, filters, labels.
P3 cosmetic and edge formatting.

## Automation approach
Page/component objects over a single locator file, JUnit 5 tags (`smoke`), trace-on-failure, HTML report, GitHub Actions
workflow with secrets, environment-based config.

## Key exclusions
CAPTCHA, brute-force lockout, real scheduled-time waits, password-protected mail to external recipients, visual
layout, mobile, accessibility audits (exploratory/manual).

## Coverage rationale (why these 21)
Each required area meets its minimum (auth 3, compose 4, drafts 5, inbox 3, search 2, filters 1, async 1, attachments 2,
plus folders 1). Every test either protects a P0/P1 user journey or exercises a distinct state transition
(draft -> sent, inbox -> trash -> inbox, send -> undo -> send) or a corner case worth guarding (BCC privacy,
malformed recipients, no duplicate drafts, server-side persistence of drafts and read state). Redundant UI paths
(CC copy, star, empty search) were dropped in favour of fewer, deeper tests with strong state assertions.
Every case in `TEST_CASES.csv` carries a written rationale for automating or not automating it.

## Non-automated (manual / exploratory)
See `TEST_CASES.csv` rows marked "Manual / Exploratory": scheduled send timing, password-protected mail, rich-text,
label drag-and-drop, CAPTCHA/lockout, keyboard shortcuts, responsive layout.

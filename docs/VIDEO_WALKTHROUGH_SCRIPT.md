# Video Walkthrough Script (4-5 minutes)

| Time | Show | Say |
|---|---|---|
| 0:00-0:30 | README + repo tree | Goal, stack (Java, Playwright, JUnit), two test accounts, what is covered |
| 0:30-1:15 | docs/TEST_STRATEGY.md + TEST_CASES.csv | Risk-based priorities (P0-P3); every case has an automate/manual rationale; 21 automated of 65 cases |
| 1:15-2:00 | Project structure | tests -> pages -> Locators (one file for selectors); BaseTest; SessionManager (login once, reuse) |
| 2:00-3:00 | Run `mvn test -Dgroups=smoke`, then show one E2E test (COMP-01) | Two accounts, unique data, state-based waits, delivery latency metric |
| 3:00-3:40 | Async test (ASYNC-01 undo send) | Wait on the Undo toast instead of sleeps; verify nothing sent, then sent once |
| 3:40-4:15 | A failure: screenshot + trace in reports/ | How a failure is diagnosed; evidence only on failure |
| 4:15-4:50 | README section 8 + DEFECT_REPORT | Answers to the 5 quality questions; defects vs assumptions; scaling plan (account pool, sharding) |

Tips: record at 1080p, keep the terminal font large, and state one trade-off out loud (sequential runs because of shared mailboxes).

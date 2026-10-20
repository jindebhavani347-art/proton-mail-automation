# Architecture Note

```
tests/  ->  pages/ (LoginPage, MailboxPage, ComposerPage, SearchPanel, FiltersPage)  ->  Locators
   |                                                                                   (all selectors)
   +-> core/BaseTest  -> core/SessionManager (login once, cached storage state)
   +-> config/Config  (env / .env / -D), utils/TestData (unique data)
```

Key decisions
1. **Single locator file.** UI changes break one file, not dozens of tests.
2. **Page/component objects own behaviour and assertions on state**; tests read like business scenarios.
3. **Session reuse.** Each account logs in once through the UI; storage state is cached in `./auth`. Only the
   authentication tests use fresh contexts, which also avoids invalidating the shared session on logout.
4. **Two contexts per test** (sender + receiver) give real end-to-end verification without shared mutable state.
5. **State-based waiting.** Web-first assertions plus bounded polling with refresh for mail delivery; no `sleep`.
6. **Evidence on failure only.** Tracing runs always; the trace zip and full-page screenshot are saved only when a test
   fails, keeping reports small and useful.
7. **Unique data per test** (`QA-<tag>-<uuid>`) so tests are independent, re-runnable and easy to find in the mailbox.
8. **Sequential by default** because two shared mailboxes are the bottleneck; scaling path = account pool per worker.
9. **No blanket retries.** A retry would hide genuine defects; add only for justified infrastructure noise, reported separately.

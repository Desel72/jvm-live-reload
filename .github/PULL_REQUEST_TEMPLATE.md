<!--
  Thanks for opening a PR! A few quick notes:

  - If this PR is still a work in progress, please open it as a draft.
  - See CONTRIBUTING.md for the full guidelines.
  - Keep the PR scoped to one change - no drive-by refactors or formatting
    passes over untouched files.
-->

## Summary

<!-- What does this PR do, in 1-3 sentences? Focus on the "why". -->

Fixes #

## How was it tested?

<!--
  Pick whichever applies and add details. Exceptions must have a good reason.

  - New test - link or path
  - Covered by existing tests (refactoring only)
  - Manual test against a real build - describe the setup
  - Non-code change, no tests needed
-->

## Checklist

- [ ] I ran the relevant test recipe locally (`just test-sbt` / `just test-gradle` / `just test-mill`)
- [ ] I ran `just code-format-check-all` and it passes
- [ ] I updated `README.md` if this changes a config key, hook, hook bundle or supported framework
- [ ] I updated the [tested frameworks table](../README.md#list-of-tested-frameworks) if this adds or upgrades framework support
- [ ] No unrelated files were reformatted or refactored

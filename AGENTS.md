# AGENTS.md

This file gives coding agents (Claude Code, Codex, Cursor, ...) the context they
need to work in this repository productively. Humans contributing to the project
should read [CONTRIBUTING.md](CONTRIBUTING.md) instead - this file overlaps with
it but is tuned for agents.

## Project overview

`jvm-live-reload` is a framework-agnostic live-reload tool for JVM web and GRPC
applications. It works by putting a reverse proxy in front of the application;
on each request the proxy detects source changes, drops the application's
`ClassLoader`, and restarts the application inside the same JVM process. The
proxy speaks plain HTTP or HTTP/2 + GRPC framing depending on configuration.

The repo ships build-tool plugins for **sbt**, **Gradle** and **mill**, all
backed by the same Java core. The plugins themselves are thin - most of the
interesting code lives under `core/`.

Read [README.md](README.md) before touching anything non-trivial. It documents
the user-facing API, configuration keys, hooks and the list of tested
frameworks - all of which constrain what changes are acceptable.

## Setup commands

Minimum JDK is **17**. The repo standardizes on `just` as the entry point for
every common task:

```sh
# Tests (each is independent and can be run separately)
just test-sbt
just test-gradle
just test-mill
just test-all              # everything, slow

# Local publishing - required before sbt/mill tests pick up core changes
just publish-local-sbt
just publish-local-mill
just clear-local-repos     # wipes published artifacts; run after editing core/

# Formatting
just code-format-check-all    # CI uses this
just code-format-apply-all    # rewrites in place
```

If you hit `InaccessibleObjectException` while running anything locally, export:

```sh
export JDK_JAVA_OPTIONS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED"
```

CI sets the same flag - see `.github/workflows/ci.yml`.

## Repository layout

| Path                   | What it contains                                                       |
| ---------------------- | ---------------------------------------------------------------------- |
| `core/build-link/`     | Shared Java API consumed by every plugin (hooks, classloader plumbing) |
| `core/runner/`         | Reverse proxy that fronts the user's application                       |
| `core/webserver/`      | HTTP proxy implementation                                              |
| `core/webserver-grpc/` | GRPC proxy and GRPC-specific hooks                                     |
| `core/hook-scala/`     | Scala-only hooks for `zio`, `cats-effect`                              |
| `sbt/`                 | sbt plugin; scripted tests under `sbt/src/test/resources/`             |
| `gradle/`              | Gradle plugin; functional tests in Kotlin                              |
| `mill/`                | mill plugin; integration tests under `mill/integration/`               |

The `core/*` modules are published as plain JVM artifacts. Plugins depend on
them via the local Ivy/Maven repository, so any change under `core/` must be
republished locally before the plugin tests pick it up.

## Code style

- **Scala** code is formatted by `scalafmt` (config in `.scalafmt.conf`, invoked
  via `scala-cli fmt`). Style is mostly the defaults.
- **Kotlin** and **Java** code under `gradle/` are formatted by Spotless via
  `./gradlew spotlessApply`.
- Match the surrounding style. The codebase mixes Scala 2.13, Scala 3, Java and
  Kotlin - don't port a file from one to another without a reason.
- Public Java APIs in `core/build-link/` are the contract used by every plugin
  and by user-supplied hooks. Treat them as stable - additions are fine, renames
  and signature changes are not.
- Configuration keys (`live.reload.*`) are also user-facing. If you add one,
  document it in `README.md` and follow the same naming pattern.

## Testing instructions

Each build tool has its own test suite:

- `sbt` uses tests under `sbt/src/test/resources/`. Each folder is a
  self-contained build with a `test` script that drives sbt and asserts on
  output.
- `gradle` uses tests in Kotlin under
  `gradle/plugin/plugin/src/functionalTest/kotlin/...`. Each test spins up a
  real Gradle build via the TestKit.
- `mill` uses tests in `mill/integration/`, driven by `IntegrationTests.scala`.

<!-- prettier-ignore-start -->
> [!WARNING]
> The mill integration helpers `runUntil` and `runGrpcUntil` in
> `mill/integration/src/IntegrationTests.scala` recurse indefinitely on
> failure - they have no max-retry cap. If the application under test fails
> to compile or start, the test will keep logging `Got exception: ...`
> forever rather than failing.
>
> When running `just test-mill` or any `mill *integration*` command from an
> agent, use a short timeout (3-5 minutes is plenty for local runs) and kill
> the run as soon as you see the same exception repeating in the logs -
> don't keep extending the wait.
<!-- prettier-ignore-end -->

When adding a test:

- Reproduce the bug or exercise the new feature in the smallest existing setup
  that fits. `http4s-*` and `zio-*` are the lightest sbt scripted fixtures; the
  Kotlin functional tests cover Ktor, http4k, Javalin and grpc-java.
- New framework support requires a new fixture plus a row in the
  [tested frameworks table](README.md#list-of-tested-frameworks).
- Anything touching `core/` should ideally be exercised by at least one plugin
  test so the cross-module wiring is verified.

## Pull request instructions

- Keep PRs scoped to one change. No drive-by refactors or formatting passes over
  untouched files.
- Run the relevant `just test-*` recipe locally before requesting review.
- Run `just code-format-check-all` - CI fails on style violations.
- Update `README.md` whenever you add or change a config key, a hook, a hook
  bundle or a supported framework.
- If you used an AI assistant, disclose it in the PR description and confirm you
  actually ran the tests. See the
  [AI-assisted contributions](CONTRIBUTING.md#ai-assisted-contributions)
  section.

## Things to avoid

- Don't add features beyond what an issue or task asks for. The README is the
  source of truth for the public surface - any new public knob is a
  documentation change too.
- Don't change defaults of existing `live.reload.*` keys without a strong
  reason; they're already in user builds.
- Don't introduce a new dependency on a third-party library in `core/*` without
  checking that it doesn't conflict with versions users may bring on their
  classpath - `core/` runs inside the user's JVM.
- Don't add commits, push branches or open PRs from an agent run unless the
  human operator explicitly asked for it. Read-only investigation and local
  edits are fine; remote side effects are not.

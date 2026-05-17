# Contributing to jvm-live-reload

First of all, thanks for taking the time to look into this. This project is in
an alpha-quality stage, so any contribution - a bug report, a framework setup
that worked (or didn't), a fix, a new feature - is genuinely appreciated.

- [Reporting issues](#reporting-issues)
- [Asking questions and sharing setups](#asking-questions-and-sharing-setups)
- [Development setup](#development-setup)
  - [Prerequisites](#prerequisites)
  - [Building and testing](#building-and-testing)
  - [Formatting](#formatting)
- [Repository layout](#repository-layout)
- [Pull request guidelines](#pull-request-guidelines)
- [Adding a framework to the tested list](#adding-a-framework-to-the-tested-list)
- [AI-assisted contributions](#ai-assisted-contributions)

## Reporting issues

If something doesn't work for your setup, please file an issue using one of the
templates:

- [Not working setup](https://github.com/seroperson/jvm-live-reload/issues/new?template=1-not_working_setup.yml) -
  your application doesn't reload, hangs, crashes on reload, and so on
- [Bug report](https://github.com/seroperson/jvm-live-reload/issues/new?template=2-bug_report.yml) -
  something behaves incorrectly but isn't directly tied to a framework
- [Feature request](https://github.com/seroperson/jvm-live-reload/issues/new?template=3-feature_request.yml) -
  missing build tool, missing hook, anything you'd like to see

The more details you can share - framework, JDK version, build tool, a minimal
reproduction - the faster the issue can be triaged. Enabling
`live.reload.debug=true` and including the stacktrace usually helps a lot.

## Asking questions and sharing setups

For general questions, or to share a setup that worked for you (even if it's
already covered in the tested list), please use
[Discussions](https://github.com/seroperson/jvm-live-reload/discussions).
Posting your setup there directly helps other users figure out whether their
stack is likely to work.

## Development setup

### Prerequisites

Minimum required JDK is **17**, same as for end users. You'll also need build
tools for whichever module you want to touch:

| Tool                                           | Used for                                       |
| ---------------------------------------------- | ---------------------------------------------- |
| [`just`](https://github.com/casey/just)        | Entry point for every common task              |
| [`sbt`](https://www.scala-sbt.org)             | `core/*`, `sbt/` plugin and its scripted tests |
| [`gradle`](https://gradle.org)                 | `gradle/` plugin and its functional tests      |
| [`mill`](https://mill-build.org)               | `mill/` plugin and its integration tests       |
| [`scala-cli`](https://scala-cli.virtuslab.org) | Scala formatting check via `scala-cli fmt`     |

If you only intend to touch one build tool, you can skip installing the others -
the corresponding `just` recipes will just not be usable.

<!-- prettier-ignore-start -->
> [!NOTE]
> You may run into the `InaccessibleObjectException` when running tests
> locally. The fix is the same as for end users - export
> `JDK_JAVA_OPTIONS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED"`.
> The CI workflow does the same thing.
<!-- prettier-ignore-end -->

### Building and testing

All common tasks are exposed through the `justfile`. The most useful ones:

```sh
# Run tests for a specific build tool
just test-sbt
just test-gradle
just test-mill

# Run everything (slow)
just test-all

# Publish the core artifacts locally so the plugins can pick them up
just publish-local-sbt
just publish-local-mill
```

The `sbt` and `mill` test recipes publish the `core/*` artifacts to your local
repository first (`$HOME/.ivy2/local/me.seroperson/` and
`$HOME/.m2/repository/me/seroperson/`), because the plugins consume them as
regular dependencies. If you change anything under `core/` and want the plugin
tests to pick it up, run `just clear-local-repos` and re-run the test recipe.

### Formatting

Scala code is formatted with `scalafmt` (driven by `scala-cli fmt`), Kotlin and
Java code under `gradle/` are formatted with Spotless:

```sh
just code-format-apply-all    # rewrites files in place
just code-format-check-all    # CI uses this one
```

CI fails if formatting doesn't match, so it's worth wiring this into a
pre-commit hook (the repo already has a minimal one - see `.git/hooks/`).

## Repository layout

The repo is a single monorepo containing several build systems that share the
same `core/` runtime:

| Path                   | What lives there                                                       |
| ---------------------- | ---------------------------------------------------------------------- |
| `core/build-link/`     | The shared Java API used by every plugin (hooks, classloader plumbing) |
| `core/runner/`         | The reverse proxy that fronts your application                         |
| `core/webserver/`      | HTTP proxy implementation                                              |
| `core/webserver-grpc/` | GRPC proxy implementation and GRPC-specific hooks                      |
| `core/hook-scala/`     | Scala-only hooks (`zio`, `cats-effect`, ...)                           |
| `sbt/`                 | The sbt plugin and its scripted tests under `sbt/src/test/resources/`  |
| `gradle/`              | The Gradle plugin and its functional tests in Kotlin                   |
| `mill/`                | The mill plugin and its integration tests                              |

When in doubt about how a piece behaves end-to-end, the scripted/functional/
integration tests are the best reference - they exercise real builds with real
frameworks.

## Pull request guidelines

A few things that help your PR get merged faster:

- Keep PRs focused. One change per PR, no drive-by refactors.
- If you fix a bug, add a test that reproduces it. The existing scripted/
  functional/integration suites are the right place to put it.
- Run the relevant `just test-*` recipe locally before pushing. CI runs all
  three, but local feedback is faster.
- Don't reformat untouched files. `just code-format-apply-all` should not
  produce changes outside your edits.
- Update the [list of tested frameworks](README.md#list-of-tested-frameworks) if
  your change adds or upgrades support.
- The default branch is `main`. There are no long-lived release branches yet.

Commit messages don't follow a strict convention, but please keep the subject
line short and descriptive, and explain the "why" in the body if it isn't
obvious from the diff.

## Adding a framework to the tested list

If you'd like to officially add a framework to the
[list of tested frameworks](README.md#list-of-tested-frameworks):

1. Pick the closest existing test as a template:
   - For `sbt`, add a folder under `sbt/src/test/resources/<framework>/` and a
     `test` script - look at `http4s-*` or `zio-*` for reference.
   - For `gradle`, add a `LiveReload<Framework>Test.kt` under
     `gradle/plugin/plugin/src/functionalTest/kotlin/...`.
   - For `mill`, add resources under `mill/integration/resources/` and wire them
     into `IntegrationTests.scala`.
2. Make sure the framework respects the
   [Changes to the application code](README.md#changes-to-the-application-code)
   contract - `/health` endpoint, `InterruptedException` handling, graceful
   shutdown.
3. Add a row to the table in `README.md` with the framework name, version, a
   link to the test and the changes required in the application code.

## AI-assisted contributions

It's fine to use AI tools (Copilot, Claude, Codex, Cursor, ...) to help with a
PR, but be sure to always verify its' output. Run the relevant `just test-*`
recipe locally and confirm that the change actually does what it claims.

See also [AGENTS.md](AGENTS.md) for instructions aimed directly at coding agents
working in this repo.

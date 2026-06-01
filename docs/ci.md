# GitHub CI

The repository runs four GitHub Actions workflows. Together they cover
pull-request validation, license-header enforcement, snapshot publication
from the `snapshot` branch, and release publication from the `main` branch.

All workflow definitions live in [`.github/workflows`](../.github/workflows).

## Branch model

`snapshot` is the active development line — all PRs target it, and every push
publishes a `-SNAPSHOT` artifact. `main` always holds the latest released
version, which is available on
[Maven Central](https://repo1.maven.org/maven2/org/eclipse/fennec/emf/) under
`org.eclipse.fennec.emf:*`.

| Branch     | Purpose                                            | Publishes to                                              |
|------------|----------------------------------------------------|-----------------------------------------------------------|
| `snapshot` | Active development. PRs target this branch.        | Sonatype Central — `-SNAPSHOT` versions                   |
| `main`     | Latest release — code here matches what is on Maven Central. | Sonatype Central → Maven Central — final versions, signed with project GPG key |

## Workflow overview

```
┌─────────────────────────┐
│   PR / feature branch   │
└────────────┬────────────┘
             │  push / pull_request
             ▼
    ┌─────────────────┐    ┌──────────────────┐
    │   build.yml     │    │   license.yml    │
    │   (CI Build)    │    │ (License header) │
    └─────────────────┘    └──────────────────┘
             │
             │  merge into snapshot
             ▼
    ┌─────────────────┐
    │  snapshot.yml   │  →  publishes SNAPSHOT artifacts
    └─────────────────┘
             │
             │  merge into main
             ▼
    ┌─────────────────┐
    │   release.yml   │  →  publishes signed release artifacts
    └─────────────────┘
```

## `build.yml` — CI Build

* **File:** [`.github/workflows/build.yml`](../.github/workflows/build.yml)
* **Triggers:**
  * `push` on any branch **except** `main` and `snapshot`
  * `pull_request` on any branch
* **Purpose:** Validate that the source tree compiles, all tests pass, and
  the performance test suite still runs across the supported Java versions.
* **Matrix:** Java 21 and Java 25 (Temurin distribution) — `fail-fast: false`
  so a failure on one JDK does not cancel the other.
* **Runner:** `ubuntu-latest`.
* **Steps:** checkout → Gradle wrapper validation → set up JDK with Gradle
  cache → `./gradlew build --info` → `./gradlew perfTest --info`.
* **Secrets used:** none — this workflow does not publish anything.

This is the workflow PR authors care about: a green run on both JDKs is the
gating signal for review.

## `license.yml` — License header check

* **File:** [`.github/workflows/license.yml`](../.github/workflows/license.yml)
* **Triggers:** `push`, `pull_request`, and manual `workflow_dispatch`.
* **Purpose:** Verify every source file carries the Eclipse Public License
  2.0 header. Uses [apache/skywalking-eyes](https://github.com/apache/skywalking-eyes)
  driven by [`.licenserc.yaml`](../.licenserc.yaml).
* **What it checks:** the SPDX header pattern declared in `.licenserc.yaml`,
  applied to every file *not* listed under `paths-ignore`. New file types
  that should not require a header must be added to `paths-ignore`.
* **Failure mode:** on a PR the action comments on the offending lines via
  `GITHUB_TOKEN`. The fix is to add the standard header (template in
  [`CONTRIBUTING.md`](../CONTRIBUTING.md#license-headers)) and push again.
* **Secrets used:** `GITHUB_TOKEN` (auto-provided) — only for PR comments.

## `snapshot.yml` — Snapshot Build

* **File:** [`.github/workflows/snapshot.yml`](../.github/workflows/snapshot.yml)
* **Triggers:** `push` to the `snapshot` branch only. Pull requests are
  explicitly excluded so untrusted code cannot reach the publishing step.
* **Purpose:** Build, test, and publish `-SNAPSHOT` artifacts whenever the
  `snapshot` branch advances.
* **Matrix:** Java 21 and Java 25.
* **What it publishes:** only the **Java 21** job runs the publishing step
  (`./gradlew release --stacktrace --scan --info` with `DO_RELEASE=false`).
  Java 25 runs build + perfTest only as a compatibility canary.
* **Test artifacts:** JUnit XML reports are uploaded under
  `test-results-java-${java-version}` for every run.
* **Secrets used:**
  * `CENTRAL_SONATYPE_TOKEN_USERNAME`, `CENTRAL_SONATYPE_TOKEN_PASSWORD` — Sonatype Central credentials
  * `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `GPG_KEY_ID` — signing key (imported into the runner's keyring, deleted at the end of the job)

## `release.yml` — Release Build

* **File:** [`.github/workflows/release.yml`](../.github/workflows/release.yml)
* **Triggers:** `push` to the `main` branch only. PRs are explicitly excluded.
* **Purpose:** Cut a signed release to Sonatype Central whenever `main` advances.
* **Matrix:** Java 21 and Java 25. As with `snapshot.yml`, only Java 21
  executes the release step (`./gradlew release --info` with
  `DO_RELEASE=true`). Java 25 acts as the compatibility check.
* **Secrets used:** same set as `snapshot.yml`.
* **Result:** signed artifacts pushed to Sonatype Central and (after the
  Central sync) to Maven Central.

## Published artifacts

Releases and snapshots are published to **Sonatype Central**, from which
releases sync to Maven Central. The group id is `org.eclipse.fennec.emf`.

| Channel    | Repository URL                                                                                     | Pushed by                |
|------------|----------------------------------------------------------------------------------------------------|--------------------------|
| Release    | [Maven Central](https://repo1.maven.org/maven2/org/eclipse/fennec/emf/) — `org.eclipse.fennec.emf:*` | `release.yml` on `main`  |
| Snapshot   | [Sonatype Central snapshots](https://central.sonatype.com/repository/maven-snapshots/org/eclipse/fennec/emf/) — `*-SNAPSHOT` | `snapshot.yml` on `snapshot` |
| Browse     | [search.maven.org `org.eclipse.fennec.emf`](https://search.maven.org/search?q=g:org.eclipse.fennec.emf) — find a specific version |                          |

The BOM coordinate to depend on the whole stack from a Gradle/Maven build:

```
org.eclipse.fennec.emf:org.eclipse.fennec.emf.osgi.bom:${fennec.version}
```

For BND workspaces, the same artifacts are consumed via the
`-library: fennec` library declaration provided by the
[`fennec.bnd.libraries`](https://github.com/eclipse-fennec/fennec.bnd.libraries)
repository.

## Secrets

The following repository / organisation secrets must be defined for
`snapshot.yml` and `release.yml` to succeed:

| Secret name                          | Purpose                                  |
|--------------------------------------|------------------------------------------|
| `CENTRAL_SONATYPE_TOKEN_USERNAME`    | Sonatype Central user token              |
| `CENTRAL_SONATYPE_TOKEN_PASSWORD`    | Sonatype Central token password          |
| `GPG_PRIVATE_KEY`                    | ASCII-armored GPG private key            |
| `GPG_PASSPHRASE`                     | Passphrase for the private key           |
| `GPG_KEY_ID`                         | Long-form key id (used by the build)     |

The GPG key is imported on the fly and the keyring is removed in a final
step that runs even when the job fails (`if: always()`). The build never
echoes secret values.

## Adding a new workflow

* Place the file under [`.github/workflows`](../.github/workflows).
* Add a license header is **not** required (the directory is in
  `paths-ignore`), but a top-of-file `name:` is appreciated for the Actions
  UI.
* Restrict publishing workflows to specific branches with `branches:` and
  exclude pull requests with `branches-ignore: [ '*' ]` (the pattern this
  repo already uses) so secrets are never exposed to PR runs from forks.
* Pin third-party actions to a major tag (`@v4`) — never to `main`.

## Reproducing CI locally

* Full PR build:
  ```bash
  ./gradlew clean build perfTest --info
  ```
* License headers:
  ```bash
  docker run --rm -v $(pwd):/github/workspace \
    ghcr.io/apache/skywalking-eyes/license-eye header check
  ```
* The snapshot/release workflows cannot be reproduced locally because they
  publish to Sonatype Central and require the project signing key.

# Eclipse Release Guide for Fennec Projects

How to bring a Fennec repository to the point where it can do a proper
Eclipse Foundation release. This repository (`emf.osgi`, released 1.0.0 in
July 2026) is the reference implementation — every file mentioned below can
be copied from here and adapted. The usual adaptation points are the
repository name, the GitHub Pages paths and the supported version in
`SECURITY.md`; everything else is largely verbatim.

Work through the sections in order; the release-day runbook is at the end.

## 1. Repository documents

The Eclipse Foundation release review checks that these exist and are
current. All of them live at the repository root:

| File | Source / template | Adaptation |
|------|-------------------|------------|
| `LICENSE` | EPL-2.0 full text | none |
| `NOTICE.md` | copy from this repo | project home, repo URL, notable dependencies |
| `README.md` | — | must link the docs, state the branch model and the Maven coordinates |
| `CONTRIBUTING.md` | copy from this repo | repo URLs; keep ECA + DCO sign-off + Dash sections |
| `CODE_OF_CONDUCT.md` | [Eclipse Community Code of Conduct 2.0](https://www.eclipse.org/org/documents/Community_Code_of_Conduct.php) full text | none |
| `SECURITY.md` | [Eclipse security-handbook template](https://github.com/eclipse-csi/security-handbook/blob/main/templates/SECURITY.md) | GitHub advisories URL (`…/<repo>/security/advisories/new`), **supported versions — update every release** |

Checklist:

- [ ] All six files present and adapted
- [ ] `CONTRIBUTING.md` points security reports at `SECURITY.md` (coordinated
      disclosure), not at a mailing list
- [ ] `SECURITY.md` names the currently supported release stream

Longer term the static ones (`CODE_OF_CONDUCT.md`, `SECURITY.md`) should move
to an `eclipse-fennec/.github` org-defaults repository; until that exists,
copy them per repo.

## 2. License headers

Every source file carries the EPL-2.0 header, enforced in CI:

- [ ] `.licenserc.yaml` copied and adapted (mind the `paths-ignore` list —
      generated code, `docs-site/**`, markdown etc. are exempt)
- [ ] `.github/workflows/license.yml` (apache/skywalking-eyes) enabled

## 3. IP cleanliness (Dash license check)

The Eclipse IP process requires every third-party dependency to be vetted.
The pipeline is fully documented — do not reinvent it:

- Setup / replication guide: [`tools/HOWTO-add-ip-dash-license-check.md`](../tools/HOWTO-add-ip-dash-license-check.md)
  (including the gotcha that Dash's `--project` wants the dotted **PMI id**,
  e.g. `technology.fennec`, not the GitHub org)
- User-facing docs: [`docs/ip-dash-license-check.md`](ip-dash-license-check.md)

Checklist:

- [ ] `tools/dash-licenses.sh` / `.bat` copied
- [ ] `.github/workflows/dash-licenses.yml` enabled
- [ ] `DEPENDENCIES` generated, reviewed and committed at the repo root
- [ ] Zero `restricted` entries at release time (submit IP reviews with
      `--review` for anything unvetted, then wait for approval)

## 4. CI and publishing pipeline

The branch model and the four core workflows are documented in
[`docs/ci.md`](ci.md). Summary:

- `snapshot` is the development branch: every push publishes `-SNAPSHOT`
  artifacts to Sonatype Central snapshots (and deploys the docs site).
- `main` holds the latest release: **a push to `main` IS the release
  trigger** — `release.yml` runs with `DO_RELEASE=true` and publishes signed
  artifacts to Maven Central, which is immutable.
- PRs are excluded from the publishing workflows so secrets never reach fork
  code.

Checklist:

- [ ] `build.yml`, `license.yml`, `snapshot.yml`, `release.yml` copied and
      adapted
- [ ] Repository secrets set: `CENTRAL_SONATYPE_TOKEN_USERNAME`,
      `CENTRAL_SONATYPE_TOKEN_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`,
      `GPG_KEY_ID`
- [ ] `cnf/build.bnd` sets `github-orga`, `github-project`, `-groupid` and
      `maven-central: true` (the fennec bnd library handles the rest)

## 5. Release OBR and baselining

Each release publishes an OSGi repository with the **exact** bundles that
went to Maven Central; the workspace baselines against it in the next cycle.
Reference: the `obr` job in [`.github/workflows/release.yml`](../.github/workflows/release.yml)
and the `Release`/`Baseline` blocks in [`cnf/build.bnd`](../cnf/build.bnd).

How it works:

1. `-releaserepo.obr` (merged property, conditional on `DO_RELEASE=true`)
   makes the single release run also release into the local `Release`
   LocalIndexedRepo (`cnf/release`). Never populate the OBR with a second
   `gradlew release` run — the artifacts would differ from Maven Central.
2. The `obr` job uploads `cnf/release` as an artifact and force-pushes it as
   the single-commit orphan branch `release-obr`, reachable at
   `https://raw.githubusercontent.com/eclipse-fennec/<repo>/release-obr/index.xml`.
3. After the **first** release exists, baselining is enabled:
   `-plugin.baseline` (OSGiRepository on that URL), `-baseline: *` and
   `-diffpackages: *;threshold=MINOR` (MICRO-level noise from generated
   version constants is ignored; API additions and breakages still fail the
   build).

Checklist:

- [ ] `-plugin.release` + `-releaserepo.obr` in `cnf/build.bnd`
- [ ] `obr` job in `release.yml`
- [ ] After the first release: baseline block enabled, `base-version` bumped

Note: this configuration is slated to move into the fennec bnd library
(`fennec-baselining: true` flag) — check whether the library already provides
it before copying the blocks.

## 6. Documentation site (recommended)

User documentation is published via a VitePress site under
`https://eclipse-fennec.github.io/<repo>/snapshot/`. Reference: the
[`docs-site/`](../docs-site) folder and the `docs`/`deploy` jobs in
`snapshot.yml`. Key properties of the setup:

- Publication is an explicit allowlist (`docs-site/guides.mjs`) — repository
  markdown stays the single source of truth, internal dev docs stay
  unpublished.
- Deploys only run after a successful build, and only from the `snapshot`
  branch (each Pages deploy replaces the whole site).
- GitHub Pages must be set to build type "GitHub Actions" in the repo
  settings.

## 7. Eclipse Foundation process (outside the repository)

- [ ] **Release record** created in the [PMI](https://projects.eclipse.org/projects/technology.fennec)
      with the planned date
- [ ] **Release review** completed for major releases *before* the artifacts
      are published (a push to `main` publishes — do not merge before the
      review concludes)
- [ ] **PMI download information** updated with the Maven Central coordinates
- [ ] Project website carries the Eclipse Foundation
      [trademark attribution and footer](https://www.eclipse.org/projects/handbook/#trademarks-website-footer)
- [ ] Community entry points: `good first issue` / `help wanted` labels,
      roadmap announcements (release-review recommendation)

## 8. Release-day runbook

1. **Prepare** on a branch off `snapshot`:
   - update `SECURITY.md` supported versions,
   - regenerate `DEPENDENCIES` and confirm zero restricted entries,
   - confirm `base-version` (`cnf/ext/version.bnd`) is the release version and
     generated version constants picked it up (run a build),
   - write release notes (diff against the previous tag).
2. **Merge to `snapshot`**, wait for green CI (build matrix, license, Dash,
   docs deploy).
3. Confirm the **release review** is done (major releases).
4. **Merge `snapshot` → `main`.** This publishes to Maven Central
   (irreversible) and pushes the release OBR to `release-obr`.
5. **Tag** the release (`git tag <version>` on `main`) and create the GitHub
   release with the notes.
6. **Post-release**, on `snapshot`:
   - bump `base-version` to the next development version,
   - verify baselining is active and green against the new OBR,
   - update the PMI (release record marked done, download info).

# HOW-TO: Add an IP Dash license check to a bnd workspace

Agent-facing guide (for future Claude instances). This documents how to set up
the `bnd repo deps` + Eclipse Dash License Tool ("IP Dash") pipeline that lives
in this repo, so it can be replicated in other Eclipse bnd projects.

## What it does

1. `bnd repo deps` (bnd **7.4.0-SNAPSHOT or newer**) exports the Maven GAVs of
   every artifact the bnd workspace resolves, as a flat newline-separated list,
   with index-file macros (e.g. `{junit.version}`) resolved.
2. The [Eclipse Dash License Tool](https://github.com/eclipse-dash/dash-licenses)
   consumes that list and writes a `DEPENDENCIES` summary, classifying each
   dependency as `approved` or `restricted` (needs IP review).
3. Optionally (`--review`), Dash opens IP review requests in the Eclipse GitLab
   IP Lab for the restricted dependencies.

The Dash tool's exit code is the **number of restricted dependencies** (0 = all
approved). CI relies on this to fail the build when something is unvetted.

## Prerequisites

- The workspace must build with bnd (a `cnf/` bnd workspace). `bnd repo deps` is
  a subcommand of the **CLI**, downloaded on demand — it does NOT require
  bumping the workspace's own bnd version.
- Java 17+ on PATH (the tool jars are plain executable jars).
- `curl` and (for the shell script) a POSIX shell / Git Bash.

## Files to copy into the target project

All of these are self-contained and parameterised; copy verbatim and they work:

| File | Purpose |
|------|---------|
| `tools/dash-licenses.sh`  | Linux/macOS/Git Bash runner. Downloads+caches the bnd CLI snapshot and dash-licenses into `cnf/cache/dash-licenses/` (gitignored), runs the two steps, exits with the restricted count. |
| `tools/dash-licenses.bat` | Windows `cmd.exe` runner, same behaviour. |
| `.github/workflows/dash-licenses.yml` | GitHub Actions: runs the script on PRs + pushes to primary branches, fails on restricted, uploads `DEPENDENCIES`. |
| `.gitlab-ci.yml` | GitLab CI: same, on MRs + pushes. Runs `--review` automatically if `DASH_IPLAB_TOKEN` + `DASH_PROJECT_ID` CI variables are set. |
| `docs/ip-dash-license-check.md` | User-facing docs. |

The scripts auto-resolve the **latest** bnd snapshot from
`maven-metadata.xml` (timestamp + buildNumber), so there is no hardcoded build
number to maintain. Override via `--bnd-version` / `--dash-version` if needed.

## Running it locally

```bash
tools/dash-licenses.sh            # regenerate DEPENDENCIES, review the diff, commit manually
tools/dash-licenses.bat           # Windows equivalent
```

## Submitting IP review issues (`--review`)

```bash
export DASH_IPLAB_TOKEN=<gitlab.eclipse.org API token>
tools/dash-licenses.sh --review --project <ECLIPSE-PMI-PROJECT-ID>
```

### ⚠️ Gotcha: `--project` is the Eclipse **PMI project id**, not the GitHub org

This is the failure that motivated this doc. Passing the GitHub org / repo slug
(e.g. `eclipse-fennec`) produces:

```
Exception in thread "main" java.lang.RuntimeException: org.eclipse.dash.api.EclipseApi: An error occurred while calling the API.
	at org.eclipse.dash.api.EclipseApi.getApiData(EclipseApi.java:174)
	at org.eclipse.dash.api.EclipseApi.getProject(EclipseApi.java:127)
	at org.eclipse.dash.licenses.validation.EclipseProjectIdValidator.validate(EclipseProjectIdValidator.java:31)
	at org.eclipse.dash.licenses.cli.Main.main(Main.java:78)
```

Dash validates `-project` against the Eclipse PMI API and throws (instead of a
clean message) when the id doesn't resolve. The id must be the **dotted PMI
project id** used by <https://projects.eclipse.org>, e.g. `technology.fennec`,
`modeling.emf`, `technology.osgi-technology` — NOT `eclipse-fennec`.

Verify a candidate id before running (HTTP 200 = valid, 404 = wrong id):

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://projects.eclipse.org/api/projects/<id>
```

Or search for the project at <https://projects.eclipse.org/> and read the id
from the project page URL. Hints for finding it in the repo itself: check
`.licenserc.yaml`, `NOTICE.md`, `pom.xml` `<groupId>`, or existing
`DEPENDENCIES` entries — Dash annotates approved Eclipse-hosted deps with their
PMI project id in the last column.

### Token

- The token is a **gitlab.eclipse.org personal access token** with `api` scope,
  belonging to a committer on the target project.
- Never commit it or echo it into logs. Pass it via the `DASH_IPLAB_TOKEN`
  environment variable (both scripts read it) or `--token`.
- In CI, store it as a **masked** variable `DASH_IPLAB_TOKEN` and set
  `DASH_PROJECT_ID` to the PMI id.

## Notes / conventions observed in this repo

- `DEPENDENCIES` lives at the repo root and is committed **manually** after
  review — CI does not commit it (it only uploads it as an artifact).
- `.gitlab-ci.yml` and `**/*.yml` are ignored by `.licenserc.yaml`; the `.sh` and
  `.bat` carry the EPL-2.0 header.
- Tool jars are cached under `cnf/cache/dash-licenses/` which is already covered
  by the gitignored `cnf/cache/`.
- Some artifacts show as `restricted` simply because they are not yet vetted;
  this is expected until the IP reviews are approved. Do not "fix" this by
  excluding them — either submit the reviews (`--review`) or wait for approval.

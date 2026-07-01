# IP Dash / License Check

This project tracks the license status of its third-party dependencies in the
[`DEPENDENCIES`](../DEPENDENCIES) file, using the
[Eclipse Dash License Tool](https://github.com/eclipse-dash/dash-licenses)
("IP Dash").

The dependency list is produced directly from the bnd workspace with the
`bnd repo deps` subcommand (bnd 7.4.0-SNAPSHOT or newer), so it always reflects
exactly the Maven artifacts the workspace resolves — no separate manifest to
maintain.

## Regenerating `DEPENDENCIES`

Run the helper script (Linux/macOS, or Git Bash on Windows):

```bash
tools/dash-licenses.sh
```

…or on Windows from `cmd.exe`:

```bat
tools\dash-licenses.bat
```

It will:

1. Download the bnd CLI snapshot and the dash-licenses tool into the gitignored
   `cnf/cache/dash-licenses/` (cached between runs).
2. Run `bnd repo deps` to export every Maven GAV the workspace uses.
3. Run dash-licenses to write the `DEPENDENCIES` summary at the repo root.

The script exits with the number of **restricted** dependencies (`0` when all
are approved). Review the diff and **commit `DEPENDENCIES` manually**.

## Opening IP review requests

Dependencies marked `restricted` are not yet vetted by the Eclipse Foundation
and need an IP review. To create the review issues automatically in the
[Eclipse GitLab IP Lab](https://gitlab.eclipse.org/eclipsefdn/emo-team/iplab):

```bash
export DASH_IPLAB_TOKEN=<your-gitlab.eclipse.org-api-token>
tools/dash-licenses.sh --review --project <eclipse-project-id>
```

`--project` is the Eclipse project short name (e.g. `technology.fennec`); it can
also be supplied via `DASH_PROJECT_ID`. A GitLab API token from
`gitlab.eclipse.org` is required and must **not** be committed.

## Continuous integration

Both CI systems run the same `tools/dash-licenses.sh` script, **fail the job**
if any dependency is still restricted, and publish the generated `DEPENDENCIES`
file as an artifact:

- **GitHub Actions** — [`License Check (IP Dash)`](../.github/workflows/dash-licenses.yml),
  on pull requests and pushes to `main`/`snapshot`.
- **GitLab CI** — [`.gitlab-ci.yml`](../.gitlab-ci.yml), on merge requests and
  pushes to `main`/`snapshot`. If the masked CI variables `DASH_IPLAB_TOKEN` and
  `DASH_PROJECT_ID` are configured, the job additionally opens IP review issues.

> Note: until the currently `restricted` artifacts are approved through IP
> review, this workflow will report a failure — that is by design, it surfaces
> the dependencies that still need vetting.

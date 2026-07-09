# Contributing to FairTrack

Work on FairTrack is tracked in Jira. Every change should be traceable back to an issue,
so that branches, commits and pull requests show up automatically in the issue's
**Development** panel.

## One-time setup

Enable the repository's git hooks after cloning — git does not do this for you:

```bash
git config core.hooksPath .githooks
```

## Issue keys

The Jira project key is **`FAIR`**, so issues are `FAIR-1`, `FAIR-2`, and so on.

Keys must be **uppercase**. Jira does not recognise `fair-12`, only `FAIR-12`.

## Branch names

Start the branch with the issue key, followed by a short slug:

```bash
git checkout -b FAIR-12-fasting-countdown
```

## Commit messages

Put the key at the start of the subject line:

```
FAIR-12 fix negative countdown after fasting goal is reached
```

You rarely have to type it. The `prepare-commit-msg` hook reads the key from the branch
name and prepends it for you, so this is enough:

```bash
git checkout -b FAIR-12-fasting-countdown
git commit -m "fix negative countdown"   # becomes: FAIR-12 fix negative countdown
```

The `commit-msg` hook then rejects any subject without a key. For a commit that genuinely
belongs to no issue — repository tooling, for instance — bypass it explicitly:

```bash
git commit --no-verify -m "raise gradle memory limit"
```

Merge, revert and autosquash (`fixup!` / `squash!`) subjects are always allowed through.

## Pull requests

Include the key in the PR title, or branch from a name that already contains it. Either is
enough for Jira to pick the pull request up.

## Smart commits are not used

Jira supports *smart commits* — commands such as `#comment`, `#time` and `#close` embedded
in a commit message — which move an issue through its workflow automatically.

FairTrack does **not** use them, by choice. Smart commits identify the acting user by
matching the commit author's email address against a Jira account. That would require
committing under a real, personal email address, which is then permanently visible in this
public repository. Commits here are authored with a GitHub `noreply` address instead.

Issue keys alone give the linking; issue status is moved in Jira by hand.

## Requirements on the Jira side

Linking only works once the **GitHub for Jira** app is installed on the Jira site and this
repository is connected to it. Without that, keys in commit messages are inert. Viewing the
links additionally requires the *View development tools* permission in Jira.

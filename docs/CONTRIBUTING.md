# Contributing to `Installr`

First of all: **thank you for your interest in contributing to `Installr`!** 🎉

`Installr` is an early-stage open source project focused on managing and installing software packages in clustered environments (e.g., Docker Swarm). The project is still evolving, and contributions of all kinds are very welcome.

---

## Project Status

`Installr` is currently in an early development phase.

This means:

* APIs and internal structures may change.
* Some components may be incomplete or experimental.
* Documentation may not yet cover all cases.

Contributing at this stage means you help shape the architecture and direction of the project.

---

## How To Contribute

Contributions are welcome and are handled through Pull Requests (PRs).

To contribute, open a Pull Request and:

* clearly describe what your change does,
* explain the motivation behind it,
* reference related issues if available, and
* mention breaking changes explicitly.

Your changes should:

* be focused and minimal,
* follow existing code style and conventions,
* add or update documentation where necessary,
* update or add tests where applicable, and
* include proper commit messages and sign-offs (see DCO below).

---

## Review Process & Patience

Please understand that this project is maintained in spare time.

* Not every PR can be reviewed immediately.
* Reviews may take some time.
* Feedback may require iterations.

---

## Developer Certificate of Origin (DCO)

This project uses the **Developer Certificate of Origin (DCO)**.

By contributing, you certify that:
* You have the right to submit your contribution.
* The work is your own, or you have the right to pass it on under the project's licenses.
* You agree that your contribution may be distributed under the project's licenses: the AGPL with the additional
  permissions of the [Installr Linking Exception](https://github.com/installr-io/installr/blob/main/LINKING-EXCEPTION), or the LGPL for the libraries in `lib/`.

### Sign-Off Requirement

All commits must be signed off using:

```bash
git commit -s
```

This adds a line like:

```text
Signed-off-by: Your Name <your.email@example.com>
```

Pull requests without proper sign-off cannot be merged.

For more details, see: [https://developercertificate.org/](https://developercertificate.org/)

---

## Commit Messages

Good commit messages make the project history readable and help reviewers understand the intent of a change.

### Rules

1. Separate the subject from the body with a blank line
2. Limit the subject line to 72 characters
3. Capitalize the subject line
4. Do not end the subject line with a period
5. Use the imperative mood in the subject line
6. Wrap the body at about 72 to 80 characters
7. Use the body to explain **what** and **why**, not **how**

### Example

```text
Summarize the change in up to 72 characters, imperative mood

If needed, add a body here - separated from the subject by a blank line.
Explain what changed and why this change was necessary. Are there side effects
or other unintuitive consequences of this change? Here's the place to explain
them. Wrap lines at around 72 to 80 characters. Leave out implementation
details; the code already shows how.

Further paragraphs come after blank lines.

 - Bullet points are okay, too

 - Typically a hyphen or asterisk is used for the bullet, preceded
   by a single space, with blank lines in between, but conventions
   vary here

Signed-off-by: Your Name <your.email@example.com>
```

A body is not always necessary. For small, self-explanatory changes a single subject line is sufficient.

---

## Separate your changes

Each commit must represent exactly one **logical change**. A logical change is a self-contained unit of work that can be understood, reviewed, and reverted independently.

If you find yourself writing "and" in the subject line, your commit likely contains more than one change and should be split.

What constitutes a single logical change is a matter of judgment. As a rule of thumb: a bug fix and its test belong together. A refactoring and a behavior change do not. Conversely, if a single logical change touches many files, all those changes belong in one commit - the number of files changed is not a reason to split.

Every commit in a series **must** leave the codebase in a buildable and passing state.

---

## Code Style & License Compliance

To ensure a consistent codebase and proper license attribution across all source files, this project uses Maven plugins for both license header management and code formatting.

### License Header

All source files must include the appropriate license header. You can automatically add or update these headers by running:

```bash
mvn license:format
```

This command applies the configured license template to all relevant files and ensures that existing headers are up to date.

### Code Formatting

Consistent code formatting is enforced using a Maven formatter plugin. To format the codebase, run:

```bash
mvn formatter:format
```

The formatter uses the **Eclipse code formatter**, with a custom style defined [here](https://github.com/installr-io/installr/blob/main/code-format/src/main/resources/java-code-format.xml). Please run this command before committing changes to keep the codebase clean and consistent.

---

We recommend running both commands before submitting a pull request to avoid unnecessary changes during code review.

---

## Branching Model

The project follows a simple trunk-based model with an "upstream first" policy for bug fixes.

### Branches and Tags

- **`main`** is the development branch. It always carries the next version as a snapshot (e.g. `0.3.0-SNAPSHOT`).
- **Releases** are tags on `main` (e.g. `0.2.0`). There is no separate release branch for the current version.
- **Maintenance branches** (`release/X.Y.x`, e.g. `release/0.2.x`) are created from a release tag only when a patch
  release is needed. They are never merged back into `main`.

### Contributing Changes

1. Create a short-lived branch from `main` (e.g. `feature/ssh-deployer` or `fix/null-pointer-in-liquibase-deployer`).
2. Open a pull request against `main`.
3. Once reviewed and CI passes, the PR is merged into `main`.

### Bug Fixes (Upstream First)

All bug fixes go into `main` first, even if they affect an older release. Maintainers then backport them to the relevant
maintenance branches using `git cherry-pick -x`, so each backported commit references its origin.

Only open a pull request directly against a `release/X.Y.x` branch if the bug does not exist on `main`
(e.g. the affected code has since been removed or rewritten). Please mention this in the PR description.

---

## Use of AI Coding Assistants

AI tools are permitted and can be a helpful aid when contributing to `Installr`. However, human contributors remain fully responsible for every contribution they submit.

### Responsibilities

When using AI assistance, you as the contributor are responsible for:

* Reviewing all AI-generated code for correctness, security, and license compliance,
* Ensuring the contribution meets the project's code style and quality standards,
* Verifying that no AI-generated code introduces incompatible licensing (see AGPL-3.0 and LGPL-3.0),
* Adding your own `Signed-off-by` tag to certify the DCO.

AI tools **MUST NOT** add `Signed-off-by` tags. Only humans can legally certify the Developer Certificate of Origin (DCO).

### Attribution

If a contribution was significantly shaped by an AI tool, please include an `Assisted-by` tag in the commit message:

```text
Assisted-by: AGENT_NAME:MODEL_VERSION
```

Example:

```text
Assisted-by: Claude:claude-sonnet-4-6
```

This helps track the evolving role of AI in the project's development history and is appreciated, but not strictly required.

---

## Getting Started

To get up and running quickly, check out the [Developer Guide](DEVELOPER_GUIDE.md), which walks you through everything you need to know to get started.

---

## Questions?

If something is unclear, feel free to open an issue and ask.

We’re building `Installr` together - thanks for being part of it. 🚀

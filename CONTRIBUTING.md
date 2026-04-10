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
* The work is your own, or you have the right to pass it on under the project's license (AGPL).
* You agree that your contribution may be distributed under the project's license.

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

The formatter is uses the **Eclipse code formatter**, with a custom style defined [here](code-format/src/main/resources/java-code-format.xml). Please run this command before committing changes to keep the codebase clean and consistent.

---

We recommend running both commands before submitting a pull request to avoid unnecessary changes during code review.

---

## Getting Started

To get up and running quickly, check out the [Developer Guide](docs/DEVELOPER_GUIDE.md), which walks you through everything you need to know to get started.

---

## Questions?

If something is unclear, feel free to open an issue and ask.

We’re building `Installr` together — thanks for being part of it. 🚀

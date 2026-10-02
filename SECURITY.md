<p align="center">
  <img src="docs/assets/wakesync-mark.svg" alt="WakeSync mark" width="110" />
</p>

# 🔒 Security Policy

WakeSync handles sleep information through Android Health Connect, so privacy and alarm reliability are security-sensitive parts of the product.

## Supported version

The current development version on `main` is the supported code line.

## Reporting a vulnerability

Do not post exploit details, credentials, tokens, personal health information, or device identifiers in a public issue.

If GitHub offers **Private vulnerability reporting** for this repository, use that channel. Otherwise, contact the repository owner through GitHub first and coordinate a private disclosure path before sharing sensitive technical details.

Public issues are appropriate for non-sensitive hardening suggestions and ordinary bugs.

## High-priority security areas

Please treat issues in these areas as security-sensitive:

- unauthorized Health Connect access;
- accidental health-data upload or logging;
- secrets committed to the repository;
- alarm behavior that can silently disable the hard-stop wake;
- exported Android components that should not be externally callable;
- unsafe PendingIntent usage;
- permission bypasses;
- full-screen alarm abuse;
- sensitive data exposed through notifications or logs.

## Health-data handling

WakeSync should:

- request only the Health Connect access it needs;
- remain read-only for sleep data;
- process sleep data locally;
- store derived history rather than duplicating raw records when possible;
- avoid logging raw health records in production;
- never include real personal health data in tests or bug reports.

## Alarm reliability

Changes to alarm scheduling should preserve the independent hard-stop alarm. A live or predictive optimization must never become the only alarm path.

# Security policy

Sculpt stores private photos, so security and privacy reports are taken seriously.

## Reporting a vulnerability

Please **don't open a public issue** for a security problem. Instead, use GitHub's private vulnerability reporting: open the **Security** tab of the repository, then choose **Report a vulnerability**.

Include what you found, how to reproduce it, and which version of the app is affected. You'll get a reply within about a week, and a fix or a plan as soon as it can be made.

## What counts

- Anything that exposes photos or pose data to another app or person, such as files written to shared storage or exported components.
- Anything that adds network access or sends data off the device.
- Build or signing problems that let someone ship a tampered APK.

## Supported versions

Only the latest release gets fixes.

## Design promises

- The app declares only the camera permission and no internet permission.
- Photos live in app-private storage.
- Android backup is disabled (`allowBackup="false"`).

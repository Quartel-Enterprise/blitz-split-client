# Security policy

Blitz Split keeps people's groups, bills, payments and Pix keys, so we take reports of security problems seriously.

## Supported versions

Only the newest version of each app gets security fixes: the Android app on Google Play, the iPhone, iPad and Mac app
on the App Store, and the web app at [app.blitzsplit.com](https://app.blitzsplit.com). The `main` branch of this repository is
what the next release ships.

## Reporting a vulnerability

**Please don't open a public issue for a security problem.** Report it privately instead:

1. Open the repository's **Security** tab and click **Report a vulnerability**.
2. Describe the problem, the platform and version (or the commit), how to reproduce it, and what an attacker could do
   with it.

Only the maintainers see the report. We aim to answer within 7 days, keep you posted while we fix it, and credit you
in the advisory once the fix is out, unless you'd rather stay anonymous.

## What is in scope

- The apps in this repository: Android, iOS, iPadOS, macOS and the web, and the shared Kotlin code under them.
- How they store data on the device (Room's SQLite, OPFS on the web), the recovery code, and the sign in with Google
  and Apple.
- How they talk to the backend: authentication, row level access to groups, invite links and QR codes.

The backend itself (the Supabase migrations and edge functions) and the site at [blitzsplit.com](https://blitzsplit.com),
with its invite pages, live in other repositories; report problems with them the same way, here.

## Out of scope

- Problems that need a device that is already compromised (rooted, jailbroken, or with malware installed).
- Reports from automated scanners with no proof that the problem can be exploited.
- Denial of service, spam, and social engineering of the maintainers or of people using the app.
- Missing best practices with no real impact (for example, a header that changes nothing for this app).

Please don't access, change or delete data that isn't yours while you investigate, and give us a reasonable time to
fix the problem before you disclose it.

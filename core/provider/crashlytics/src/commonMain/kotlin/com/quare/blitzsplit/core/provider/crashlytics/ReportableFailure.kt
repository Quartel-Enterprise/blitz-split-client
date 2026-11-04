package com.quare.blitzsplit.core.provider.crashlytics

/*
 * Why: a failure whose message was written to be reported, with no name, amount nor id, and no cause: CrashReporter
 * sends it as it is, while any other throwable goes as the names of its classes only.
 */
interface ReportableFailure

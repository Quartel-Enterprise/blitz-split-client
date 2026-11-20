package com.quare.blitzsplit.core.watch.domain.model

// Why: a code nobody asked for, or one already used (404 invalid_code).
class InvalidWatchPairingCodeException : Exception("The watch pairing code is invalid")

// Why: the code's 5 minutes passed (410 expired), on the phone approving it or on the watch polling it.
class ExpiredWatchPairingException : Exception("The watch pairing code expired")

// Why: too many wrong codes typed in a row (429 too_many_attempts).
class TooManyWatchPairingAttemptsException : Exception("Too many watch pairing attempts")

// Why: a local only profile has no account on the server to share with a watch (15.11b).
class WatchPairingNeedsAccountException : Exception("A local only profile can't connect a watch")

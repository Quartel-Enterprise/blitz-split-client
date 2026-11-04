#!/bin/bash

# Runs a heavy build command once no other heavy command is running on this machine, in any checkout or worktree:
#   ./scripts/heavy-check.sh ./gradlew jvmTest :koverVerifyCi --continue
#   ./scripts/heavy-check.sh xcodebuild -project iosApp/BoraRachar.xcodeproj -scheme BoraRachar-iOS ... build
#
# Why: each worktree's Gradle daemon (8 GB of heap), its Kotlin daemon (4 GB) and Kotlin/Native need more memory than
# the Mac has for two or three sessions building at once. Side by side they swap, and a check of three minutes takes an
# hour; one after the other, each takes its few minutes.
#
# The lock is one file every checkout shares, held with flock(2) by this script's own process: a command that ends,
# fails or is killed releases it, so no stale lock is ever left. The command doesn't inherit it, so a daemon it starts
# doesn't hold the queue either. A SIGTERM or SIGHUP sent to this script goes on to the command, and a terminal's Ctrl-C
# reaches the command by itself, so a signal this script can catch never releases the queue while its build still runs;
# a SIGKILL can't be caught, and leaves the build running outside the queue. Waiters poll the lock, so they get it in no
# set order. A heavy-check.sh run by the command (a whole step wrapped in one call) runs straight away: it already holds
# the queue, and waiting would wait for itself. Exits with the command's exit code.
#
# Environment:
#   HEAVY_CHECK_LOCK  the lock file (default ~/.cache/blitzsplit/heavy-checks.lock)
#   HEAVY_CHECK_HELD  set by this script for the command it runs

set -eo pipefail

if [ $# -eq 0 ]; then
  echo "usage: $0 <command> [args...]" >&2
  exit 2
fi

if [ -n "$HEAVY_CHECK_HELD" ]; then
  exec "$@"
fi

LOCK="${HEAVY_CHECK_LOCK:-$HOME/.cache/blitzsplit/heavy-checks.lock}"
mkdir -p "$(dirname -- "$LOCK")"

exec perl -MFcntl=:flock -MIO::Handle -MCwd=getcwd -MPOSIX -e '
  my $lock = shift @ARGV;
  open(my $fh, "+>>", $lock) or die "heavy-check: cannot open $lock: $!\n";
  # Says who holds the queue each time that changes, so a long wait shows what it waits for.
  my $shown;
  until (flock($fh, LOCK_EX | LOCK_NB)) {
    seek($fh, 0, 0);
    my $holder = <$fh> // "";
    chomp $holder;
    if (!defined $shown || $holder ne $shown) {
      print STDERR "heavy-check: waiting for another heavy check to end", ($holder ne "" ? " ($holder)" : ""), "\n";
      $shown = $holder;
    }
    sleep 2;
  }
  truncate($fh, 0);
  print $fh "pid $$ in ", (getcwd() // "?"), ": ", substr(join(" ", @ARGV), 0, 120), "\n";
  $fh->flush;
  $ENV{HEAVY_CHECK_HELD} = 1;
  # Blocked around fork, so a TERM or HUP always finds $pid set and goes on to the command.
  my $signals = POSIX::SigSet->new(SIGTERM, SIGHUP);
  my $mask = POSIX::SigSet->new;
  sigprocmask(SIG_BLOCK, $signals, $mask);
  my $pid;
  $SIG{$_} = sub { kill $_[0], $pid } for qw(TERM HUP);
  # The command shares the terminal, so a Ctrl-C reaches it already: a second one would cut its own cancel short.
  $SIG{INT} = "IGNORE";
  $pid = fork() // die "heavy-check: cannot fork: $!\n";
  if ($pid == 0) {
    $SIG{$_} = "DEFAULT" for qw(TERM HUP INT);
    sigprocmask(SIG_SETMASK, $mask);
    exec { $ARGV[0] } @ARGV;
    print STDERR "heavy-check: cannot run $ARGV[0]: $!\n";
    exit 127;
  }
  sigprocmask(SIG_SETMASK, $mask);
  waitpid($pid, 0);
  exit(($? & 127) ? 128 + ($? & 127) : $? >> 8);
' "$LOCK" "$@"

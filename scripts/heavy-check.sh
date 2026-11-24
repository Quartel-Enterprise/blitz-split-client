#!/bin/bash

# Runs a heavy build command once no other heavy command is running on this machine, in any checkout or worktree:
#   ./scripts/heavy-check.sh ./gradlew jvmTest :koverVerifyCi --continue
#   ./scripts/heavy-check.sh xcodebuild -project iosApp/BoraRachar.xcodeproj -scheme BoraRachar-iOS ... build
#   ./scripts/heavy-check.sh --yield      # from a command it runs: lets the checks that wait go first
#
# Why: each worktree's Gradle daemon (8 GB of heap), its Kotlin daemon (4 GB) and Kotlin/Native need more memory than
# the Mac has for two or three sessions building at once. Side by side they swap, and a check of three minutes takes an
# hour; one after the other, each takes its few minutes.
#
# The lock is one file every checkout shares, held with flock(2) by this script's own process: a command that ends,
# fails or is killed releases it, so no stale lock is ever left. The command doesn't inherit it, so a daemon it starts
# doesn't hold the queue either. A SIGTERM or SIGHUP sent to this script goes on to the command, and a terminal's Ctrl-C
# reaches the command by itself, so a signal this script can catch never releases the queue while its build still runs;
# a SIGKILL can't be caught, and leaves the build running outside the queue. A heavy-check.sh run by the command (a
# whole step wrapped in one call) runs straight away: it already holds the queue, and waiting would wait for itself.
# Exits with the command's exit code.
#
# The queue is first come, first served. Each waiter keeps a ticket in <lock>.queue, numbered from a counter the
# waiters share (never from the clock, which can be set back), and only the lowest ticket's owner takes the lock: a
# script that takes the queue step after step (the store screenshots' lane) gets back in line behind whoever arrived
# meanwhile, where polling the lock alone let it take every turn. A ticket whose owner is gone, or stopped touching
# it, is dropped by the next waiter that looks. A waiter that was itself stopped for that long (a Mac that slept)
# drops only the tickets whose process is gone: the others slept too, and keep their place.
#
# --yield is for a command that holds the queue for long and has points where nothing of its own runs: between two of
# the languages the store screenshots capture, say. There it calls `heavy-check.sh --yield`, which returns at once
# when nobody waits, and otherwise hands the queue to the waiters, gets back in line and returns once the queue is the
# command's again. A check of a minute then waits for one language, not for all of them. Only checks of other
# checkouts get the turn: one of the command's own checkout could delete what the command is in the middle of (a
# `./gradlew clean`, its build folder), so it waits for the command to end, as every waiter did, and so does whoever
# is behind it in line: the command gets back in line right before it. Outside the queue --yield does nothing, so the
# script that calls it runs the same on CI.
#
# Environment:
#   HEAVY_CHECK_LOCK      the lock file (default ~/.cache/blitzsplit/heavy-checks.lock)
#   HEAVY_CHECK_HELD      set by this script for the command it runs
#   HEAVY_CHECK_HOLDER    set by this script for the command it runs: the process that holds the queue, which --yield asks
#   HEAVY_CHECK_CHECKOUT  the checkout a check belongs to (default: the git checkout it is run from), for the tests
#   HEAVY_CHECK_STALE     the seconds after which an untouched ticket is dropped (default 30), for the tests

set -eo pipefail

if [ $# -eq 0 ]; then
  echo "usage: $0 <command> [args...] | --yield" >&2
  exit 2
fi

LOCK="${HEAVY_CHECK_LOCK:-$HOME/.cache/blitzsplit/heavy-checks.lock}"

if [ "$1" != --yield ] && [ -n "$HEAVY_CHECK_HELD" ]; then
  exec "$@"
fi

mkdir -p "$(dirname -- "$LOCK")" "$LOCK.queue"
CHECKOUT="${HEAVY_CHECK_CHECKOUT:-$(git rev-parse --show-toplevel 2> /dev/null || pwd)}"

exec perl -MFcntl=:flock -MIO::Handle -MCwd=getcwd -MPOSIX=:signal_h,:sys_wait_h -MTime::HiRes=time,sleep -e '
  my $lock = shift @ARGV;
  my $checkout = shift @ARGV;
  my $queue = "$lock.queue";
  # A waiter touches its ticket every second: one left alone this long is a waiter that is gone.
  my $stale = $ENV{HEAVY_CHECK_STALE} || 30;
  # When this process last looked at the line.
  my $looked;

  sub read_line {
    my ($path) = @_;
    open(my $in, "<", $path) or return "";
    my $line = <$in> // "";
    close($in);
    chomp $line;
    return $line;
  }

  # The tickets of the waiters still there, first in line first. Drops the ones whose owner is gone: its process, or
  # its touch. A process that has not looked for a while was stopped itself, as a Mac that sleeps stops them all, and
  # an old touch then says nothing: that look goes by the processes alone.
  sub waiting {
    my $now = time();
    my $is_watching = defined $looked && $now - $looked < $stale / 2;
    $looked = $now;
    opendir(my $dir, $queue) or return ();
    my @tickets = sort grep { /^\d+(\.5)?-\d+$/ } readdir($dir);
    closedir($dir);
    my @live;
    for my $ticket (@tickets) {
      my ($pid) = $ticket =~ /-(\d+)$/;
      my $touched = (stat("$queue/$ticket"))[9];
      next unless defined $touched;
      if ((kill(0, $pid) || $!{EPERM}) && !($is_watching && $now - $touched >= $stale)) {
        push @live, $ticket;
      } else {
        unlink "$queue/$ticket";
      }
    }
    return @live;
  }

  # The next place in line: a count the waiters share, taken under a lock of its own.
  sub next_ticket {
    open(my $count, "+>>", "$queue.next") or die "heavy-check: cannot open $queue.next: $!\n";
    flock($count, LOCK_EX);
    seek($count, 0, 0);
    my $number = (<$count> // 0) + 1;
    truncate($count, 0);
    print $count "$number\n";
    close($count);
    return sprintf("%017d-%d", $number, $$);
  }

  # Asks the holder to hand the queue to the waiters, if there are any, and returns once it answered: with the queue
  # its own again, or kept all along.
  sub yield_turn {
    my ($holder) = @_;
    exit 0 unless waiting();
    my $turn = "$lock.turn.$holder";
    my $before = read_line($turn);
    kill("USR1", $holder) or exit 0;
    # The holder writes a new count as its answer. One that is gone has no queue to wait for.
    sleep 0.2 while read_line($turn) eq $before && kill(0, $holder);
    exit 0;
  }

  if ($ARGV[0] eq "--yield") {
    my $holder = $ENV{HEAVY_CHECK_HOLDER} // "";
    # Only the process the lock file names is asked: the variable outlives its holder in whatever the command left
    # running, and that number may be another process by now.
    exit 0 unless $holder =~ /^\d+$/ && read_line($lock) =~ /^pid $holder /;
    yield_turn($holder);
  }

  open(my $fh, "+>>", $lock) or die "heavy-check: cannot open $lock: $!\n";
  my $ticket = next_ticket();
  my $turn = "$lock.turn.$$";
  my $pid;
  my $held = 0;

  # Gives the lock back, and with it the line that says who holds it: a waiter never reads the name of a check that
  # is over.
  sub release {
    truncate($fh, 0);
    flock($fh, LOCK_UN);
    $held = 0;
  }

  # Ends as the command ended.
  sub finish {
    my ($status) = @_;
    release() if $held;
    unlink $turn;
    exit(($status & 127) ? 128 + ($status & 127) : $status >> 8);
  }

  # Waits in line and takes the lock. With a command already running (--yield gave its turn away), a command that
  # ended meanwhile ends this script too.
  sub take_turn {
    my $mine = "$queue/$ticket";
    # Says who holds the queue each time that changes, so a long wait shows what it waits for.
    my $shown;
    while (1) {
      # The same name again if another waiter took this one for gone: the place in line stays. It says which
      # checkout waits, for a holder asked to give its turn away.
      if (!-e $mine && open(my $out, ">", $mine)) { print $out "$checkout\n"; close($out) }
      utime(undef, undef, $mine);
      my @line = waiting();
      last if @line && $line[0] eq $ticket && flock($fh, LOCK_EX | LOCK_NB);
      if (defined $pid && waitpid($pid, WNOHANG) == $pid) {
        unlink $mine;
        finish($?);
      }
      my $holder = read_line($lock);
      if (!defined $shown || $holder ne $shown) {
        print STDERR "heavy-check: waiting for another heavy check to end", ($holder ne "" ? " ($holder)" : ""), "\n";
        $shown = $holder;
      }
      sleep 1;
    }
    unlink $mine;
    truncate($fh, 0);
    print $fh "pid $$ in ", (getcwd() // "?"), ": ", substr(join(" ", @ARGV), 0, 120), "\n";
    $fh->flush;
    $held = 1;
  }

  take_turn();
  # A count an earlier process of this number left would pass for this process.
  unlink $turn;
  $ENV{HEAVY_CHECK_HELD} = 1;
  $ENV{HEAVY_CHECK_HOLDER} = $$;
  my $turns = 0;
  my $asked = 0;
  # Blocked around fork, so a TERM or HUP always finds $pid set and goes on to the command.
  my $signals = POSIX::SigSet->new(SIGTERM, SIGHUP);
  my $mask = POSIX::SigSet->new;
  sigprocmask(SIG_BLOCK, $signals, $mask);
  $SIG{$_} = sub { kill $_[0], $pid } for qw(TERM HUP);
  $SIG{USR1} = sub { $asked = 1 };
  # The command shares the terminal, so a Ctrl-C reaches it already: a second one would cut its own cancel short.
  $SIG{INT} = "IGNORE";
  $pid = fork() // die "heavy-check: cannot fork: $!\n";
  if ($pid == 0) {
    $SIG{$_} = "DEFAULT" for qw(TERM HUP INT USR1);
    sigprocmask(SIG_SETMASK, $mask);
    exec { $ARGV[0] } @ARGV;
    print STDERR "heavy-check: cannot run $ARGV[0]: $!\n";
    exit 127;
  }
  sigprocmask(SIG_SETMASK, $mask);
  until (waitpid($pid, WNOHANG) == $pid) {
    if ($asked) {
      $asked = 0;
      my @line = waiting();
      # The first check of this checkout in line, which waits for the command to end: only the ones ahead of it go.
      my ($own) = grep { read_line("$queue/$_") eq $checkout } @line;
      if (@line && (!defined $own || $own ne $line[0])) {
        print STDERR "heavy-check: letting the heavy checks that wait go first\n";
        release();
        # A new ticket, behind everyone who waits now, or one right before that check: a half more than the number
        # before its own, which sorts after every ticket of that number.
        $ticket = defined $own ? sprintf("%017d.5-%d", ($own =~ /^(\d+)/)[0] - 1, $$) : next_ticket();
        take_turn();
        # One asked again meanwhile was answered by this same turn: the command runs again from here on.
        $asked = 0;
      }
      $turns++;
      if (open(my $out, ">", $turn)) { print $out "$turns\n"; close($out) }
    }
    sleep 0.1;
  }
  finish($?);
' "$LOCK" "$CHECKOUT" "$@"

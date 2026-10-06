#!/bin/sh
# Codear sandbox runner. It runs INSIDE the sandbox container, as root, and does everything that must happen next
# to the untrusted code: compile it, run it once per test with hard limits, and write what happened to files.
# The engine reads those files afterwards. It never parses the program's own output stream, so a program cannot
# fake a result by printing something that looks like one.
#
# Environment (set by the engine):
#   LANGUAGE            python | cpp
#   NUM_TESTS           number of input_<i>.txt files in the current directory (i from 0)
#   TIME_LIMIT_MS       CPU-time limit per test (checked by the engine, and between tests by this script)
#   CPU_LIMIT_S         the same limit rounded up to whole seconds, enforced by the kernel so a busy loop dies at once
#   WALL_LIMIT_S        wall-clock limit per test (a backstop for programs that sleep or block)
#   MEMORY_LIMIT_BYTES  address-space limit per test
#   OUTPUT_LIMIT_BYTES  largest file the program may write (its stdout and stderr each count)
#   COMPILE_TIMEOUT_S   time allowed for compiling
#   RUN_AS              numeric uid:gid to run untrusted code as (empty: keep root; only for local tests)
#   TEST_SEPARATOR      line printed after each test, for the live progress bar only
#
# Files written to ./results:
#   compile.status  exit code of the compile step (0 = fine, 124 = timed out)
#   compile.txt     compiler output
#   code_<i>        exit code of test i as the shell saw it (124 = wall limit, 137 = killed, 152 = CPU limit,
#                   153 = output limit, 128+n = signal n)
#   meta_<i>        "<wall s> <user s> <sys s> <peak KB>" from GNU time
#   out_<i>         stdout of test i
#   err_<i>         stderr of test i
#   done            written last: the runner finished
#
# The run stops at the first test that fails at run time (crash, time, memory or output limit); the engine decides
# wrong answers afterwards, because only it knows the expected output.
set -u

WORKDIR=$(pwd)
RESULTS="$WORKDIR/results"
BUILD=/tmp/build
SEPARATOR=${TEST_SEPARATOR:-"==="}

mkdir -p "$RESULTS" "$BUILD"
chmod 700 "$RESULTS"   # the program must not be able to read or list what the judge records

# Untrusted code runs as an unprivileged user with no capabilities. The inputs stay readable by root only, so the
# program can read its own stdin but cannot open the other tests' input files.
if [ -n "${RUN_AS:-}" ]; then
    UID_GID=${RUN_AS%%:*}
    DROP="setpriv --reuid=${RUN_AS%%:*} --regid=${RUN_AS##*:} --clear-groups --no-new-privs"
    chown "$UID_GID" "$BUILD"
else
    DROP=""
fi
chmod 711 "$WORKDIR"
chmod 600 input_*.txt 2>/dev/null

compile() {
    case "$LANGUAGE" in
        python)
            cp code.py "$BUILD/code.py"
            COMPILE="python3 -B -m py_compile $BUILD/code.py"
            RUN="python3 -B $BUILD/code.py"
            ;;
        cpp)
            cp code.cpp "$BUILD/code.cpp"
            COMPILE="g++ $BUILD/code.cpp -o $BUILD/a.out -O2"
            RUN="$BUILD/a.out"
            ;;
        *)
            echo "unsupported language: $LANGUAGE" > "$RESULTS/compile.txt"
            echo 2 > "$RESULTS/compile.status"
            return 1
            ;;
    esac
    [ -n "${RUN_AS:-}" ] && chown -R "${RUN_AS%%:*}" "$BUILD"

    # the compiler runs as the unprivileged user too, so "#include" cannot be used to read the other inputs
    timeout -k 2 "$COMPILE_TIMEOUT_S" $DROP prlimit --fsize=131072 $COMPILE > "$RESULTS/compile.txt" 2>&1
    code=$?
    echo "$code" > "$RESULTS/compile.status"
    return "$code"
}

# True when the CPU time recorded for test $1 is over the limit.
cpu_over_limit() {
    awk -v limit="$TIME_LIMIT_MS" 'NR == 1 { exit !(($2 + $3) * 1000 > limit) }' "$RESULTS/meta_$1" 2>/dev/null
}

run_one() {
    i=$1
    # `time` stays root so it can write the metrics file. timeout runs as the same unprivileged user as the program
    # it supervises: root without capabilities is not allowed to signal another user's process.
    # timeout asks politely first (exit 124 = out of wall time) and only then forces the kill after 2 more seconds.
    /usr/bin/time -f "%e %U %S %M" -o "$RESULTS/meta_$i" \
        $DROP timeout -k 2 "$WALL_LIMIT_S" \
        prlimit --cpu="$CPU_LIMIT_S" --fsize="$OUTPUT_LIMIT_BYTES" --as="$MEMORY_LIMIT_BYTES" --core=0 --stack=268435456 \
        $RUN < "input_$i.txt" > "$RESULTS/out_$i" 2> "$RESULTS/err_$i"
    code=$?
    printf '%s' "$code" > "$RESULTS/code_$i"
    return "$code"
}

if compile; then
    i=0
    while [ "$i" -lt "$NUM_TESTS" ]; do
        echo "[TEST-START-$i]"
        run_one "$i"
        failed=$?
        echo "$SEPARATOR"
        [ "$failed" -ne 0 ] && break
        cpu_over_limit "$i" && break
        i=$((i + 1))
    done
fi

touch "$RESULTS/done"
echo "[RUNNER-DONE]"

#!/bin/sh
# Boots release jars on a plain dedicated Forge server and checks that the server starts.
#
# Usage: tools/release/boot-server.sh <server dir> <log file> <jar>...
#
# <server dir> holds a Forge 1.12.2 server (forge-*.jar, minecraft_server.1.12.2.jar, libraries/, eula.txt). Its
# mods/ folder is emptied and given exactly the jars named, and its world is deleted. The server runs with
# -Dtrickytrialsbackport.loadPatched=true, which makes the mod load and verify every class its coremod patches once the
# server has started (PatchCheck.targets: every target of P1 to P25 the side loads, and P25's Fluidlogged API class when that mod is in mods/), so a patch that only
# fails the JVM's verifier on Mojang's real ProGuard-framed bytes shows here. The dev runServer cannot show it: it runs Minecraft recompiled by
# javac, with different stack-map frames.
#
# JVM_ARGS adds JVM arguments, for example the other family mods' -D<modid>.loadPatched=true when their jars are
# booted together.
#
# Passes (exit 0) when the log has "Done (", one "PATCHCHECK ok" line per mod asked (this one and each
# -D<modid>.loadPatched=true in JVM_ARGS), and no VerifyError, crash report or
# "PATCHCHECK FAIL". Run it on a release build, after `./gradlew build`.
set -u
if [ $# -lt 3 ]; then
    echo "usage: $0 <server dir> <log file> <jar>..." >&2
    exit 2
fi
dir=$1
log=$2
shift 2
case "$log" in
    /*|?:*) ;;
    *) log="$(pwd)/$log" ;;
esac
for jar in "$@"; do
    [ -f "$jar" ] || { echo "no such jar: $jar" >&2; exit 2; }
done
forge=$(ls "$dir"/forge-1.12.2-*.jar 2>/dev/null | head -n 1)
[ -n "$forge" ] || { echo "no forge-1.12.2-*.jar in $dir" >&2; exit 2; }
java=${JAVA:-java}
[ -x /e/MincraftModding/JDK8/bin/java.exe ] && java=${JAVA:-/e/MincraftModding/JDK8/bin/java}

rm -rf "$dir/mods" "$dir/world" "$dir/crash-reports"
mkdir -p "$dir/mods"
for jar in "$@"; do
    cp "$jar" "$dir/mods/"
done
: > "$log"
(
    n=0
    while ! grep -q "Done (\|PATCHCHECK \|Crash report\|crash report\|VerifyError" "$log" 2>/dev/null && [ $n -lt 150 ]; do
        sleep 2
        n=$((n + 1))
    done
    sleep 6
    echo stop
    sleep 20
) | (cd "$dir" && "$java" -Xmx3G -Dfml.queryResult=confirm ${JVM_ARGS:-} -Dtrickytrialsbackport.loadPatched=true -jar "$(basename "$forge")" nogui) > "$log" 2>&1
echo "exit $?" >> "$log"

fail=0
grep -q "Done (" "$log" || { echo "FAIL: the server did not finish starting"; fail=1; }
want=$((1 + $(printf '%s\n' ${JVM_ARGS:-} | grep -c 'loadPatched=true')))
got=$(grep -c "PATCHCHECK ok" "$log")
[ "$got" -ge "$want" ] || { echo "FAIL: $got 'PATCHCHECK ok' lines, $want mods asked"; fail=1; }
if grep -q "VerifyError\|PATCHCHECK FAIL\|Crash report\|crash report" "$log"; then
    echo "FAIL:"
    grep -m 5 "VerifyError\|PATCHCHECK FAIL\|Crash report\|crash report" "$log"
    fail=1
fi
[ $fail -eq 0 ] && echo "PASS:" && grep "PATCHCHECK ok" "$log" | sed 's/^.*\] \[\(.*\)\]: PATCHCHECK/  \1: PATCHCHECK/'
exit $fail

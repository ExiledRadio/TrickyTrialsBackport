#!/bin/sh
# The superflat self-test (FlatSelfTest) in a fresh superflat world: points eclipse/server.properties at level-type
# FLAT with a preset, on a world of its own (eclipse/flatworld, made new each run), runs the flat group, then puts
# level-type, generator-settings and level-name back (the rest of the suite runs in a Default world).
#   sh tools/flat/selftest.sh [default|stone|decoration]
# default:    3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village                        (1.12's Classic Flat)
# stone:      3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village            (ores could go)
# decoration: 3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village,decoration
set -e
root="$(cd "$(dirname "$0")/../.." && pwd)"
props="$root/eclipse/server.properties"
case "${1:-default}" in
    default) preset='3;minecraft:bedrock,2*minecraft:dirt,minecraft:grass;1;village' ;;
    stone) preset='3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village' ;;
    decoration) preset='3;minecraft:bedrock,59*minecraft:stone,3*minecraft:dirt,minecraft:grass;1;village,decoration' ;;
    *) echo "unknown preset $1" >&2; exit 2 ;;
esac
old_level="$(sed -n 's/^level-name=//p' "$props")"
# A literal path: the flat world is always eclipse/flatworld.
(cd "$root/eclipse" && rm -rf flatworld)
sed -i "s/^level-type=.*/level-type=FLAT/; s|^generator-settings=.*|generator-settings=$preset|; s/^level-name=.*/level-name=flatworld/" "$props"
status=0
(cd "$root" && ./gradlew runServer -Pselftest=flat -PdeployDir= -PcoreDeployDir=) || status=$?
sed -i "s/^level-type=.*/level-type=DEFAULT/; s|^generator-settings=.*|generator-settings=|; s/^level-name=.*/level-name=${old_level:-selftest}/" "$props"
grep -h "SELFTEST DONE" "$root/eclipse/logs/latest.log" 2>/dev/null | tail -1 || true
exit $status

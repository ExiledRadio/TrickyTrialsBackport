# Briefs for the streams

While twelve agents share the machine, this repo's own session builds every stream itself, one at a
time. These briefs are for later, if the user raises the limit and streams go to separate sessions.
`docs/PLAN.md` has the full content list per stream and the shared-file rules; finished streams'
`docs/streams/<stream>.md` show what "done" looks like.

## Common to every brief

- **Where**: a worktree of `E:\MincraftModding\Workspaces\TrickyTrialsBackport`, made with
  `git worktree add ..\TrickyTrialsBackport-<stream> -b tr-<stream> main`. Copy `deps/` and `eclipse/eula.txt`
  and `eclipse/server.properties` from the main worktree; set the port from the plan's rule. Commit on the
  branch; never push; the integrator merges.
- **Read first**: `docs/PLAN.md`, `docs/CORE_REQUESTS.md`, a finished `docs/streams/*.md`, and the memory
  notes on FG2, reobf, `@Config`, sided classes, lang `pack.mcmeta`, dev-env coremods, VintageFix and
  1.12 worldgen in `C:\Users\Radio\.claude\projects\E--MincraftModding\memory\`.
- **Behavior**: current Java per minecraft.wiki; assets and names from the 1.21.11 jar through
  `tools/assets.py` and a `tools/<stream>/generate.py`.
- **Code**: your package only; register through `common/Content`; owned blocks through `common/Owners`;
  your entity and packet ids from the plan's table. A change to `common/` is the integrator's.
- **Test**: `selftest/<Stream>SelfTest.java` (one line in `SelfTestRunner.TESTS`),
  `./gradlew runServer -Pselftest=<stream>`, `python -I tools/check_assets.py` with 0 errors, and
  `./gradlew build` last; check free RAM (8 GB) before each. Entities: spawn, tick, AI, drops, spawning
  rules, NBT round trip. Owned copies: test with and without the owner's jar in `eclipse/mods`.
- **Done**: the jar's classes carry `func_` names, `docs/streams/<stream>.md` (changelog section, how it
  works, tested, not tested), a commit on the branch. No mixins, no coremod.
- **Rules**: never mention other backport mods in user-facing text; American spelling; every
  user-facing string a lang key; never send keystrokes or clicks to the user's screen.

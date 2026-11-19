# Stream 18: the locator bar (1.21.6)

Package `locator` (client parts in `locator/client`). Config `locatorBar` (`enabled`, on by default; decision 6).
No entities. Packet id 90 (`WaypointMessage`); 91-94 unused. Branch `claude/tr-locator-8e7fef`, dev-server port 25918.

Behavior is 1.21.11's, which is 1.21.6's apart from the game rule's later snake-case name (`ServerWaypointManager`,
`WaypointTransmitter` and its three connections, `LivingEntity`'s and `ServerPlayer`'s waypoint methods, `Waypoint`,
`TrackedWaypoint`, `ClientWaypointManager`, `LocatorBarRenderer`, `ContextualBarRenderer`, `Gui.nextContextualInfoState`,
`WaypointStyle`, `GameRenderer.projectHorizonToScreen` / `projectPointToScreen`, `ARGB.setBrightness`), decompiled from
the 1.21.11 client jar. Sprites from the same jar.

## Changelog section

*   **Locator bar**: while other players are nearby, a bar in place of the experience bar shows where they are. Each
    player within 60 degrees either side of your view is a colored dot (their team's color, or a color of their own),
    fading as they get farther away; a blinking arrow above or below the dot means they are above or below your view.
    Your experience level still shows above the bar, and the experience bar comes back for five seconds whenever your
    experience changes. Riding a horse, the jump bar comes back while you charge a jump.
*   **What hides you**: sneaking, being invisible, or wearing a carved pumpkin or a mob head keeps you off other
    players' bars. Spectators are never shown, but see everyone.
*   **Game rule `locatorBar`** (on): off, nobody's bar shows anyone.
*   **Attributes**: players have a *Waypoint Transmit Range* and a *Waypoint Receive Range* (60,000,000 blocks); you
    show on another player's bar only within the smaller of your transmit range and their receive range.

## How it works

*   **Attributes** (`Waypoints.TRANSMIT_RANGE`, `RECEIVE_RANGE`): 1.12.2 `RangedAttribute`s named as vanilla
    (`waypoint_transmit_range`, `waypoint_receive_range`, so the tooltip keys are vanilla's), 0 to 6e7, default 0,
    added to every player as it joins a world with base 6e7 (`Player.createAttributes`). Only players have them, as in
    vanilla, where other mobs' 0 means they never transmit.
*   **Hiding** (`Waypoints.updateModifiers`, every tick before the ranges are read): vanilla's three -1
    multiply-total modifiers (1.12.2's operation 2), transient:
    `waypoint_transmit_range_crouch` while crouching (1.12.2's sneak, except flying, gliding or sleeping, where 1.21 has
    no crouching pose); `waypoint_transmit_range_hide` while the head slot holds a carved pumpkin (1.12.2's pumpkin) or
    a mob head (the skull item, all six; another mod's `carved_pumpkin` or `piglin_head`), vanilla's
    `Waypoint.addHideAttribute` items; and invisibility's, registered on the effect itself
    (`registerPotionAttributeModifier`, so the potion's tooltip lists it as 1.21's does), with a per-tick fallback for
    an effect loaded from a save before the attribute existed on the player.
*   **Who is sent to whom** (`Waypoints.ignores`, `doesSourceIgnoreReceiver`): never yourself; a spectator receiver
    sees everyone at any range; a spectator source, a source carrying the receiver, or one at least
    `min(transmit, receive)` away is not shown. Not on a source's first tick.
*   **Connections** (`WaypointManager`, one per world, `ServerWaypointManager`): per receiver and source, of one of
    vanilla's three kinds, chosen by `makeWaypointConnectionWith`: beyond 332 blocks an **angle** (radians of the
    receiver-to-source direction), else within the receiver's view distance a **block** position, else a **chunk**.
    Vanilla's update rates: a block connection sends when the source's block changes, a chunk connection when its chunk
    changes, an angle when it turns by more than half a degree; nothing is sent otherwise. Vanilla's break rules remake a
    connection (a new track): a block one moved more than one block in a tick or now ignored; a chunk one moved more
    than one chunk, ignored, or come into view; an angle one ignored, in view, or no longer really far. A team change
    remakes it too (`ServerScoreboard.updatePlayerWaypoint`). Vanilla updates on every move and every
    `ServerChunkCache.move`; here the world tick does the same pass once a tick. Players who leave the world (log out,
    change dimension, respawn) have their connections removed first, so a respawned player's old waypoint is gone
    before the new one is sent. The chunk view is 1.12.2's square of `view-distance` chunks around the receiver's chunk
    (1.21's is a circle).
*   **Color** (`Waypoint.Icon.cloneAndAssignStyle`): the source's team color (black shown as `#303030`); without one
    the client colors the dot from the UUID (`ARGB.setBrightness(uuid.hashCode(), 0.9)`).
*   **The game rule** (`LocatorBar`): added to every world that lacks it, `true`. Turned off, every connection breaks
    (`breakAllConnections`) and nothing more is sent; on again, everyone is tracked again
    (`MinecraftServer.onGameRuleChanged`).
*   **Packet 90** (`WaypointMessage`, `ClientboundTrackedWaypointPacket`): track, untrack or update; UUID, optional RGB
    color, kind (empty, block, chunk, angle) and its varints or float.
*   **Client** (`ClientWaypoints`, `ClientWaypointManager`): the waypoints by UUID, cleared on connect and disconnect,
    applied on the client thread. `LocatorBarHud` cancels Forge's `EXPERIENCE` element (and `JUMPBAR` while riding a
    horse and not charging a jump) and draws, as `LocatorBarRenderer` and `Gui.nextContextualInfoState` do: the 182 x 5
    background (a 12 x 5 nine-slice sprite, 5-pixel ends, middle tiled), the level text as 1.12.2 draws it
    (survival or adventure, level above 0), then the dots farthest first: 9 x 9 at
    `ceil((width - 9) / 2) + floor(yaw * 173 / 120)`, 2 above the bar, within -60 (exclusive) to 60 degrees, the
    default style's sprite by distance (`default_0` under 128 blocks, `default_3` from 332, 1 or 2 between), tinted.
    The arrow (`locator_bar_arrow_up`/`_down`, two animated frames, 10 and 4 ticks) sits 6 above or below: for a block
    waypoint from the point projected to the screen (vanilla's projection, including its handling behind the camera);
    for chunk and angle waypoints from the horizon (`tan(pitch) / tan(fov / 2)` beyond 1). A block waypoint aims at the
    player's eyes while that player is loaded within 3 blocks of the sent position, else at the block's center. The
    field of view is 1.12.2's `getFOVModifier` (by reflection). The angles, sprite choice, positions and arrows live in
    `BarMath`, shared with the self-test. The experience bar's five seconds: a change to the level or progress, except
    in a player's first 20 ticks (vanilla skips the first experience packet after joining).
*   **No patches.**

## Left out

*   **`/waypoint`** (list, modify color and style): the plan's section 18 does not list it, and 1.12.2 has no
    `/attribute` either, so the ranges can only be changed by another mod or item attribute modifiers. Without the
    command no waypoint ever has a style other than the default or a color other than its team's, so the packet carries
    no style and the `bowtie` style is not shipped.
*   **Mobs as waypoints**: vanilla lets any living entity transmit once its range is raised by command; without the
    command only players ever transmit, so only players have the attributes.
*   With waypoints shown, riding a horse and experience just changed, 1.21 shows the experience bar; 1.12.2 always
    draws the jump bar while riding, so the locator bar or the jump bar shows then.
*   The camera position is the view entity's eyes, also in third person (1.21 uses the camera's own position).

## Tested headlessly

`./gradlew runServer -Pselftest=all --offline` from a fresh world (Default, seed 1013 as the other streams, port 25918),
then `python -I tools/check_assets.py`: **`SELFTEST DONE pass=3279 fail=0`** (locator alone: **142**), **148 blocks,
2475 states, 173 item models, 778 sound files, 0 errors**. Release jar built last with
`./gradlew build --rerun-tasks --offline -PdeployDir= -PcoreDeployDir=`: reobfuscated (`func_` names), 0 `/selftest/`
entries. (Three earlier fresh worlds with random seeds each failed one to three other streams' terrain- or
timing-dependent checks, different ones each time: decor's random paintings, flora's fallen logs, chambers, breeze,
effects' cobwebs, pale garden trees; never the locator's.)

*   The game rule: in the overworld, `true`, read as on; the setting on by default.
*   Attributes: both 6e7 on a player, added once, the attribute's own default 0, clamped to 0..6e7, vanilla's name.
*   Hiding: sneaking hides (and still receives), standing shows again, sneaking while flying does not hide; the pumpkin
    and all six skulls hide, an iron helmet, a jack o'lantern and nothing do not; invisibility hides through the
    per-tick check and through the effect's own modifier (amplifier II), and stops hiding when it ends; glowing does not.
*   Ranges: shown at 50 blocks; not at exactly the receive range, shown just inside it; not beyond the transmit range;
    a spectator receiver past every range; a spectator source never; back in survival again.
*   Kinds: block in view, chunk out of view, angle past 332 blocks; none to oneself or on a first tick; the angle's
    direction (due south pi, due east pi/2) and that it is ahead of a receiver facing the source.
*   The manager with a packet recorder: two players each get the other's track and not their own; nothing when nobody
    moves; one update per block moved, with the new position; sneaking removes and standing tracks again; a team
    remakes the waypoint in the team's color (black dark gray, no color none); the rule off removes every waypoint and
    sends nothing more, on tracks again; a block connection is kept while its source stays put and becomes a chunk one
    when it moves out of view; no update inside a chunk, one into the next; an angle past 332 blocks, nothing for less
    than half a degree, an update past it; past the receive range removed while the other way stays; a player who
    leaves loses every connection and is told to remove the others; a third player joins.
*   The packet: each kind's round trip (operation, id, color or none, kind, position, chunk, angle), read to the end.
*   The bar's numbers (`BarMath`): the sprite by distance at 0, 127.9, 128, 229, 230, 332 and infinity; visible within
    -60 (exclusive) to 60 and not NaN; angles ahead, left, right, facing east, wrapped yaw; dot x at 0, 60 and -60
    degrees; arrows level, above, below, behind (vanilla's flip near the horizon), looking down; the horizon arrows;
    the UUID color's 0.9 brightness and opacity; team colors.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn (no client self-test in this repo): the bar, the dots, their colors and fading, the arrows and
    their animation, the experience bar's five seconds, the jump bar. The numbers they use are tested through
    `BarMath`.
*   Packets over a real connection (the encoding is tested by a round trip; the recorder stands in for the network).
*   Two real clients: no multiplayer play test was run.

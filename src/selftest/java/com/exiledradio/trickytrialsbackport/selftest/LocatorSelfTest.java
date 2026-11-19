package com.exiledradio.trickytrialsbackport.selftest;

import com.exiledradio.trickytrialsbackport.locator.BarMath;
import com.exiledradio.trickytrialsbackport.locator.LocatorBar;
import com.exiledradio.trickytrialsbackport.locator.WaypointManager;
import com.exiledradio.trickytrialsbackport.locator.WaypointMessage;
import com.exiledradio.trickytrialsbackport.locator.Waypoints;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Stream 18, the locator bar: which players are sent to whom, what hides a player, the ranges, the game rule, the
 * packet and the bar's drawing numbers. Real server players outside the world, driven through a manager whose
 * packets are recorded.
 */
public final class LocatorSelfTest implements SelfTest {

    private static final int VIEW = 10;

    @Override
    public String name() {
        return "locator";
    }

    private static final class Sent {
        final EntityPlayerMP to;
        final WaypointMessage m;

        Sent(EntityPlayerMP to, WaypointMessage m) {
            this.to = to;
            this.m = m;
        }
    }

    private final List<Sent> sent = new ArrayList<>();

    private EntityPlayerMP player(Check c, String name, double x, double z) {
        EntityPlayerMP p = Players.real(c.world, "loc" + name + System.nanoTime());
        Waypoints.addAttributes(p);
        p.ticksExisted = 1;
        move(p, x, z);
        return p;
    }

    private static void move(EntityPlayerMP p, double x, double z) {
        p.setPosition(x, 100.0D, z);
        p.chunkCoordX = (int) Math.floor(x) >> 4;
        p.chunkCoordZ = (int) Math.floor(z) >> 4;
    }

    @Override
    public void run(Check c) {
        rule(c);
        attributes(c);
        hiding(c);
        ranges(c);
        kinds(c);
        manager(c);
        packet(c);
        bar(c);
    }

    private void rule(Check c) {
        c.ok(c.world.getGameRules().hasRule(LocatorBar.RULE), "the overworld has the locatorBar rule");
        c.eq("true", c.world.getGameRules().getString(LocatorBar.RULE), "locatorBar defaults to true");
        c.ok(LocatorBar.rule(c.world), "the rule reads on");
        c.ok(LocatorBar.on(), "the setting is on by default");
    }

    private void attributes(Check c) {
        EntityPlayerMP p = player(c, "attr", 0, 0);
        c.eq(6.0E7D, Waypoints.value(p, Waypoints.TRANSMIT_RANGE), "player transmit range");
        c.eq(6.0E7D, Waypoints.value(p, Waypoints.RECEIVE_RANGE), "player receive range");
        c.ok(Waypoints.transmits(p) && Waypoints.receives(p), "a player transmits and receives");
        Waypoints.addAttributes(p);
        c.eq(6.0E7D, Waypoints.value(p, Waypoints.TRANSMIT_RANGE), "adding twice keeps one attribute");
        c.eq(0.0D, Waypoints.TRANSMIT_RANGE.getDefaultValue(), "the attribute's own default is 0 (only players have range)");
        c.eq(6.0E7D, Waypoints.TRANSMIT_RANGE.clampValue(1.0E9D), "range clamps to 6e7");
        c.eq(0.0D, Waypoints.RECEIVE_RANGE.clampValue(-5.0D), "range clamps to 0");
        c.eq("waypoint_transmit_range", Waypoints.TRANSMIT_RANGE.getName(), "attribute name (vanilla's tooltip key)");
    }

    private void hiding(Check c) {
        EntityPlayerMP p = player(c, "hide", 0, 0);
        p.setSneaking(true);
        Waypoints.updateModifiers(p);
        c.ok(!Waypoints.transmits(p), "sneaking hides");
        c.ok(Waypoints.receives(p), "sneaking still receives");
        p.setSneaking(false);
        Waypoints.updateModifiers(p);
        c.ok(Waypoints.transmits(p), "standing up shows again");

        p.capabilities.isFlying = true;
        p.setSneaking(true);
        Waypoints.updateModifiers(p);
        c.ok(Waypoints.transmits(p), "sneaking while flying is not crouching");
        p.capabilities.isFlying = false;
        p.setSneaking(false);
        Waypoints.updateModifiers(p);

        List<ItemStack> hides = Arrays.asList(new ItemStack(Blocks.PUMPKIN), new ItemStack(Items.SKULL, 1, 0), new ItemStack(Items.SKULL, 1, 1),
                new ItemStack(Items.SKULL, 1, 2), new ItemStack(Items.SKULL, 1, 3), new ItemStack(Items.SKULL, 1, 4), new ItemStack(Items.SKULL, 1, 5));
        for (ItemStack head : hides) {
            p.setItemStackToSlot(EntityEquipmentSlot.HEAD, head.copy());
            Waypoints.updateModifiers(p);
            c.ok(!Waypoints.transmits(p), "wearing " + head.getItem().getRegistryName() + "@" + head.getMetadata() + " hides");
        }
        for (ItemStack head : Arrays.asList(new ItemStack(Items.IRON_HELMET), new ItemStack(Blocks.LIT_PUMPKIN), ItemStack.EMPTY)) {
            p.setItemStackToSlot(EntityEquipmentSlot.HEAD, head.copy());
            Waypoints.updateModifiers(p);
            c.ok(Waypoints.transmits(p), "wearing " + head + " does not hide");
        }
        c.ok(!Waypoints.hidesWearer(ItemStack.EMPTY), "an empty head slot does not hide");

        p.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 200));
        Waypoints.updateModifiers(p);
        c.ok(!Waypoints.transmits(p), "invisibility hides");
        p.removePotionEffect(MobEffects.INVISIBILITY);
        Waypoints.updateModifiers(p);
        c.ok(Waypoints.transmits(p), "invisibility gone shows again");
        // The effect's own modifier, without the per-tick fallback.
        p.addPotionEffect(new PotionEffect(MobEffects.INVISIBILITY, 200, 1));
        c.ok(!Waypoints.transmits(p), "the invisibility effect's modifier hides by itself");
        p.removePotionEffect(MobEffects.INVISIBILITY);
        c.ok(Waypoints.transmits(p), "and is removed with the effect");
        p.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 200));
        Waypoints.updateModifiers(p);
        c.ok(Waypoints.transmits(p), "glowing does not hide");
        p.removePotionEffect(MobEffects.GLOWING);
    }

    private void ranges(Check c) {
        EntityPlayerMP a = player(c, "ra", 0, 0);
        EntityPlayerMP b = player(c, "rb", 0, 50);
        c.ok(!Waypoints.ignores(a, b), "50 blocks away is shown");
        b.getAttributeMap().getAttributeInstanceByName("waypoint_receive_range").setBaseValue(50.0D);
        c.ok(Waypoints.ignores(a, b), "at the receive range exactly is not shown (distance >= range)");
        b.getAttributeMap().getAttributeInstanceByName("waypoint_receive_range").setBaseValue(51.0D);
        c.ok(!Waypoints.ignores(a, b), "inside the receive range is shown");
        a.getAttributeMap().getAttributeInstanceByName("waypoint_transmit_range").setBaseValue(20.0D);
        c.ok(Waypoints.ignores(a, b), "beyond the source's transmit range is not shown");
        b.setGameType(GameType.SPECTATOR);
        c.ok(!Waypoints.ignores(a, b), "a spectator receiver sees past every range");
        b.setGameType(GameType.SURVIVAL);
        a.getAttributeMap().getAttributeInstanceByName("waypoint_transmit_range").setBaseValue(Waypoints.MAX_RANGE);
        a.setGameType(GameType.SPECTATOR);
        c.ok(Waypoints.ignores(a, b), "a spectator source is not shown");
        a.setGameType(GameType.SURVIVAL);
        c.ok(!Waypoints.ignores(a, b), "back in survival it is");
    }

    private void kinds(Check c) {
        EntityPlayerMP r = player(c, "kr", 0, 0);
        EntityPlayerMP near = player(c, "kn", 40, 0);
        c.eq((int) Waypoints.BLOCK, Waypoints.connectionType(near, r, VIEW), "a source in a loaded chunk is sent by block");
        c.eq((int) Waypoints.CHUNK, Waypoints.connectionType(near, r, 1), "out of the view distance by chunk");
        EntityPlayerMP far = player(c, "kf", 400, 0);
        c.eq((int) Waypoints.AZIMUTH, Waypoints.connectionType(far, r, 100), "beyond 332 blocks by angle");
        c.eq(-1, Waypoints.connectionType(r, r, VIEW), "no connection to oneself");
        near.ticksExisted = 0;
        c.eq(-1, Waypoints.connectionType(near, r, VIEW), "not on a source's first tick");
        // Azimuth: the receiver minus the source, turned 90 degrees clockwise, as atan2(z, x).
        EntityPlayerMP south = player(c, "ks", 0, 500);
        c.ok(Math.abs(Math.abs(Waypoints.azimuth(r, south)) - (float) Math.PI) < 1.0E-4F, "azimuth of a receiver due south is pi");
        c.ok(Math.abs(BarMath.yawToAzimuth(180.0F, Waypoints.azimuth(r, south))) < 1.0E-3D, "which is ahead of it facing north");
        EntityPlayerMP east = player(c, "ke", 500, 0);
        c.ok(Math.abs(Waypoints.azimuth(r, east) - (float) Math.PI / 2) < 1.0E-4F, "azimuth of a receiver due east is pi/2");
        c.ok(Math.abs(BarMath.yawToAzimuth(90.0F, Waypoints.azimuth(r, east))) < 1.0E-3D, "which is ahead of it facing west");
    }

    private void manager(Check c) {
        WaypointManager m = new WaypointManager((to, msg) -> this.sent.add(new Sent(to, msg)));
        EntityPlayerMP a = player(c, "ma", 0, 0);
        EntityPlayerMP b = player(c, "mb", 20, 0);
        List<EntityPlayerMP> players = new ArrayList<>(Arrays.asList(a, b));
        this.sent.clear();
        m.tick(players, true, VIEW);
        c.eq(2, this.sent.size(), "two players: each is sent the other");
        c.ok(has(b, WaypointMessage.TRACK, a.getUniqueID()) && has(a, WaypointMessage.TRACK, b.getUniqueID()), "both tracks");
        c.eq((int) Waypoints.BLOCK, m.connection(a, b), "near: a block connection");
        c.ok(!has(a, WaypointMessage.TRACK, a.getUniqueID()), "nobody is sent themselves");

        this.sent.clear();
        m.tick(players, true, VIEW);
        c.eq(0, this.sent.size(), "nobody moved: nothing sent");

        move(a, 1.5D, 0.5D);
        this.sent.clear();
        m.tick(players, true, VIEW);
        c.eq(1, this.sent.size(), "a moved one block: one update");
        c.ok(has(b, WaypointMessage.UPDATE, a.getUniqueID()), "the update goes to b");
        c.eq(1, this.sent.isEmpty() ? -1 : this.sent.get(0).m.x, "the update has the new x");

        move(a, 0.3D, 0.5D);
        this.sent.clear();
        m.tick(players, true, VIEW);
        c.eq(1, this.sent.size(), "moving back within the block one update");

        a.setSneaking(true);
        this.sent.clear();
        m.tick(players, true, VIEW);
        c.ok(has(b, WaypointMessage.UNTRACK, a.getUniqueID()), "a sneaks: b is told to remove a");
        c.eq(-1, m.connection(a, b), "and the connection is gone");
        c.eq(1, m.size(), "b is still shown to a");
        a.setSneaking(false);
        this.sent.clear();
        m.tick(players, true, VIEW);
        c.ok(has(b, WaypointMessage.TRACK, a.getUniqueID()), "a stands: tracked again");

        // Team colors.
        Scoreboard board = c.world.getScoreboard();
        String teamName = "loc" + (System.nanoTime() % 100000);
        ScorePlayerTeam team = board.createTeam(teamName);
        team.setColor(TextFormatting.RED);
        board.addPlayerToTeam(a.getName(), teamName);
        this.sent.clear();
        m.tick(players, true, VIEW);
        Sent track = find(b, WaypointMessage.TRACK, a.getUniqueID());
        c.ok(track != null, "joining a team remakes the connection");
        c.eq(0xFF5555, track == null ? null : track.m.color, "the waypoint has the team's color");
        team.setColor(TextFormatting.BLACK);
        c.eq(0x303030, Waypoints.teamColor(a), "a black team shows dark gray");
        team.setColor(TextFormatting.RESET);
        c.eq(-1, Waypoints.teamColor(a), "a team without a color gives none");
        board.removeTeam(team);
        c.eq(-1, Waypoints.teamColor(b), "no team, no color");

        // The game rule.
        this.sent.clear();
        m.tick(players, false, VIEW);
        c.ok(has(b, WaypointMessage.UNTRACK, a.getUniqueID()) && has(a, WaypointMessage.UNTRACK, b.getUniqueID()), "rule off: every waypoint removed");
        c.eq(0, m.size(), "rule off: no connections");
        this.sent.clear();
        m.tick(players, false, VIEW);
        c.eq(0, this.sent.size(), "rule off: nothing more is sent");
        m.tick(players, true, VIEW);
        c.eq(2, m.size(), "rule back on: both connections again");

        // Far away: by chunk, then by angle.
        move(a, 150.0D, 0.0D);
        m.tick(players, true, VIEW);
        c.eq((int) Waypoints.BLOCK, m.connection(a, b), "130 blocks with a view distance of 10 is still in a sent chunk");
        m.tick(players, true, 2);
        c.eq((int) Waypoints.BLOCK, m.connection(a, b), "a block connection that did not move is kept (vanilla)");
        move(a, 145.0D, 0.0D);
        m.tick(players, true, 2);
        c.eq((int) Waypoints.CHUNK, m.connection(a, b), "moved, out of the view distance: by chunk");
        move(a, 152.0D, 0.0D);
        this.sent.clear();
        m.tick(players, true, 2);
        c.eq(0, this.sent.size(), "a moved within its chunk: nothing sent");
        move(a, 165.0D, 0.0D);
        this.sent.clear();
        m.tick(players, true, 2);
        c.ok(has(b, WaypointMessage.UPDATE, a.getUniqueID()), "a crossed into the next chunk: an update");
        move(a, 1000.0D, 0.0D);
        this.sent.clear();
        m.tick(players, true, 2);
        c.eq((int) Waypoints.AZIMUTH, m.connection(a, b), "beyond 332 blocks: by angle");
        Sent az = find(b, WaypointMessage.TRACK, a.getUniqueID());
        c.ok(az != null && az.m.type == Waypoints.AZIMUTH, "an angle track is sent");
        move(a, 1000.0D, 1.0D);
        this.sent.clear();
        m.tick(players, true, 2);
        c.ok(!has(b, WaypointMessage.UPDATE, a.getUniqueID()), "turning less than half a degree sends nothing");
        move(a, 1000.0D, 100.0D);
        this.sent.clear();
        m.tick(players, true, 2);
        c.ok(has(b, WaypointMessage.UPDATE, a.getUniqueID()), "turning more sends an update");

        // Range: past b's receive range a is removed.
        b.getAttributeMap().getAttributeInstanceByName("waypoint_receive_range").setBaseValue(500.0D);
        this.sent.clear();
        m.tick(players, true, 2);
        c.ok(has(b, WaypointMessage.UNTRACK, a.getUniqueID()), "past the receive range: removed");
        c.eq(-1, m.connection(a, b), "past the receive range: no connection");
        c.eq((int) Waypoints.BLOCK, m.connection(b, a), "a still sees b, by the block connection made while near: only the source moving breaks one (vanilla)");

        // A player who leaves.
        players.remove(a);
        this.sent.clear();
        m.tick(players, true, 2);
        c.eq(0, m.size(), "a left: no connections");
        c.ok(has(a, WaypointMessage.UNTRACK, b.getUniqueID()), "a is told to remove b as it leaves");

        // A third player.
        EntityPlayerMP d = player(c, "md", 10, 10);
        players.add(d);
        b.getAttributeMap().getAttributeInstanceByName("waypoint_receive_range").setBaseValue(Waypoints.MAX_RANGE);
        this.sent.clear();
        m.tick(players, true, VIEW);
        c.eq(2, m.size(), "b and d see each other");
    }

    private boolean has(EntityPlayerMP to, byte op, UUID id) {
        return find(to, op, id) != null;
    }

    private Sent find(EntityPlayerMP to, byte op, UUID id) {
        for (Sent s : this.sent) {
            if (s.to == to && s.m.operation == op && s.m.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    private void packet(Check c) {
        UUID id = UUID.randomUUID();
        WaypointMessage[] all = new WaypointMessage[4];
        for (int i = 0; i < 4; i++) {
            WaypointMessage m = new WaypointMessage();
            m.operation = (byte) (i % 3);
            m.id = id;
            m.color = i == 1 ? 0xFF5555 : -1;
            m.type = (byte) i;
            m.x = -123456;
            m.y = 70;
            m.z = 98765;
            m.angle = 1.25F;
            all[i] = m;
        }
        for (WaypointMessage m : all) {
            ByteBuf buf = Unpooled.buffer();
            m.toBytes(buf);
            WaypointMessage back = new WaypointMessage();
            back.fromBytes(buf);
            String kind = "packet type " + m.type;
            c.eq(m.operation, back.operation, kind + " operation");
            c.eq(id, back.id, kind + " id");
            c.eq(m.color, back.color, kind + " color");
            c.eq(m.type, back.type, kind + " type");
            if (m.type == Waypoints.BLOCK) {
                c.ok(back.x == m.x && back.y == m.y && back.z == m.z, kind + " position");
            } else if (m.type == Waypoints.CHUNK) {
                c.ok(back.x == m.x && back.z == m.z, kind + " chunk");
            } else if (m.type == Waypoints.AZIMUTH) {
                c.eq(m.angle, back.angle, kind + " angle");
            }
            c.eq(0, buf.readableBytes(), kind + " read to the end");
        }
    }

    private void bar(Check c) {
        c.eq(0, BarMath.sprite(0.0F), "dot 0 near");
        c.eq(0, BarMath.sprite(127.9F), "dot 0 under 128");
        c.eq(1, BarMath.sprite(128.0F), "dot 1 from 128");
        c.eq(1, BarMath.sprite(229.0F), "dot 1 to 230");
        c.eq(2, BarMath.sprite(230.0F), "dot 2 from 230");
        c.eq(3, BarMath.sprite(332.0F), "dot 3 from 332");
        c.eq(3, BarMath.sprite(Float.POSITIVE_INFINITY), "dot 3 for an angle");
        c.ok(BarMath.visible(60.0D) && BarMath.visible(-59.9D) && BarMath.visible(0.0D), "visible within 60 degrees");
        c.ok(!BarMath.visible(-60.0D) && !BarMath.visible(60.1D) && !BarMath.visible(Double.NaN), "hidden beyond 60 degrees");
        c.eq(0.0D, BarMath.yawToPoint(0, 0, 0.0F, 0, 10), "straight ahead (south at yaw 0) is 0");
        c.eq(-90.0D, BarMath.yawToPoint(0, 0, 0.0F, 10, 0), "east while facing south is 90 to the left");
        c.eq(90.0D, BarMath.yawToPoint(0, 0, 0.0F, -10, 0), "west while facing south is 90 to the right");
        c.eq(0.0D, BarMath.yawToPoint(0, 0, -90.0F, 10, 0), "east while facing east is ahead");
        c.eq(0.0D, BarMath.yawToPoint(0, 0, 270.0F, 10, 0), "yaw is wrapped");
        c.eq(0.0D, BarMath.yawToAzimuth(0.0F, 0.0F), "an angle of 0 at yaw 0 is ahead");
        int center = (int) Math.ceil((320 - 9) / 2.0D);
        c.eq(center, BarMath.dotX(320, 0.0D), "a dot ahead is centered");
        c.eq(center + 86, BarMath.dotX(320, 60.0D), "a dot at 60 degrees is at the bar's right end");
        c.eq(center - 87, BarMath.dotX(320, -59.99D), "and the left end");
        c.eq(BarMath.NONE, BarMath.pitchToPoint(0, 0, 10, 0.0F, 0.0F, 70.0F), "level ahead: no arrow");
        c.eq(BarMath.UP, BarMath.pitchToPoint(0, 100, 10, 0.0F, 0.0F, 70.0F), "far above: up");
        c.eq(BarMath.DOWN, BarMath.pitchToPoint(0, -100, 10, 0.0F, 0.0F, 70.0F), "far below: down");
        // Behind the camera vanilla reads the projected y, which flips there: near the horizon the arrow is the
        // opposite way, far above or below it points the true way.
        c.eq(BarMath.DOWN, BarMath.pitchToPoint(0, 1, -10, 0.0F, 0.0F, 70.0F), "behind, a little above: down (vanilla)");
        c.eq(BarMath.UP, BarMath.pitchToPoint(0, -1, -10, 0.0F, 0.0F, 70.0F), "behind, a little below: up (vanilla)");
        c.eq(BarMath.UP, BarMath.pitchToPoint(0, 100, -10, 0.0F, 0.0F, 70.0F), "behind, far above: up");
        c.eq(BarMath.NONE, BarMath.pitchToPoint(0, -100, 10, 0.0F, 80.0F, 70.0F), "looking down at it: no arrow");
        c.eq(BarMath.NONE, BarMath.pitchFromHorizon(BarMath.horizon(0.0F, 70.0F)), "level look: no arrow for a chunk");
        c.eq(BarMath.UP, BarMath.pitchFromHorizon(BarMath.horizon(60.0F, 70.0F)), "looking down: the horizon is up");
        c.eq(BarMath.DOWN, BarMath.pitchFromHorizon(BarMath.horizon(-60.0F, 70.0F)), "looking up: the horizon is down");
        c.eq(BarMath.UP, BarMath.pitchFromHorizon(BarMath.horizon(90.0F, 70.0F)), "straight down: up");
        int color = Waypoints.idColor(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        c.eq(230, Math.max(r, Math.max(g, b)), "an id color has 0.9 brightness");
        c.eq(0xFF000000, color & 0xFF000000, "an id color is opaque");
        c.eq(0xFF5555, Waypoints.rgb(TextFormatting.RED), "red");
        c.eq(0xFFAA00, Waypoints.rgb(TextFormatting.GOLD), "gold");
        c.eq(-1, Waypoints.rgb(TextFormatting.BOLD), "bold is no color");
    }
}

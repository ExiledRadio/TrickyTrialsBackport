package com.exiledradio.trickytrialsbackport.effects;

import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.WindBurst;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * What the four effects do, as 1.21.11's {@code WeavingMobEffect}, {@code OozingMobEffect},
 * {@code WindChargedMobEffect}, {@code InfestedMobEffect} and {@code WebBlock}.
 *
 * <h2>On death</h2>
 * 1.21 runs an effect's {@code onMobRemoved} when the dead entity is removed, 20 ticks into its death (players
 * too). Here that is the tick its {@code deathTime} goes from 19 to 20:
 * <ul>
 *   <li><b>Weaving</b>: 2-3 cobwebs among 15 random spots of the 3x3x3 cube around the entity's block, each
 *       replaceable with a solid top below, a puff and the cobweb sound on each; for players always, for
 *       mobs only with {@code mobGriefing}.</li>
 *   <li><b>Oozing</b>: two size-2 slimes half a block up, fewer as nearby slimes (within 2 blocks) near
 *       {@code maxEntityCramming}.</li>
 *   <li><b>Wind Charged</b>: a wind charge's burst from the middle of the body, radius 3-5, knockback 1.22.</li>
 * </ul>
 *
 * <h2>When hurt</h2>
 * <b>Infested</b>: a 10% chance, on any hit that does damage, of 1-2 silverfish thrown out the way the entity
 * looks, up to 90 degrees to either side.
 *
 * <h2>In cobwebs</h2>
 * <b>Weaving</b> lets its wearer through a cobweb at half speed and a quarter fall speed, where 1.12.2's web
 * allows a quarter and a twentieth: after a web-slowed move, the same move is made again with the difference
 * (horizontal once more, vertical four times more). Mobs on the server, the player on their own client.
 *
 * <h2>Immunity</h2>
 * Silverfish cannot be Infested and slimes (not magma cubes) cannot be Oozing, as 1.21's entity tags.
 *
 * <h2>Particles</h2>
 * 1.21 gives each of these effects its own particle in place of the colored swirl. The server leaves them out
 * of the entity's swirl color and tells watching clients which of them to draw ({@link EffectParticlesMessage}).
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class EffectEvents {

    private static final Field IN_WEB = ReflectionHelper.findField(Entity.class, "isInWeb", "field_70134_J");
    private static final Method SET_SLIME_SIZE = ReflectionHelper.findMethod(EntitySlime.class, "setSlimeSize", "func_70799_a",
            int.class, boolean.class);
    @SuppressWarnings("unchecked")
    private static final DataParameter<Integer> POTION_COLOR = (DataParameter<Integer>) get(
            ReflectionHelper.findField(EntityLivingBase.class, "POTION_EFFECTS", "field_184633_f"), null);

    /** Entities whose last move was slowed by a web while they had Weaving; one map per side. */
    private static final Map<Entity, Boolean> WEBBED_SERVER = new WeakHashMap<Entity, Boolean>();
    private static final Map<Entity, Boolean> WEBBED_CLIENT = new WeakHashMap<Entity, Boolean>();

    /** Server: entities with one of our effects, or whose clients still draw one, and what was last sent. */
    private static final Map<EntityLivingBase, Integer> SENT = new WeakHashMap<EntityLivingBase, Integer>();

    private EffectEvents() {
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return the four effects in 1.21's order; empty when the stream is off */
    static List<EffectPotion> ours() {
        if (EffectsContent.windCharged == null) {
            return Collections.emptyList();
        }
        List<EffectPotion> list = new ArrayList<EffectPotion>(4);
        list.add(EffectsContent.windCharged);
        list.add(EffectsContent.weaving);
        list.add(EffectsContent.oozing);
        list.add(EffectsContent.infested);
        return list;
    }

    // ------------------------------------------------------------------------------------------
    // Immunity
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void applicable(PotionEvent.PotionApplicableEvent event) {
        Potion potion = event.getPotionEffect().getPotion();
        if (potion == null || EffectsContent.infested == null) {
            return;
        }
        Class<?> type = event.getEntityLiving().getClass();
        if (potion == EffectsContent.infested && type == EntitySilverfish.class
                || potion == EffectsContent.oozing && type == EntitySlime.class) {
            event.setResult(Event.Result.DENY);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Entity tick: death, webs, particle bookkeeping
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void livingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (EffectsContent.weaving == null) {
            return;
        }
        web(entity);
        if (entity.world.isRemote || entity.getActivePotionMap().isEmpty()) {
            return;
        }
        boolean any = false;
        for (EffectPotion potion : ours()) {
            if (entity.isPotionActive(potion)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        synchronized (SENT) {
            if (!SENT.containsKey(entity)) {
                SENT.put(entity, 0);
            }
        }
        // Vanilla's death update follows this event in the same tick: deathTime 19 becomes 20 and the entity is
        // removed, 1.21's moment for onMobRemoved.
        if (entity.getHealth() <= 0.0F && entity.deathTime == 19 && !entity.isDead) {
            onRemoved(entity);
        }
    }

    /** 1.21's {@code triggerOnDeathMobEffects} for our effects (the entity's others don't act on removal). */
    static void onRemoved(EntityLivingBase entity) {
        World world = entity.world;
        for (PotionEffect effect : new ArrayList<PotionEffect>(entity.getActivePotionEffects())) {
            Potion potion = effect.getPotion();
            if (potion == EffectsContent.weaving) {
                if (entity instanceof EntityPlayer || world.getGameRules().getBoolean("mobGriefing")) {
                    weave(world, entity.getRNG(), new BlockPos(entity.posX, entity.posY, entity.posZ));
                }
            } else if (potion == EffectsContent.oozing) {
                ooze(world, entity);
            } else if (potion == EffectsContent.windCharged) {
                burst(world, entity);
            }
        }
        // 1.21 clears the effects of a removed entity; ours go so nothing triggers twice.
        for (EffectPotion potion : ours()) {
            entity.removePotionEffect(potion);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Weaving
    // ------------------------------------------------------------------------------------------

    /**
     * 1.21's {@code spawnCobwebsRandomlyAround}: 15 random spots of {@code BlockPos.randomInCube(random, 15,
     * pos, 1)}, the first 2-3 distinct ones that are replaceable over a sturdy top.
     *
     * @return the cobwebs placed
     */
    static Set<BlockPos> weave(World world, Random random, BlockPos pos) {
        Set<BlockPos> chosen = new LinkedHashSet<BlockPos>();
        int max = 2 + random.nextInt(2);
        for (int i = 0; i < 15; i++) {
            BlockPos at = new BlockPos(pos.getX() - 1 + random.nextInt(3), pos.getY() - 1 + random.nextInt(3),
                    pos.getZ() - 1 + random.nextInt(3));
            BlockPos below = at.down();
            if (!chosen.contains(at) && world.getBlockState(at).getBlock().isReplaceable(world, at)
                    && world.getBlockState(below).getBlockFaceShape(world, below, EnumFacing.UP) == BlockFaceShape.SOLID) {
                chosen.add(at);
                if (chosen.size() >= max) {
                    break;
                }
            }
        }
        for (BlockPos at : chosen) {
            world.setBlockState(at, Blocks.WEB.getDefaultState(), 3);
            // Level event 3018: ten puffs in the block and the cobweb placing sound.
            if (world instanceof WorldServer) {
                for (int i = 0; i < 10; i++) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL,
                            at.getX() + random.nextDouble(), at.getY() + random.nextDouble(), at.getZ() + random.nextDouble(),
                            0, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, 1.0);
                }
            }
            world.playSound(null, at, EffectsContent.sound(EffectsContent.COBWEB_PLACE), SoundCategory.BLOCKS, 1.0F,
                    (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        }
        return chosen;
    }

    /**
     * Weaving in cobwebs: 1.21's web slows a Weaving entity to (0.5, 0.25, 0.5) of its move, 1.12.2's to (0.25,
     * 0.05, 0.25). After a web-slowed move the rest is moved again, through the same collision as any move.
     */
    private static void web(EntityLivingBase entity) {
        boolean client = entity.world.isRemote;
        // Players move on their own client; everything else on the server.
        if (client ? !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isUser()) : entity instanceof EntityPlayer) {
            return;
        }
        Map<Entity, Boolean> webbed = client ? WEBBED_CLIENT : WEBBED_SERVER;
        boolean weaving = entity.isPotionActive(EffectsContent.weaving);
        Boolean was;
        synchronized (webbed) {
            was = webbed.remove(entity);
        }
        if (was != null && weaving && !entity.isRiding()) {
            double dx = entity.posX - entity.prevPosX;
            double dy = entity.posY - entity.prevPosY;
            double dz = entity.posZ - entity.prevPosZ;
            // Anything bigger than a web lets through was a teleport or a server correction, not the web's move.
            if (dx * dx + dz * dz < 0.25 && Math.abs(dy) < 0.5 && (dx != 0.0 || dy != 0.0 || dz != 0.0)) {
                setInWeb(entity, false);
                entity.move(MoverType.SELF, dx, dy * 4.0, dz);
            }
        }
        if (weaving && isInWeb(entity)) {
            synchronized (webbed) {
                webbed.put(entity, Boolean.TRUE);
            }
        }
    }

    static boolean isInWeb(Entity entity) {
        try {
            return IN_WEB.getBoolean(entity);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    static void setInWeb(Entity entity, boolean value) {
        try {
            IN_WEB.setBoolean(entity, value);
        } catch (IllegalAccessException e) {
            // never: the field was made accessible
        }
    }

    // ------------------------------------------------------------------------------------------
    // Oozing
    // ------------------------------------------------------------------------------------------

    /** 1.21's {@code numberOfSlimesToSpawn}: all of them without a cramming limit, else what fits under it. */
    static int slimesToSpawn(int cramming, int nearby, int count) {
        return cramming < 1 ? count : Math.min(Math.max(0, cramming - nearby), count);
    }

    /** @return the slimes spawned */
    static List<EntitySlime> ooze(World world, EntityLivingBase entity) {
        int count = 2;
        int cramming = world.getGameRules().getInt("maxEntityCramming");
        int nearby = 0;
        if (cramming >= 1) {
            for (EntitySlime slime : world.getEntitiesWithinAABB(EntitySlime.class, entity.getEntityBoundingBox().grow(2.0))) {
                if (slime != entity && slime.getClass() == EntitySlime.class) {
                    nearby++;
                }
            }
        }
        List<EntitySlime> out = new ArrayList<EntitySlime>();
        int n = slimesToSpawn(cramming, nearby, count);
        for (int i = 0; i < n; i++) {
            EntitySlime slime = new EntitySlime(world);
            try {
                SET_SLIME_SIZE.invoke(slime, 2, true);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            slime.setLocationAndAngles(entity.posX, entity.posY + 0.5, entity.posZ, world.rand.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(slime);
            out.add(slime);
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------
    // Wind Charged
    // ------------------------------------------------------------------------------------------

    /**
     * 1.21's wind charge explosion from the middle of the body: radius 3 + 0-2, the wind charge's 1.22
     * knockback, the large gust, the breeze's burst sound. The dying entity is not pushed.
     */
    static void burst(World world, EntityLivingBase entity) {
        float radius = 3.0F + entity.getRNG().nextFloat() * 2.0F;
        WindBurst.burst(world, entity, entity.posX, entity.posY + entity.height / 2.0F, entity.posZ,
                new WindBurst.Settings(radius, 1.22F, true, EffectsContent.sound(EffectsContent.BURST_SOUND), false, null));
    }

    // ------------------------------------------------------------------------------------------
    // Infested
    // ------------------------------------------------------------------------------------------

    /** After every other handler, so only a hit that really lands counts (1.21 calls onMobHurt after the damage). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || EffectsContent.infested == null || event.getAmount() <= 0.0F
                || !entity.isPotionActive(EffectsContent.infested)) {
            return;
        }
        infest(entity);
    }

    /** @return the silverfish released (none 90% of the time) */
    static List<EntitySilverfish> infest(EntityLivingBase entity) {
        List<EntitySilverfish> out = new ArrayList<EntitySilverfish>();
        Random random = entity.getRNG();
        if (random.nextFloat() > 0.1F) {
            return out;
        }
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            out.add(silverfish(entity, entity.posX, entity.posY + entity.height / 2.0, entity.posZ));
        }
        return out;
    }

    private static EntitySilverfish silverfish(EntityLivingBase entity, double x, double y, double z) {
        World world = entity.world;
        Random random = entity.getRNG();
        EntitySilverfish fish = new EntitySilverfish(world);
        float angle = random.nextFloat() * (float) Math.PI - (float) (Math.PI / 2);
        Vec3d look = entity.getLookVec();
        float lx = (float) look.x * 0.3F;
        float ly = (float) look.y * 0.3F * 1.5F;
        float lz = (float) look.z * 0.3F;
        float sin = MathHelper.sin(angle);
        float cos = MathHelper.cos(angle);
        fish.setLocationAndAngles(x, y, z, world.rand.nextFloat() * 360.0F, 0.0F);
        fish.motionX = lx * cos + lz * sin;
        fish.motionY = ly;
        fish.motionZ = -lx * sin + lz * cos;
        world.spawnEntity(fish);
        fish.playSound(SoundEvents.ENTITY_SILVERFISH_HURT, 1.0F, 1.0F);
        return fish;
    }

    // ------------------------------------------------------------------------------------------
    // Particles: swirl color and what clients draw
    // ------------------------------------------------------------------------------------------

    /**
     * What watching clients draw for this entity: bits 0-3 our visible effects in 1.21's order, bit 4 all
     * effects ambient, bits 8-15 how many other effects show particles (the swirl's share of the draws).
     */
    static int particleState(EntityLivingBase entity) {
        List<EffectPotion> ours = ours();
        int bits = 0;
        int others = 0;
        boolean ambient = true;
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            ambient &= effect.getIsAmbient();
            int index = ours.indexOf(effect.getPotion());
            if (!effect.doesShowParticles()) {
                continue;
            }
            if (index >= 0) {
                bits |= 1 << index;
            } else {
                others++;
            }
        }
        if (bits == 0) {
            return 0;
        }
        return bits | (ambient ? 1 << 4 : 0) | Math.min(others, 255) << 8;
    }

    /** The swirl color of the entity's other effects only (0, no swirl, if there are none). */
    static int swirlColor(EntityLivingBase entity) {
        List<PotionEffect> others = new ArrayList<PotionEffect>();
        List<EffectPotion> ours = ours();
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            if (!ours.contains(effect.getPotion())) {
                others.add(effect);
            }
        }
        return others.isEmpty() ? 0 : PotionUtils.getPotionColorFromEffectList(others);
    }

    /** After every entity's update and before the entity tracker sends their data: color and particle state. */
    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        synchronized (SENT) {
            for (Iterator<Map.Entry<EntityLivingBase, Integer>> it = SENT.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<EntityLivingBase, Integer> entry = it.next();
                EntityLivingBase entity = entry.getKey();
                if (entity.world != event.world) {
                    continue;
                }
                boolean any = false;
                if (!entity.isDead) {
                    for (EffectPotion potion : ours()) {
                        any |= entity.isPotionActive(potion);
                    }
                }
                if (any) {
                    int color = swirlColor(entity);
                    if (entity.getDataManager().get(POTION_COLOR) != color) {
                        entity.getDataManager().set(POTION_COLOR, color);
                    }
                }
                int state = any ? particleState(entity) : 0;
                if (state != entry.getValue()) {
                    send(entity, state);
                    entry.setValue(state);
                }
                if (!any) {
                    it.remove();
                }
            }
        }
    }

    private static void send(EntityLivingBase entity, int state) {
        EffectParticlesMessage message = new EffectParticlesMessage(entity.getEntityId(), state);
        NetworkHandler.channel.sendToAllTracking(message, entity);
        if (entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).connection != null) {
            NetworkHandler.channel.sendTo(message, (EntityPlayerMP) entity);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EntityLivingBase) || !(event.getEntityPlayer() instanceof EntityPlayerMP)) {
            return;
        }
        Integer state;
        synchronized (SENT) {
            state = SENT.get(event.getTarget());
        }
        if (state != null && state != 0) {
            NetworkHandler.channel.sendTo(new EffectParticlesMessage(event.getTarget().getEntityId(), state),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    /** @return the particle state last sent for this entity (for the tests) */
    static int sentState(EntityLivingBase entity) {
        synchronized (SENT) {
            Integer state = SENT.get(entity);
            return state == null ? 0 : state;
        }
    }
}

package com.exiledradio.trickytrialsbackport.animals;

import com.exiledradio.trickytrialsbackport.ModConfig;
import com.exiledradio.trickytrialsbackport.TrickyTrialsBackport;
import com.exiledradio.trickytrialsbackport.common.Content;
import com.exiledradio.trickytrialsbackport.network.NetworkHandler;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorProjectileDispense;
import net.minecraft.dispenser.IPosition;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.world.World;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import javax.annotation.Nullable;

/**
 * Stream 16: 1.21.5's farm animal variants. Warm and cold cows, pigs and chickens on vanilla's own entities (an
 * {@link AnimalData} capability, saved in the entity's NBT through patch P18, drawn by our renderers), sheep colors by
 * biome, the blue and brown eggs and their thrown egg (entity {@code egg}, id 40), and wolf voices. Packet 80
 * ({@link AnimalVariantMessage}) carries a variant to clients.
 */
@Mod.EventBusSubscriber(modid = TrickyTrialsBackport.MOD_ID)
public final class AnimalsContent {

    public static final int EGG_ID = 40;

    @Nullable
    public static ItemColoredEgg blueEgg;
    @Nullable
    public static ItemColoredEgg brownEgg;

    private AnimalsContent() {
    }

    public static boolean enabled() {
        return ModConfig.animals.enabled;
    }

    public static boolean eggs() {
        return enabled() && ModConfig.animals.coloredEggs;
    }

    /** @return the egg a chicken of this variant lays, or null for temperate (1.12.2's egg) or without the eggs */
    @Nullable
    public static Item egg(FarmVariant v) {
        return v == FarmVariant.COLD ? blueEgg : v == FarmVariant.WARM ? brownEgg : null;
    }

    /** From the mod's pre-init. */
    public static void preInit() {
        if (enabled()) {
            AnimalData.register();
            WolfVoice.registerSounds();
        }
    }

    /** From the mod's init: the dispenser throws the eggs, as it throws 1.12.2's. */
    public static void init() {
        if (!eggs()) {
            return;
        }
        for (final ItemColoredEgg egg : new ItemColoredEgg[]{blueEgg, brownEgg}) {
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(egg, new BehaviorProjectileDispense() {
                @Override
                protected IProjectile getProjectileEntity(World world, IPosition pos, ItemStack stack) {
                    return new EntityColoredEgg(world, pos.getX(), pos.getY(), pos.getZ(), egg.variant);
                }
            });
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!eggs()) {
            return;
        }
        blueEgg = Content.item(event.getRegistry(), "blue_egg", new ItemColoredEgg(FarmVariant.COLD));
        brownEgg = Content.item(event.getRegistry(), "brown_egg", new ItemColoredEgg(FarmVariant.WARM));
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!eggs()) {
            return;
        }
        // 1.12.2's egg: tracked within 64 blocks, every 10 ticks, with its velocity.
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityColoredEgg.class)
                .id(Content.id("egg"), EGG_ID)
                .name(TrickyTrialsBackport.MOD_ID + ".egg")
                .tracker(64, 10, true)
                .build());
    }

    /**
     * 1.21.5's cake and pumpkin pie take {@code #eggs}. 1.12.2's recipes take its egg; ours are the same recipes with
     * a blue or brown egg in its place.
     */
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!eggs()) {
            return;
        }
        Ingredient egg = Ingredient.fromItems(blueEgg, brownEgg);
        ShapedOreRecipe cake = new ShapedOreRecipe(null, new ItemStack(Items.CAKE), "AAA", "BEB", "CCC",
                'A', Items.MILK_BUCKET, 'B', Items.SUGAR, 'C', Items.WHEAT, 'E', egg);
        cake.setRegistryName(Content.id("cake"));
        event.getRegistry().register(cake);
        ShapelessOreRecipe pie = new ShapelessOreRecipe(null, new ItemStack(Items.PUMPKIN_PIE), Blocks.PUMPKIN, Items.SUGAR, egg);
        pie.setRegistryName(Content.id("pumpkin_pie"));
        event.getRegistry().register(pie);
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (enabled() && FarmAnimals.carries(event.getObject())) {
            event.addCapability(AnimalData.KEY, new AnimalData.Provider());
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (enabled() && FarmAnimals.kindOf(target) != null && event.getEntityPlayer() instanceof EntityPlayerMP
                && AnimalData.of(target) != null) {
            NetworkHandler.channel.sendTo(new AnimalVariantMessage(target.getEntityId(), FarmAnimals.variant(target)),
                    (EntityPlayerMP) event.getEntityPlayer());
        }
    }

    /**
     * A spawn egg used on an animal of its own kind: 1.21.5's {@code AgeableMob.mobInteract} breeds it with itself
     * ({@code getBreedOffspring(level, this)}), so the baby has the animal's variant. 1.12.2 makes that baby with
     * {@code createChild} and fires no {@link BabyEntitySpawnEvent}, so the animal is remembered here, before it
     * handles the click, and the baby that joins the world in that click takes its variant.
     */
    @Nullable
    private static Entity eggParent;

    @SubscribeEvent
    public static void eggOnAnimal(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        eggParent = null;
        Entity target = event.getTarget();
        if (enabled() && !target.world.isRemote && event.getItemStack().getItem() instanceof net.minecraft.item.ItemMonsterPlacer
                && target instanceof net.minecraft.entity.EntityAgeable && FarmAnimals.kindOf(target) != null) {
            eggParent = target;
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOW)
    public static void eggBaby(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        Entity parent = eggParent;
        Entity child = event.getEntity();
        if (parent == null || child.world.isRemote || child.getClass() != parent.getClass()
                || !(child instanceof net.minecraft.entity.EntityAgeable) || !((net.minecraft.entity.EntityAgeable) child).isChild()) {
            return;
        }
        eggParent = null;
        FarmAnimals.bred(parent, parent, child, parent.world.rand);
    }

    @SubscribeEvent
    public static void eggClickDone(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            eggParent = null;
        }
    }

    @SubscribeEvent
    public static void bred(BabyEntitySpawnEvent event) {
        if (enabled() && event.getChild() != null && !event.getParentA().world.isRemote) {
            FarmAnimals.bred(event.getParentA(), event.getParentB(), event.getChild(), event.getParentA().getRNG());
        }
    }

    /**
     * 1.21.5's {@code gameplay/chicken_lay}: a warm chicken lays a brown egg and a cold one a blue egg. 1.12.2's
     * chicken lays when its timer runs out on its living update, which comes after this event in the same tick: a
     * warm or cold chicken about to lay lays ours and restarts the timer, so 1.12.2's never lays.
     */
    @SubscribeEvent
    public static void livingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!eggs() || !ModConfig.animals.chickenVariants || !(event.getEntityLiving() instanceof EntityChicken)) {
            return;
        }
        EntityChicken chicken = (EntityChicken) event.getEntityLiving();
        if (chicken.world.isRemote || chicken.isChild() || chicken.isChickenJockey() || chicken.isDead
                || chicken.timeUntilNextEgg > 1) {
            return;
        }
        Item egg = egg(FarmAnimals.variant(chicken));
        if (egg == null) {
            return;
        }
        chicken.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, (chicken.getRNG().nextFloat() - chicken.getRNG().nextFloat()) * 0.2F + 1.0F);
        chicken.dropItem(egg, 1);
        chicken.timeUntilNextEgg = chicken.getRNG().nextInt(6000) + 6000;
    }
}

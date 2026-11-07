package com.exiledradio.trickytrialsbackport.common;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An advancement trigger that fires with an item and passes when the criterion's {@code "item"} (1.12.2's
 * item predicate) matches it: 1.21's {@code crafter_recipe_crafted}, keyed on what was made.
 */
public final class ItemTrigger implements ICriterionTrigger<ItemTrigger.Instance> {

    private final ResourceLocation id;
    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new HashMap<PlayerAdvancements, Set<Listener<Instance>>>();
    private boolean registered;

    public ItemTrigger(ResourceLocation id) {
        this.id = id;
    }

    /** Registers with the advancement system; before any server loads advancements. */
    public synchronized void register() {
        if (!this.registered) {
            this.registered = true;
            CriteriaTriggers.register(this);
        }
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public synchronized void addListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        Set<Listener<Instance>> set = this.listeners.get(advancements);
        if (set == null) {
            set = new HashSet<Listener<Instance>>();
            this.listeners.put(advancements, set);
        }
        set.add(listener);
    }

    @Override
    public synchronized void removeListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        Set<Listener<Instance>> set = this.listeners.get(advancements);
        if (set != null) {
            set.remove(listener);
            if (set.isEmpty()) {
                this.listeners.remove(advancements);
            }
        }
    }

    @Override
    public synchronized void removeAllListeners(PlayerAdvancements advancements) {
        this.listeners.remove(advancements);
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        return new Instance(this.id, ItemPredicate.deserialize(json.get("item")));
    }

    public void trigger(EntityPlayerMP player, ItemStack stack) {
        PlayerAdvancements advancements = player.getAdvancements();
        List<Listener<Instance>> passed = new ArrayList<Listener<Instance>>();
        synchronized (this) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                return;
            }
            for (Listener<Instance> listener : set) {
                if (listener.getCriterionInstance().item.test(stack)) {
                    passed.add(listener);
                }
            }
        }
        for (Listener<Instance> listener : passed) {
            listener.grantCriterion(advancements);
        }
    }

    /** One criterion: an item predicate. */
    public static final class Instance extends AbstractCriterionInstance {
        final ItemPredicate item;

        Instance(ResourceLocation id, ItemPredicate item) {
            super(id);
            this.item = item;
        }
    }
}

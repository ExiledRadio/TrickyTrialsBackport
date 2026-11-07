package com.exiledradio.trickytrialsbackport.common;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An advancement trigger that fires with a number and passes when it is at least the criterion's minimum,
 * read from one named key: {@code "min_height"} for 1.21's {@code fall_after_explosion} with its
 * {@code distance.y.min}, {@code "min_damage"} for a mace smash's damage. 1.12.2 lacks both.
 */
public final class ValueTrigger implements ICriterionTrigger<ValueTrigger.Instance> {

    private final ResourceLocation id;
    private final String key;
    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new HashMap<PlayerAdvancements, Set<Listener<Instance>>>();
    private boolean registered;

    public ValueTrigger(ResourceLocation id, String key) {
        this.id = id;
        this.key = key;
    }

    /** Registers with the advancement system; call before any server loads advancements (pre-init). */
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
        return new Instance(this.id, JsonUtils.getFloat(json, this.key, 0.0F));
    }

    public void trigger(EntityPlayerMP player, double value) {
        PlayerAdvancements advancements = player.getAdvancements();
        List<Listener<Instance>> passed = new ArrayList<Listener<Instance>>();
        synchronized (this) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                return;
            }
            for (Listener<Instance> listener : set) {
                if (value >= listener.getCriterionInstance().min) {
                    passed.add(listener);
                }
            }
        }
        for (Listener<Instance> listener : passed) {
            listener.grantCriterion(advancements);
        }
    }

    /** One criterion: a minimum. */
    public static final class Instance extends AbstractCriterionInstance {
        final float min;

        Instance(ResourceLocation id, float min) {
            super(id);
            this.min = min;
        }
    }
}

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
 * An advancement trigger that fires with a height and passes when it is at least the criterion's
 * {@code "min_height"}: 1.21's {@code fall_after_explosion} with its {@code distance.y.min}, which 1.12.2
 * lacks.
 */
public final class HeightTrigger implements ICriterionTrigger<HeightTrigger.Instance> {

    private final ResourceLocation id;
    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new HashMap<PlayerAdvancements, Set<Listener<Instance>>>();
    private boolean registered;

    HeightTrigger(ResourceLocation id) {
        this.id = id;
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
        return new Instance(this.id, JsonUtils.getFloat(json, "min_height", 0.0F));
    }

    public void trigger(EntityPlayerMP player, double height) {
        PlayerAdvancements advancements = player.getAdvancements();
        List<Listener<Instance>> passed = new ArrayList<Listener<Instance>>();
        synchronized (this) {
            Set<Listener<Instance>> set = this.listeners.get(advancements);
            if (set == null) {
                return;
            }
            for (Listener<Instance> listener : set) {
                if (height >= listener.getCriterionInstance().minHeight) {
                    passed.add(listener);
                }
            }
        }
        for (Listener<Instance> listener : passed) {
            listener.grantCriterion(advancements);
        }
    }

    /** One criterion: a minimum height. */
    public static final class Instance extends AbstractCriterionInstance {
        final float minHeight;

        Instance(ResourceLocation id, float minHeight) {
            super(id);
            this.minHeight = minHeight;
        }
    }
}

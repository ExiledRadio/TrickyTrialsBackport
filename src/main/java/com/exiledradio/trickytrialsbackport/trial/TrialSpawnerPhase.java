package com.exiledradio.trickytrialsbackport.trial;

import net.minecraft.util.IStringSerializable;

/** 1.21's {@code TrialSpawnerState}: the block state property {@code trial_spawner_state}, with its light and look. */
public enum TrialSpawnerPhase implements IStringSerializable {
    INACTIVE("inactive", 0, Particles.NONE, -1.0, false),
    WAITING_FOR_PLAYERS("waiting_for_players", 4, Particles.SMALL_FLAMES, 200.0, true),
    ACTIVE("active", 8, Particles.FLAMES_AND_SMOKE, 1000.0, true),
    WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, Particles.SMALL_FLAMES, -1.0, false),
    EJECTING_REWARD("ejecting_reward", 8, Particles.SMALL_FLAMES, -1.0, false),
    COOLDOWN("cooldown", 0, Particles.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

    /** What the client draws in each phase (1.21's {@code ParticleEmission}). */
    public enum Particles {
        NONE, SMALL_FLAMES, FLAMES_AND_SMOKE, SMOKE_INSIDE_AND_TOP_FACE
    }

    private final String name;
    public final int light;
    public final Particles particles;
    public final double spinningMobSpeed;
    public final boolean capableOfSpawning;

    TrialSpawnerPhase(String name, int light, Particles particles, double spinningMobSpeed, boolean capableOfSpawning) {
        this.name = name;
        this.light = light;
        this.particles = particles;
        this.spinningMobSpeed = spinningMobSpeed;
        this.capableOfSpawning = capableOfSpawning;
    }

    public boolean hasSpinningMob() {
        return this.spinningMobSpeed >= 0.0;
    }

    @Override
    public String getName() {
        return this.name;
    }
}

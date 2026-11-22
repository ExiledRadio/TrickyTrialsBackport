package com.exiledradio.trickytrialsbackport.creaking.client.anim;

/** What an animation channel moves: 1.21's {@code ModelPart} offsets. No client types, so tests can sample poses on a server. */
public interface Posable {

    void offsetPos(Vec3f v);

    void offsetRotation(Vec3f v);

    void offsetScale(Vec3f v);
}

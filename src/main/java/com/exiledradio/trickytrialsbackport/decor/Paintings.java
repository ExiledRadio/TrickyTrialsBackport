package com.exiledradio.trickytrialsbackport.decor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The paintings 1.21 and 1.21.7 added, with 1.21.11's sizes in blocks ({@code data/minecraft/painting_variant}), in
 * the order of its {@code placeable} tag. All 21 are placeable. 1.12.2's own 26 are 1.21's too, unchanged, and stay
 * vanilla paintings.
 */
public final class Paintings {

    /** One painting: its name (texture {@code textures/painting/<name>.png}) and its size in blocks. */
    public static final class Variant {
        public final String name;
        public final int width;
        public final int height;
        public final int index;

        Variant(String name, int width, int height, int index) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.index = index;
        }

        public int area() {
            return this.width * this.height;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private static final List<Variant> ALL = new ArrayList<Variant>();

    static {
        add("baroque", 2, 2);
        add("humble", 2, 2);
        add("meditative", 1, 1);
        add("prairie_ride", 1, 2);
        add("unpacked", 4, 4);
        add("backyard", 3, 4);
        add("bouquet", 3, 3);
        add("cavebird", 3, 3);
        add("changing", 4, 2);
        add("cotan", 3, 3);
        add("endboss", 3, 3);
        add("fern", 3, 3);
        add("finding", 4, 2);
        add("lowmist", 4, 2);
        add("orb", 4, 4);
        add("owlemons", 3, 3);
        add("passage", 4, 2);
        add("pond", 3, 4);
        add("sunflowers", 3, 3);
        add("tides", 3, 3);
        add("dennis", 3, 3);
    }

    private Paintings() {
    }

    private static void add(String name, int width, int height) {
        ALL.add(new Variant(name, width, height, ALL.size()));
    }

    public static List<Variant> all() {
        return Collections.unmodifiableList(ALL);
    }

    @Nullable
    public static Variant byName(String name) {
        for (Variant v : ALL) {
            if (v.name.equals(name)) {
                return v;
            }
        }
        return null;
    }

    public static Variant byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : ALL.get(0);
    }
}

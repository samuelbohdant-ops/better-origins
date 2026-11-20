package com.iafenvoy.origins.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Better Origins: configuration based on NeoForge's own config system (ModConfigSpec)
 * instead of the external Jupiter library. File: config/origins-common.toml.
 * In game: Mods -> Better Origins -> Config.
 *
 * Field names and getValue() are the same as before, so the rest of the code did not change.
 * Before the file is loaded, getValue() returns the default value instead of crashing.
 */
public final class OriginsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final OriginsConfig INSTANCE = new OriginsConfig(BUILDER);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public final General general;
    public final ModifyPlayerSpawnPower modifyPlayerSpawnPower;
    public final Debug debug;

    private OriginsConfig(ModConfigSpec.Builder builder) {
        this.general = new General(builder);
        this.modifyPlayerSpawnPower = new ModifyPlayerSpawnPower(builder);
        this.debug = new Debug(builder);
    }

    /** A config value that can be read at any time (default value until the file is loaded). */
    public static final class Entry<T> {
        private final ModConfigSpec.ConfigValue<T> value;

        private Entry(ModConfigSpec.ConfigValue<T> value) {
            this.value = value;
        }

        public T getValue() {
            return SPEC != null && SPEC.isLoaded() ? this.value.get() : this.value.getDefault();
        }
    }

    public static final class General {
        public final Entry<Integer> permissionLevel;
        public final Entry<Boolean> compactUsabilityHints;
        public final Entry<Integer> hudOffsetX;
        public final Entry<Integer> hudOffsetY;

        private General(ModConfigSpec.Builder builder) {
            builder.translation("category.origins.general.title").push("general");
            this.permissionLevel = new Entry<>(builder
                    .comment("Permission level used when powers run commands (0-4)")
                    .translation("config.origins.general.permissionLevel")
                    .defineInRange("permissionLevel", 2, 0, 4));
            this.compactUsabilityHints = new Entry<>(builder
                    .translation("config.origins.general.compactUsabilityHints")
                    .define("compactUsabilityHints", false));
            this.hudOffsetX = new Entry<>(builder
                    .comment("Horizontal offset of the power bars on the screen")
                    .translation("config.origins.general.hudOffsetX")
                    .defineInRange("hudOffsetX", 0, Integer.MIN_VALUE, Integer.MAX_VALUE));
            this.hudOffsetY = new Entry<>(builder
                    .comment("Vertical offset of the power bars on the screen")
                    .translation("config.origins.general.hudOffsetY")
                    .defineInRange("hudOffsetY", 0, Integer.MIN_VALUE, Integer.MAX_VALUE));
            builder.pop();
        }
    }

    public static final class ModifyPlayerSpawnPower {
        public final Entry<Integer> radius;
        public final Entry<Integer> horizontalBlockCheckInterval;
        public final Entry<Integer> verticalBlockCheckInterval;

        private ModifyPlayerSpawnPower(ModConfigSpec.Builder builder) {
            builder.translation("category.origins.modifyPlayerSpawnPower.title").push("modifyPlayerSpawnPower");
            this.radius = new Entry<>(builder
                    .translation("config.origins.modifyPlayerSpawnPower.radius")
                    .defineInRange("radius", 6400, 1, Integer.MAX_VALUE));
            this.horizontalBlockCheckInterval = new Entry<>(builder
                    .translation("config.origins.modifyPlayerSpawnPower.horizontalBlockCheckInterval")
                    .defineInRange("horizontalBlockCheckInterval", 64, 0, Integer.MAX_VALUE));
            this.verticalBlockCheckInterval = new Entry<>(builder
                    .translation("config.origins.modifyPlayerSpawnPower.verticalBlockCheckInterval")
                    .defineInRange("verticalBlockCheckInterval", 64, 0, Integer.MAX_VALUE));
            builder.pop();
        }
    }

    public static final class Debug {
        public final Entry<Boolean> builtinRegistries;
        public final Entry<Boolean> dynamicRegistries;

        private Debug(ModConfigSpec.Builder builder) {
            builder.translation("category.origins.debug.title").push("debug");
            this.builtinRegistries = new Entry<>(builder
                    .translation("config.origins.debug.builtinRegistries")
                    .define("builtinRegistries", false));
            this.dynamicRegistries = new Entry<>(builder
                    .translation("config.origins.debug.dynamicRegistries")
                    .define("dynamicRegistries", false));
            builder.pop();
        }
    }
}

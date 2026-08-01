package com.velocityspider.createjetengines.engine;

import net.minecraft.util.StringRepresentable;

/**
 * The five module kinds a jet engine chain can be built from.
 *
 * <p>A chain reads, from intake to exhaust:
 * {@code FAN -> COMPRESSOR(1..n) -> COMBUSTION -> [AFTERBURNER] -> NOZZLE}.
 */
public enum ModuleType implements StringRepresentable {

    FAN("fan"),
    COMPRESSOR("compressor"),
    COMBUSTION("combustion_core"),
    AFTERBURNER("afterburner"),
    NOZZLE("nozzle");

    private final String name;

    ModuleType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}

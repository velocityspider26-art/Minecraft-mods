package com.velocityspider.crashphysics.material;

import com.velocityspider.crashphysics.physics.MaterialProfile;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the crash simulation needs to know about one block state.
 *
 * @param profile    density and strengths
 * @param fracture   how it fails
 * @param ejecta     what it is thrown out as when dug out of a crater, or null if it is not thrown
 * @param scar       what the block underneath a crater turns into (torn turf, compacted soil), or null
 * @param skid       what it turns into when a vehicle slides over it, or null
 * @param volatility explosion power when it is destroyed in a crash (burners, engines, fuel), 0 for none
 * @param ignites    if destroying it starts fires (hot burners, furnaces, campfires)
 */
public record CrashMaterial(MaterialProfile profile, FractureMode fracture, @Nullable BlockState ejecta, @Nullable BlockState scar,
                            @Nullable BlockState skid, float volatility, boolean ignites) {

    public static final CrashMaterial UNBREAKABLE = new CrashMaterial(MaterialProfile.UNBREAKABLE, FractureMode.BRITTLE, null, null, null, 0.0f, false);

    public boolean unbreakable() {
        return this.profile.unbreakable();
    }
}

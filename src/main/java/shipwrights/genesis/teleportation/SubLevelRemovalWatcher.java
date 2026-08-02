package shipwrights.genesis.teleportation;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import shipwrights.genesis.GenesisMod;

import java.util.HashSet;
import java.util.Set;

/**
 * Reports who removes a sub-level, and why.
 *
 * <p>Transferred craft were disappearing about a second after arriving, with
 * the destination container reporting zero sub-levels and no explanation:
 * Sable's own unload logging is compile-time gated off, so the removal was
 * invisible. This attaches to each container and records the reason together
 * with the call site, which is the only way to tell an unload (nothing is
 * holding the chunk) apart from a deliberate removal (something asked for it).
 * Purely diagnostic — it changes no behaviour.</p>
 */
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class SubLevelRemovalWatcher {
    /** Levels already carrying the watcher, so it is attached exactly once. */
    private static final Set<ResourceKey<Level>> ATTACHED = new HashSet<>();

    private SubLevelRemovalWatcher() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ATTACHED.contains(level.dimension())) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }

        ATTACHED.add(level.dimension());
        container.addObserver(new SubLevelObserver() {
            @Override
            public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
                StringBuilder origin = new StringBuilder();
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                // Skip the getStackTrace/observer frames; report the callers that
                // actually decided to remove it.
                for (int i = 3; i < Math.min(stack.length, 14); i++) {
                    origin.append("\n    at ").append(stack[i]);
                }
                GenesisMod.LOGGER.warn("[REMOVED] sub-level {} in {} reason={}{}",
                        subLevel.getUniqueId(), level.dimension().location(), reason, origin);
            }
        });
        GenesisMod.LOGGER.info("[REMOVED] watching sub-level removals in {}", level.dimension().location());
    }
}

package shipwrights.genesis.tests;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.compat.aeronautics.AeronauticsConstruct;
import shipwrights.genesis.compat.aeronautics.AeronauticsContraptionLookup;

import java.util.UUID;

/**
 * GameTests for the Genesis construct teleportation system, ported to Create Aeronautics.
 *
 * <p>Tests assert only on observable outcomes — which dimension a construct or entity ends up in —
 * so they stay valid across teleportation redesigns. Construct assembly goes through
 * {@link TestShipHelper#assembleConstruct}, which uses Sable's assembly helper.</p>
 *
 * <p>The Valkyrien-Skies-only "connected ships teleport together" hinge test has no Create
 * Aeronautics equivalent (CA has no ship-to-ship revolute joints) and was removed during the port.</p>
 */
@GameTestHolder(GenesisMod.MOD_ID)
public class TeleportGameTests {

    private static final ResourceKey<Level> SPACE_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, GenesisMod.SPACE_DIM);


    private static final BlockPos SHIP_ASSEMBLY_REL_POS = new BlockPos(2, 0, 2);

    /** Smoke test: the GameTest framework can discover tests in the {@code genesis} namespace. */
    @GameTest
    public static void emptyPlatform(GameTestHelper helper) {
        helper.succeed();
    }

    /**
     * A construct that climbs past the Kármán line crosses seamlessly into 3D space (the
     * {@code great_unknown} space dimension), where it can then fly toward the Moon.
     */
    @GameTest(timeoutTicks = 100)
    public static void karmanCrossing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension '" + GenesisMod.SPACE_DIM + "' not loaded");
            return;
        }

        AeronauticsConstruct construct = TestShipHelper.assembleConstruct(helper, SHIP_ASSEMBLY_REL_POS);
        if (construct == null) {
            helper.fail("Failed to assemble construct (is Create Aeronautics present?)");
            return;
        }
        UUID id = construct.id();
        TestShipHelper.moveConstructAboveKarman(level, construct);

        helper.succeedWhen(() -> {
            if (AeronauticsContraptionLookup.getConstructById(spaceLevel, id) == null) {
                throw new GameTestAssertException("Construct not in space yet");
            }
        });
    }

    /** A construct in space within a celestial's collision radius is moved to that planet. */
    @GameTest(timeoutTicks = 100)
    public static void planetEntry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension not loaded");
            return;
        }

        AeronauticsConstruct construct = TestShipHelper.assembleConstruct(helper, SHIP_ASSEMBLY_REL_POS);
        if (construct == null) {
            helper.fail("Failed to assemble construct");
            return;
        }
        // Push into space near the overworld celestial, then expect return to the overworld.
        AeronauticsConstruct spaceConstruct = shipwrights.genesis.compat.aeronautics.AeronauticsTeleportHelper.teleportToLevel(
                construct, spaceLevel,
                construct.positionInWorld(), construct.rotation(),
                new org.joml.Vector3d(), new org.joml.Vector3d());
        if (spaceConstruct == null) {
            helper.fail("Failed to move construct into space");
            return;
        }
        UUID id = spaceConstruct.id();
        TestShipHelper.moveConstructNearPlanet(spaceLevel, spaceConstruct, ResourceLocation.withDefaultNamespace("overworld"));

        ServerLevel overworld = level.getServer().overworld();
        helper.succeedWhen(() -> {
            if (AeronauticsContraptionLookup.getConstructById(overworld, id) == null) {
                throw new GameTestAssertException("Construct did not return to the overworld yet");
            }
        });
    }

    /** An entity riding a construct travels with it across the Kármán line into space. */
    @GameTest(timeoutTicks = 200)
    public static void entityTeleportsWithShip(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel spaceLevel = level.getServer().getLevel(SPACE_DIM_KEY);
        if (spaceLevel == null) {
            helper.fail("Space dimension not loaded");
            return;
        }

        AeronauticsConstruct construct = TestShipHelper.assembleConstruct(helper, SHIP_ASSEMBLY_REL_POS);
        if (construct == null) {
            helper.fail("Failed to assemble construct");
            return;
        }

        BlockPos pigPos = helper.absolutePos(SHIP_ASSEMBLY_REL_POS).above();
        Pig pig = EntityType.PIG.create(level);
        if (pig == null) {
            helper.fail("Could not create test pig");
            return;
        }
        pig.setPos(pigPos.getX() + 0.5, pigPos.getY(), pigPos.getZ() + 0.5);
        level.addFreshEntity(pig);
        UUID pigId = pig.getUUID();

        TestShipHelper.moveConstructAboveKarman(level, construct);

        helper.succeedWhen(() -> {
            if (spaceLevel.getEntity(pigId) == null) {
                throw new GameTestAssertException("Pig has not travelled to space with the construct yet");
            }
        });
    }
}

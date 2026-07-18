package net.mcreator.crustychunks.init;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CrustyChunksModSounds {
   public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "crusty_chunks");
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_LAUNCH = REGISTRY.register(
      "rocket_launch", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "rocket_launch"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> GOLEMIDLE = REGISTRY.register(
      "golemidle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "golemidle"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> GOLEMMAD = REGISTRY.register(
      "golemmad", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "golemmad"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MAGAZINE = REGISTRY.register(
      "magazine", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "magazine"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER = REGISTRY.register(
      "revolver", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "revolver"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SMALLEXPLOSION = REGISTRY.register(
      "smallexplosion", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "smallexplosion"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BOUNCE = REGISTRY.register(
      "bounce", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "bounce"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUNRELOAD = REGISTRY.register(
      "shotgunreload", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "shotgunreload"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> CANNONFAR = REGISTRY.register(
      "cannonfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "cannonfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SMALLCASING = REGISTRY.register(
      "smallcasing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "smallcasing"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUMCASING = REGISTRY.register(
      "mediumcasing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "mediumcasing"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUNCASING = REGISTRY.register(
      "shotguncasing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "shotguncasing"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MECHSTEP = REGISTRY.register(
      "mechstep", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "mechstep"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> HUNTERFAR = REGISTRY.register(
      "hunterfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "hunterfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> HUNTERNEAR = REGISTRY.register(
      "hunternear", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "hunternear"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> DISTANTSHOT = REGISTRY.register(
      "distantshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "distantshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> FARBLAST = REGISTRY.register(
      "farblast", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "farblast"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> PEELERPOD = REGISTRY.register(
      "peelerpod", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "peelerpod"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> FLAREGUN = REGISTRY.register(
      "flaregun", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "flaregun"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> RUMBLE = REGISTRY.register(
      "rumble", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "rumble"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKETFLIGHT = REGISTRY.register(
      "rocketflight", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "rocketflight"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> ROCKETFAR = REGISTRY.register(
      "rocketfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "rocketfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> HEAVYLAUNCH = REGISTRY.register(
      "heavylaunch", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "heavylaunch"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> WIZZ = REGISTRY.register("wizz", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "wizz")));
   public static final DeferredHolder<SoundEvent, SoundEvent> SPARKS = REGISTRY.register(
      "sparks", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "sparks"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> HUMM = REGISTRY.register("humm", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "humm")));
   public static final DeferredHolder<SoundEvent, SoundEvent> STRIKERSTEP = REGISTRY.register(
      "strikerstep", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "strikerstep"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SONICBOOM = REGISTRY.register(
      "sonicboom", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "sonicboom"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE = REGISTRY.register(
      "turbine", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "turbine"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MEGAMECHSTEP = REGISTRY.register(
      "megamechstep", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "megamechstep"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SIRENFAR = REGISTRY.register(
      "sirenfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "sirenfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SIREN = REGISTRY.register("siren", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "siren")));
   public static final DeferredHolder<SoundEvent, SoundEvent> DRONE = REGISTRY.register("drone", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "drone")));
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT = REGISTRY.register("bolt", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "bolt")));
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLTRELOAD = REGISTRY.register(
      "boltreload", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "boltreload"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT3 = REGISTRY.register("bolt3", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "bolt3")));
   public static final DeferredHolder<SoundEvent, SoundEvent> BOLT2 = REGISTRY.register("bolt2", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "bolt2")));
   public static final DeferredHolder<SoundEvent, SoundEvent> DRYFIRE = REGISTRY.register(
      "dryfire", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "dryfire"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> RIFLEMAGAZINE = REGISTRY.register(
      "riflemagazine", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "riflemagazine"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVERRELOAD = REGISTRY.register(
      "revolverreload", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "revolverreload"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BEEP = REGISTRY.register("beep", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "beep")));
   public static final DeferredHolder<SoundEvent, SoundEvent> PEELERPODFAR = REGISTRY.register(
      "peelerpodfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "peelerpodfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> DISTANTSHOTMEDIUM = REGISTRY.register(
      "distantshotmedium", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "distantshotmedium"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MOTOR = REGISTRY.register("motor", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "motor")));
   public static final DeferredHolder<SoundEvent, SoundEvent> LEVERACTION = REGISTRY.register(
      "leveraction", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "leveraction"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDERWAFFING = REGISTRY.register(
      "commanderwaffing", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "commanderwaffing"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> COMMANDERALERT = REGISTRY.register(
      "commanderalert", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "commanderalert"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SPACENUKE = REGISTRY.register(
      "spacenuke", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "spacenuke"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SMALLFARBLAST = REGISTRY.register(
      "smallfarblast", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "smallfarblast"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> DISTANTGUNFIRE = REGISTRY.register(
      "distantgunfire", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "distantgunfire"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BLOOP = REGISTRY.register("bloop", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "bloop")));
   public static final DeferredHolder<SoundEvent, SoundEvent> ARTYFALL = REGISTRY.register(
      "artyfall", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "artyfall"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> GLORY = REGISTRY.register("glory", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "glory")));
   public static final DeferredHolder<SoundEvent, SoundEvent> SURRENDER = REGISTRY.register(
      "surrender", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "surrender"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> LOCATED = REGISTRY.register(
      "located", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "located"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SEARCHING = REGISTRY.register(
      "searching", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "searching"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> PETROLENGINE = REGISTRY.register(
      "petrolengine", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "petrolengine"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> AUTOCANNONSHOT = REGISTRY.register(
      "autocannonshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "autocannonshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> HEAVYAUTOCANNONSHOT = REGISTRY.register(
      "heavyautocannonshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "heavyautocannonshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SMALLSHOT = REGISTRY.register(
      "smallshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "smallshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MEDIUMSHOT = REGISTRY.register(
      "mediumshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "mediumshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SILENCEDSHOT = REGISTRY.register(
      "silencedshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "silencedshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BATTLECANNON = REGISTRY.register(
      "battlecannon", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "battlecannon"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> CANNONCLOSE = REGISTRY.register(
      "cannonclose", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "cannonclose"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> FABRICATOR = REGISTRY.register(
      "fabricator", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "fabricator"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> JETFAR = REGISTRY.register(
      "jetfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "jetfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> JETIDLE = REGISTRY.register(
      "jetidle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "jetidle"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> JETSTART = REGISTRY.register(
      "jetstart", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "jetstart"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> BULLETCRACK = REGISTRY.register(
      "bulletcrack", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "bulletcrack"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> PISTOLMAGIN = REGISTRY.register(
      "pistolmagin", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "pistolmagin"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> PISTOLACTION = REGISTRY.register(
      "pistolaction", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "pistolaction"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> GUNMECHANISM = REGISTRY.register(
      "gunmechanism", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "gunmechanism"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> AUTOLOADER = REGISTRY.register(
      "autoloader", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "autoloader"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> MIDRANGESHOT = REGISTRY.register(
      "midrangeshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "midrangeshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> LARGESHOT = REGISTRY.register(
      "largeshot", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "largeshot"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSIONSMALL = REGISTRY.register(
      "explosionsmall", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "explosionsmall"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSIONLARGE = REGISTRY.register(
      "explosionlarge", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "explosionlarge"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSIONCOLOSSAL = REGISTRY.register(
      "explosioncolossal", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "explosioncolossal"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSIONCOLOSSALFAR = REGISTRY.register(
      "explosioncolossalfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "explosioncolossalfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSIONLARGEFAR = REGISTRY.register(
      "explosionlargefar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "explosionlargefar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSIONSMALLFAR = REGISTRY.register(
      "explosionsmallfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "explosionsmallfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER = REGISTRY.register(
      "geiger", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "geiger"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> TINYEXPLOSIONFAR = REGISTRY.register(
      "tinyexplosionfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "tinyexplosionfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> ROTARYNEAR = REGISTRY.register(
      "rotarynear", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "rotarynear"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> ROTARYFAR = REGISTRY.register(
      "rotaryfar", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "rotaryfar"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> DIESELENGINEIDLE = REGISTRY.register(
      "dieselengineidle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "dieselengineidle"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> DIESELENGINEREV = REGISTRY.register(
      "dieselenginerev", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "dieselenginerev"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> LARGEDIESELENGINE = REGISTRY.register(
      "largedieselengine", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "largedieselengine"))
   );
   public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUNCYCLE = REGISTRY.register(
      "shotguncycle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("crusty_chunks", "shotguncycle"))
   );
}

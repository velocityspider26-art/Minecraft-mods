package net.mcreator.crustychunks.procedures;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;

@EventBusSubscriber({Dist.CLIENT})
public class ClientSoundUtils {
   private static final List<ClientSoundUtils.DelayedSound> DELAYED_SOUNDS = new ArrayList<>();
   private static final double SPEED_OF_SOUND = 343.0;

   public static void playSoundAtDistance(double x, double y, double z, String soundPath, float volume, float pitch) {
      if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            double dist = Math.sqrt(mc.player.distanceToSqr(x, y, z));
            int delay = travelDelayEnabled() ? (int)(dist / 17.15) : 0;
            playSoundDelayedTicks(delay, x, y, z, soundPath, volume, pitch);
         }
      }
   }

   /** Queue a sound on the client tick queue (no extra threads). delayTicks <= 0 plays immediately. */
   public static void playSoundDelayedTicks(int delayTicks, double x, double y, double z, String soundPath, float volume, float pitch) {
      Vec3 pos = new Vec3(x, y, z);
      if (delayTicks <= 0) {
         play(pos, soundPath, volume, pitch);
      } else {
         synchronized (DELAYED_SOUNDS) {
            DELAYED_SOUNDS.add(new ClientSoundUtils.DelayedSound(delayTicks, pos, soundPath, volume, pitch));
         }
      }
   }

   /** Config: speed-of-sound simulation can be disabled if delayed audio is unwanted. */
   public static boolean travelDelayEnabled() {
      try {
         return net.mcreator.crustychunks.WariumConfig.SOUND_TRAVEL_DELAY.get();
      } catch (Throwable t) {
         return true;
      }
   }

   private static void play(Vec3 pos, String path, float vol, float pitch) {
      SoundEvent event = (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.tryParse(path));
      Level level = Minecraft.getInstance().level;
      if (event != null && level != null) {
         level.playLocalSound(pos.x, pos.y, pos.z, event, SoundSource.NEUTRAL, vol, pitch, false);
      }
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post e) {
      if (true) {
         synchronized (DELAYED_SOUNDS) {
            Iterator<ClientSoundUtils.DelayedSound> it = DELAYED_SOUNDS.iterator();

            while (it.hasNext()) {
               ClientSoundUtils.DelayedSound s = it.next();
               if (--s.ticks <= 0) {
                  play(s.pos, s.path, s.vol, s.pitch);
                  it.remove();
               }
            }
         }
      }
   }

   private static class DelayedSound {
      int ticks;
      Vec3 pos;
      String path;
      float vol;
      float pitch;

      DelayedSound(int t, Vec3 p, String pa, float v, float pi) {
         this.ticks = t;
         this.pos = p;
         this.path = pa;
         this.vol = v;
         this.pitch = pi;
      }
   }
}

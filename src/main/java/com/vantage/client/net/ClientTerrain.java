package com.vantage.client.net;

import com.vantage.net.PlanetInfo;
import com.vantage.net.TerrainColumns;
import com.vantage.net.TerrainRequest;
import com.vantage.net.VantageNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Client end of Vantage's network messages: what the server said, and requests awaiting answers. */
public final class ClientTerrain implements VantageNetwork.ClientHandler {
    public static final ClientTerrain INSTANCE = new ClientTerrain();

    private record Pending(Consumer<TerrainColumns> callback, long sentAt) {
    }

    private volatile @Nullable PlanetInfo planet;
    private final Map<Long, Pending> pending = new ConcurrentHashMap<>();

    private ClientTerrain() {
    }

    @Override
    public void planet(PlanetInfo info) {
        this.planet = info;
    }

    /** What the server said about this dimension, or null if it said nothing (no Vantage there). */
    public @Nullable PlanetInfo planet(ResourceKey<Level> dimension) {
        PlanetInfo p = this.planet;
        return p != null && p.dimension().equals(dimension) ? p : null;
    }

    /**
     * Sends a request; {@code callback} later gets the answer, or null if none came in time.
     *
     * @return false if the same section column is already being asked for
     */
    public boolean send(TerrainRequest request, Consumer<TerrainColumns> callback) {
        long key = key(request.level(), request.sx(), request.sz());
        if (this.pending.putIfAbsent(key, new Pending(callback, System.currentTimeMillis())) != null) {
            return false;
        }
        try {
            if (Minecraft.getInstance().getConnection() == null) {
                throw new IllegalStateException("not connected");
            }
            PacketDistributor.sendToServer(request);
            return true;
        } catch (RuntimeException e) {
            this.pending.remove(key);
            callback.accept(null);
            return true;
        }
    }

    @Override
    public void columns(TerrainColumns columns) {
        Pending p = this.pending.remove(key(columns.level(), columns.sx(), columns.sz()));
        if (p != null) {
            p.callback().accept(columns);
        }
    }

    /** Gives up on requests older than {@code timeoutMillis}. Main thread. */
    public void expire(long timeoutMillis) {
        long now = System.currentTimeMillis();
        List<Pending> late = new ArrayList<>();
        this.pending.entrySet().removeIf(e -> {
            if (now - e.getValue().sentAt() > timeoutMillis) {
                late.add(e.getValue());
                return true;
            }
            return false;
        });
        for (Pending p : late) {
            p.callback().accept(null);
        }
    }

    /** Forgets everything (logging out or changing dimension). */
    public void reset(boolean forgetPlanet) {
        List<Pending> all = new ArrayList<>(this.pending.values());
        this.pending.clear();
        for (Pending p : all) {
            p.callback().accept(null);
        }
        if (forgetPlanet) {
            this.planet = null;
        }
    }

    private static long key(int level, int sx, int sz) {
        return ((long) level << 58) ^ ((long) (sx & 0x1FFFFFFF) << 29) ^ (sz & 0x1FFFFFFF);
    }
}

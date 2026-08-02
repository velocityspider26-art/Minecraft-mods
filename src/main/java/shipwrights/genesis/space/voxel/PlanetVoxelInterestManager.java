package shipwrights.genesis.space.voxel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.networking.PlanetVoxelBrickPacket;
import shipwrights.genesis.networking.PlanetVoxelInterestPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Per-player, viewport-driven streaming for the authoritative planet pyramid.
 * Selection work stays off the server thread and packet emission is bounded on
 * both a per-player and global basis.
 */
final class PlanetVoxelInterestManager implements AutoCloseable {
    private static final int MAX_RESIDENT_SNAPSHOT = 100_000;
    // Enough for every root of a complete coarse cube: six faces x 8x8 bricks
    // x two vertical layers at the coverage LOD. Sending half of them left the
    // far side of the planet with nothing to draw at all.
    private static final int MAX_BASELINE_ROOTS = 1024;
    private static final int MAX_DESIRED_BRICKS = 8_192;
    private static final int MAX_PREDICTION_REQUESTS = 512;
    private static final int MAX_PENDING_PER_PLAYER = 10_000;
    private static final int MAX_SENT_REVISIONS = 16_384;
    private static final int MAX_PACKETS_PER_PLAYER_TICK = 8;
    private static final int MAX_PACKETS_GLOBAL_TICK = 48;
    private static final long MAX_BYTES_PER_PLAYER_TICK = 256L * 1024L;
    private static final long MAX_BYTES_GLOBAL_TICK = 1024L * 1024L;

    private final MinecraftServer server;
    private final PlanetVoxelStore store;
    private final PredictionRequester predictionRequester;
    private final Map<UUID, PlayerState> players = new LinkedHashMap<>();
    private final ThreadPoolExecutor selectorWorker;
    private boolean closed;
    private double cubeHalfExtent = 1.0;
    private int seaLevel = 63;

    PlanetVoxelInterestManager(MinecraftServer server, PlanetVoxelStore store,
                               PredictionRequester predictionRequester) {
        this.server = server;
        this.store = store;
        this.predictionRequester = predictionRequester;
        this.selectorWorker = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(8), runnable -> {
                    Thread thread = new Thread(runnable, "Genesis-PlanetVoxelInterest");
                    thread.setDaemon(true);
                    thread.setPriority(Thread.MIN_PRIORITY);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    void updateGeometry(double newCubeHalfExtent, int newSeaLevel) {
        if (!Double.isFinite(newCubeHalfExtent) || newCubeHalfExtent <= 0.0) return;
        boolean changed = Math.abs(cubeHalfExtent - newCubeHalfExtent) > 1.0E-6
                || seaLevel != newSeaLevel;
        cubeHalfExtent = newCubeHalfExtent;
        seaLevel = newSeaLevel;
        if (changed) {
            for (PlayerState state : players.values()) state.refreshPending = true;
        }
    }

    void onLogin(ServerPlayer player, ResourceLocation planet) {
        if (closed || player == null) return;
        PlayerState state = players.computeIfAbsent(player.getUUID(), PlayerState::new);
        if (state.request == null) {
            state.request = InterestRequest.bootstrap(planet, cubeHalfExtent);
            state.token++;
            state.refreshPending = true;
        }
    }

    void onLogout(ServerPlayer player) {
        if (player != null) players.remove(player.getUUID());
    }

    void update(ServerPlayer player, PlanetVoxelInterestPacket packet) {
        if (closed || player == null || packet == null) return;
        PlayerState state = players.computeIfAbsent(player.getUUID(), PlayerState::new);
        state.request = InterestRequest.from(packet);
        state.token++;
        state.refreshPending = true;
        startRefresh(state);
    }

    void publishUpsert(ResourceLocation planet, PlanetVoxelBrick brick) {
        if (closed || brick == null) return;
        for (PlayerState state : players.values()) {
            if (state.request == null || !state.request.planet.equals(planet)) continue;
            if (state.desiredKeys.contains(brick.key())
                    || state.sentRevisions.containsKey(brick.key())) {
                enqueuePut(state, planet, brick);
            }
            state.refreshPending = true;
        }
    }

    void publishRemoval(ResourceLocation planet, PlanetVoxelBrickKey key, long revision) {
        if (closed || key == null) return;
        for (PlayerState state : players.values()) {
            if (state.request == null || !state.request.planet.equals(planet)) continue;
            state.pendingPuts.remove(key);
            state.desiredKeys.remove(key);
            if (state.sentRevisions.containsKey(key)) {
                enqueueRemoval(state, planet, key, revision);
            }
            state.refreshPending = true;
        }
    }

    void refreshAll() {
        if (closed) return;
        for (PlayerState state : players.values()) state.refreshPending = true;
    }

    void tick() {
        if (closed) return;
        for (PlayerState state : players.values()) {
            if (state.refreshPending && !state.inFlight) startRefresh(state);
        }

        int globalPackets = 0;
        long globalBytes = 0L;
        for (PlayerState state : players.values()) {
            if (globalPackets >= MAX_PACKETS_GLOBAL_TICK
                    || globalBytes >= MAX_BYTES_GLOBAL_TICK) break;
            ServerPlayer player = server.getPlayerList().getPlayer(state.playerId);
            if (player == null) continue;
            int playerPackets = 0;
            long playerBytes = 0L;
            while (playerPackets < MAX_PACKETS_PER_PLAYER_TICK
                    && globalPackets < MAX_PACKETS_GLOBAL_TICK) {
                QueuedPacket queued = pollFirst(state.pendingRemovals);
                if (queued == null) queued = pollFirst(state.pendingPuts);
                if (queued == null) break;
                if (playerPackets > 0 && (playerBytes + queued.estimatedBytes > MAX_BYTES_PER_PLAYER_TICK
                        || globalBytes + queued.estimatedBytes > MAX_BYTES_GLOBAL_TICK)) {
                    requeueFront(state, queued);
                    break;
                }
                PacketDistributor.sendToPlayer(player, queued.packet);
                state.sentRevisions.put(queued.key, queued.revision);
                playerPackets++;
                globalPackets++;
                playerBytes += queued.estimatedBytes;
                globalBytes += queued.estimatedBytes;
            }
        }
    }

    private void startRefresh(PlayerState state) {
        if (closed || state.inFlight || state.request == null) return;
        InterestRequest request = state.request;
        long token = state.token;
        double halfExtent = cubeHalfExtent;
        int capturedSeaLevel = seaLevel;
        Set<PlanetVoxelBrickKey> previous = Set.copyOf(state.viewportSelection);
        state.inFlight = true;
        state.refreshPending = false;
        try {
            selectorWorker.execute(() -> {
                try {
                    List<PlanetVoxelBrick> resident = store.snapshot(key -> true,
                            MAX_RESIDENT_SNAPSHOT);
                    StreamPlan plan = buildPlan(resident, request, halfExtent,
                            capturedSeaLevel, previous);
                    server.execute(() -> installPlan(state.playerId, token, plan));
                } catch (Throwable error) {
                    server.execute(() -> failRefresh(state.playerId, token, error));
                }
            });
        } catch (RejectedExecutionException rejected) {
            state.inFlight = false;
            state.refreshPending = true;
        }
    }

    private void installPlan(UUID playerId, long token, StreamPlan plan) {
        if (closed) return;
        PlayerState state = players.get(playerId);
        if (state == null) return;
        state.inFlight = false;
        if (state.token != token || state.request == null
                || !state.request.planet.equals(plan.planet)) {
            state.refreshPending = true;
            return;
        }
        state.viewportSelection.clear();
        state.viewportSelection.addAll(plan.viewportSelection);
        predictionRequester.request(plan.predictionRequests);
        state.desiredKeys.clear();
        state.pendingPuts.clear();
        for (PlanetVoxelBrick brick : plan.orderedBricks) {
            state.desiredKeys.add(brick.key());
            enqueuePut(state, plan.planet, brick);
        }
    }

    private void failRefresh(UUID playerId, long token, Throwable error) {
        PlayerState state = players.get(playerId);
        if (state == null) return;
        state.inFlight = false;
        if (state.token == token) state.refreshPending = true;
        GenesisMod.LOGGER.warn("[PLANET-VOXEL] viewport selection failed for {}", playerId, error);
    }

    static StreamPlan buildPlan(List<PlanetVoxelBrick> resident,
                                InterestRequest request,
                                double cubeHalfExtent,
                                int seaLevel,
                                Set<PlanetVoxelBrickKey> previousSelection) {
        if (resident == null || resident.isEmpty()) {
            return new StreamPlan(request.planet, List.of(), Set.of(), Set.of());
        }
        Map<PlanetVoxelBrickKey, PlanetVoxelBrick> byKey = new HashMap<>(resident.size() * 2);
        for (PlanetVoxelBrick brick : resident) {
            if (brick == null || brick.isEmpty()) continue;
            PlanetVoxelBrick old = byKey.get(brick.key());
            if (old == null || old.revision() < brick.revision()) byKey.put(brick.key(), brick);
        }
        List<PlanetVoxelBrick> current = List.copyOf(byKey.values());
        PlanetVoxelLodSelector.OrbitView view = new PlanetVoxelLodSelector.OrbitView(
                request.camera, request.look, cubeHalfExtent, 1.0, seaLevel,
                request.viewportHeight, request.aspectRatio, request.verticalFovRadians,
                request.targetVoxelPixels, PlanetVoxelBrickKey.MAX_LOD,
                request.maximumBricks, true);
        List<PlanetVoxelLodSelector.Selection> selected = PlanetVoxelLodSelector.selectOrbit(
                current, view, previousSelection);

        LinkedHashMap<PlanetVoxelBrickKey, PlanetVoxelBrick> ordered = new LinkedHashMap<>();
        addBaselineRoots(byKey, ordered);
        Set<PlanetVoxelBrickKey> viewport = new LinkedHashSet<>();
        Set<PlanetVoxelBrickKey> predictionRequests = new LinkedHashSet<>();
        int hardLimit = Math.min(MAX_DESIRED_BRICKS,
                Math.max(512, request.maximumBricks + MAX_BASELINE_ROOTS));
        for (PlanetVoxelLodSelector.Selection selection : selected) {
            PlanetVoxelBrick selectedBrick = selection.brick();
            viewport.add(selectedBrick.key());
            collectMissingPredictions(selection, byKey, request.targetVoxelPixels,
                    predictionRequests);
            List<PlanetVoxelBrick> ancestors = new ArrayList<>();
            PlanetVoxelBrickKey cursor = selectedBrick.key();
            while (cursor.lod() < PlanetVoxelBrickKey.MAX_LOD) {
                cursor = cursor.parent();
                PlanetVoxelBrick ancestor = byKey.get(cursor);
                if (ancestor != null) ancestors.add(ancestor);
            }
            for (int index = ancestors.size() - 1; index >= 0; index--) {
                if (ordered.size() >= hardLimit) break;
                PlanetVoxelBrick ancestor = ancestors.get(index);
                ordered.putIfAbsent(ancestor.key(), ancestor);
            }
            if (ordered.size() >= hardLimit) break;
            ordered.putIfAbsent(selectedBrick.key(), selectedBrick);
        }
        return new StreamPlan(request.planet, List.copyOf(ordered.values()),
                Set.copyOf(viewport), Set.copyOf(predictionRequests));
    }

    private static void collectMissingPredictions(
            PlanetVoxelLodSelector.Selection selection,
            Map<PlanetVoxelBrickKey, PlanetVoxelBrick> resident,
            double targetVoxelPixels,
            Set<PlanetVoxelBrickKey> requests) {
        PlanetVoxelBrick parent = selection.brick();
        PlanetVoxelBrickKey parentKey = parent.key();
        if (parentKey.lod() == 0
                || selection.projectedVoxelPixels() <= targetVoxelPixels * 0.82
                || requests.size() >= MAX_PREDICTION_REQUESTS) {
            return;
        }
        int occupiedMask = parent.occupiedOctantMask();
        for (int slot = 0; slot < 8 && requests.size() < MAX_PREDICTION_REQUESTS; slot++) {
            if ((occupiedMask & (1 << slot)) == 0) continue;
            PlanetVoxelBrickKey child = childKey(parentKey, slot);
            if (!resident.containsKey(child)) requests.add(child);
        }
    }

    private static PlanetVoxelBrickKey childKey(PlanetVoxelBrickKey parent, int slot) {
        return new PlanetVoxelBrickKey(parent.face(), parent.lod() - 1,
                parent.brickU() * 2 + (slot & 1),
                parent.brickY() * 2 + ((slot >>> 1) & 1),
                parent.brickV() * 2 + ((slot >>> 2) & 1));
    }

    private static void addBaselineRoots(Map<PlanetVoxelBrickKey, PlanetVoxelBrick> byKey,
                                         LinkedHashMap<PlanetVoxelBrickKey, PlanetVoxelBrick> ordered) {
        Set<PlanetVoxelBrickKey> uniqueRoots = new HashSet<>();
        for (PlanetVoxelBrickKey key : byKey.keySet()) {
            PlanetVoxelBrickKey root = key;
            PlanetVoxelBrickKey cursor = key;
            while (cursor.lod() < PlanetVoxelBrickKey.MAX_LOD) {
                cursor = cursor.parent();
                if (byKey.containsKey(cursor)) root = cursor;
            }
            uniqueRoots.add(root);
        }
        Map<shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face,
                List<PlanetVoxelBrickKey>> byFace = new EnumMap<>(
                shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face.class);
        for (PlanetVoxelBrickKey root : uniqueRoots) {
            byFace.computeIfAbsent(root.face(), ignored -> new ArrayList<>()).add(root);
        }
        Comparator<PlanetVoxelBrickKey> order = Comparator
                .comparingInt(PlanetVoxelBrickKey::lod).reversed()
                .thenComparingInt(PlanetVoxelBrickKey::brickU)
                .thenComparingInt(PlanetVoxelBrickKey::brickV)
                .thenComparingInt(PlanetVoxelBrickKey::brickY);
        int longest = 0;
        for (List<PlanetVoxelBrickKey> roots : byFace.values()) {
            roots.sort(order);
            longest = Math.max(longest, roots.size());
        }
        for (int index = 0; index < longest && ordered.size() < MAX_BASELINE_ROOTS; index++) {
            for (shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face face
                    : shipwrights.genesis.teleportation.CubeNetSurfaceTransform.Face.values()) {
                List<PlanetVoxelBrickKey> roots = byFace.get(face);
                if (roots == null || index >= roots.size()) continue;
                PlanetVoxelBrick root = byKey.get(roots.get(index));
                if (root != null) ordered.putIfAbsent(root.key(), root);
                if (ordered.size() >= MAX_BASELINE_ROOTS) break;
            }
        }
    }

    private static void enqueuePut(PlayerState state, ResourceLocation planet,
                                   PlanetVoxelBrick brick) {
        long sent = state.sentRevisions.getOrDefault(brick.key(), -1L);
        if (sent >= brick.revision()) return;
        QueuedPacket existing = state.pendingPuts.get(brick.key());
        if (existing != null && existing.revision >= brick.revision()) return;
        state.pendingPuts.put(brick.key(), new QueuedPacket(brick.key(), brick.revision(),
                PlanetVoxelBrickPacket.put(planet, brick), estimateBytes(brick), false));
        trim(state.pendingPuts);
    }

    private static void enqueueRemoval(PlayerState state, ResourceLocation planet,
                                       PlanetVoxelBrickKey key, long revision) {
        QueuedPacket existing = state.pendingRemovals.get(key);
        if (existing != null && existing.revision >= revision) return;
        state.pendingRemovals.put(key, new QueuedPacket(key, revision,
                PlanetVoxelBrickPacket.remove(planet, key, revision), 96, true));
        trim(state.pendingRemovals);
    }

    private static int estimateBytes(PlanetVoxelBrick brick) {
        return (int) Math.max(128L, Math.min(128L * 1024L, brick.estimatedBytes()));
    }

    private static void trim(LinkedHashMap<PlanetVoxelBrickKey, QueuedPacket> queue) {
        while (queue.size() > MAX_PENDING_PER_PLAYER) {
            Iterator<PlanetVoxelBrickKey> iterator = queue.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
    }

    private static QueuedPacket pollFirst(
            LinkedHashMap<PlanetVoxelBrickKey, QueuedPacket> queue) {
        Iterator<Map.Entry<PlanetVoxelBrickKey, QueuedPacket>> iterator =
                queue.entrySet().iterator();
        if (!iterator.hasNext()) return null;
        QueuedPacket packet = iterator.next().getValue();
        iterator.remove();
        return packet;
    }

    private static void requeueFront(PlayerState state, QueuedPacket queued) {
        LinkedHashMap<PlanetVoxelBrickKey, QueuedPacket> queue = queued.removal
                ? state.pendingRemovals : state.pendingPuts;
        LinkedHashMap<PlanetVoxelBrickKey, QueuedPacket> copy = new LinkedHashMap<>();
        copy.put(queued.key, queued);
        copy.putAll(queue);
        queue.clear();
        queue.putAll(copy);
    }

    @Override
    public void close() {
        closed = true;
        players.clear();
        selectorWorker.shutdownNow();
    }

    static record InterestRequest(ResourceLocation planet,
                                  Vector3d camera,
                                  Vector3d look,
                                  int viewportHeight,
                                  double aspectRatio,
                                  double verticalFovRadians,
                                  double targetVoxelPixels,
                                  int maximumBricks) {
        InterestRequest {
            camera = new Vector3d(camera);
            look = new Vector3d(look).normalize();
            maximumBricks = Math.max(128, Math.min(6_144, maximumBricks));
            targetVoxelPixels = Math.max(0.5, targetVoxelPixels);
        }

        static InterestRequest from(PlanetVoxelInterestPacket packet) {
            return new InterestRequest(packet.planet(),
                    new Vector3d(packet.cameraX(), packet.cameraY(), packet.cameraZ()),
                    new Vector3d(packet.lookX(), packet.lookY(), packet.lookZ()),
                    packet.viewportHeight(), packet.aspectRatio(), packet.verticalFovRadians(),
                    packet.targetVoxelPixels(), packet.maximumBricks());
        }

        static InterestRequest bootstrap(ResourceLocation planet, double halfExtent) {
            return new InterestRequest(planet, new Vector3d(0.0, halfExtent * 3.0, 0.0),
                    new Vector3d(0.0, -1.0, 0.0), 720, 16.0 / 9.0,
                    Math.toRadians(70.0), 8.0, 512);
        }
    }

    static record StreamPlan(ResourceLocation planet,
                             List<PlanetVoxelBrick> orderedBricks,
                             Set<PlanetVoxelBrickKey> viewportSelection,
                             Set<PlanetVoxelBrickKey> predictionRequests) {
    }

    @FunctionalInterface
    interface PredictionRequester {
        void request(Set<PlanetVoxelBrickKey> keys);
    }

    private static final class PlayerState {
        private final UUID playerId;
        private final LinkedHashMap<PlanetVoxelBrickKey, Long> sentRevisions =
                new LinkedHashMap<>(1024, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<PlanetVoxelBrickKey, Long> eldest) {
                        return size() > MAX_SENT_REVISIONS;
                    }
                };
        private final LinkedHashMap<PlanetVoxelBrickKey, QueuedPacket> pendingPuts =
                new LinkedHashMap<>();
        private final LinkedHashMap<PlanetVoxelBrickKey, QueuedPacket> pendingRemovals =
                new LinkedHashMap<>();
        private final Set<PlanetVoxelBrickKey> desiredKeys = new LinkedHashSet<>();
        private final Set<PlanetVoxelBrickKey> viewportSelection = new LinkedHashSet<>();
        private InterestRequest request;
        private long token;
        private boolean refreshPending;
        private boolean inFlight;

        private PlayerState(UUID playerId) {
            this.playerId = playerId;
        }
    }

    private record QueuedPacket(PlanetVoxelBrickKey key, long revision,
                                PlanetVoxelBrickPacket packet, int estimatedBytes,
                                boolean removal) {
    }
}

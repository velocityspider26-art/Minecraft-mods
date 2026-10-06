package com.vantage.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.vantage.Vantage;
import com.vantage.client.visual.VisualRegistry;
import com.vantage.core.Lod;
import com.vantage.core.SectionKey;
import com.vantage.mesh.Mesher;
import com.vantage.world.LodWorld;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Draws the current {@link Planner.Plan}. Render thread only.
 *
 * <p>Per frame: install finished meshes (time and byte budgeted), frustum-cull the planned
 * sections, write one indirect command per visible face group, and issue one multi-draw per
 * pass. LODs go into the main colour buffer with Vantage's own depth buffer, then their depth
 * is re-projected into vanilla's depth buffer.
 */
public final class LodRenderer implements AutoCloseable {
    private static final int MAX_GROUP_QUADS = Lod.VOLUME;
    private static final int UNIT_LIGHTMAP = 10;
    private static final int UNIT_AUX = 11;
    private static final float NEAR = 0.5f;
    private static final boolean NO_DEPTH_TRANSFER = Boolean.getBoolean("vantage.debug.noDepthTransfer");
    private static final boolean NO_DRAW = Boolean.getBoolean("vantage.debug.noLodDraw");

    public final GlCaps caps;
    private final ShaderProgram program;
    private final ShaderProgram maskedProgram;
    private final ShaderProgram depthProgram;
    private final GeometryArena arena;
    private final int vao;
    private final int indexBuffer;
    private final int instanceBuffer;
    private final int commandBuffer;
    private final int emptyVao;
    private final int visualBuffer;
    private int visualCount;
    private int visualRevision = -1;
    private final int coverageTexture;
    private long coverageVersion = -1;
    private CoverageMap.Snapshot uploadedCoverage = CoverageMap.Snapshot.EMPTY;
    private int fbo;
    private int depthTexture;
    private int fbWidth = -1;
    private int fbHeight = -1;
    private int fbColor = -1;
    private final boolean reverseZ;

    private final IntArrayList instances = new IntArrayList();
    private final IntArrayList visibleEntries = new IntArrayList();
    private final IntArrayList faceMasks = new IntArrayList();
    private final IntArrayList[] commands = {new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList()};
    private final Matrix4f lodProj = new Matrix4f();
    private final Matrix4f viewProj = new Matrix4f();
    private final Matrix4f cullMatrix = new Matrix4f();
    private final Vector3f viewDir = new Vector3f();
    private final FrustumIntersection frustum = new FrustumIntersection();

    // statistics, read by the debug overlay
    public int lastSections;
    public int lastDraws;
    public long lastQuads;
    public double lastCpuMillis;
    /** Time spent choosing and encoding draws on the CPU, excluding the GL calls themselves. */
    public double lastBuildMillis;

    private LodRenderer(GlCaps caps, long maxGeometryBytes) {
        this.caps = caps;
        this.reverseZ = caps.clipControl;
        String common = "#define VANTAGE 1\n" + (Boolean.getBoolean("vantage.debug.levelColors") ? "#define LEVEL_COLORS 1\n" : "");
        this.program = ShaderProgram.load("lod.vsh", "lod.fsh", common);
        this.maskedProgram = ShaderProgram.load("lod.vsh", "lod.fsh", common + "#define MASKED 1\n");
        this.depthProgram = ShaderProgram.load("fullscreen.vsh", "depth_transfer.fsh", common);
        this.arena = new GeometryArena(Math.min(maxGeometryBytes, caps.maxStorageBlockBytes));

        this.indexBuffer = GL15C.glGenBuffers();
        IntBuffer idx = MemoryUtil.memAllocInt(MAX_GROUP_QUADS * 6);
        for (int q = 0; q < MAX_GROUP_QUADS; q++) {
            int b = q * 4;
            idx.put(b).put(b + 1).put(b + 2).put(b + 2).put(b + 3).put(b);
        }
        idx.flip();
        this.instanceBuffer = GL15C.glGenBuffers();
        this.commandBuffer = GL15C.glGenBuffers();

        int prevVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        int prevArray = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);
        this.vao = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(this.vao);
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer);
        GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, idx, GL15C.GL_STATIC_DRAW);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.instanceBuffer);
        GL20C.glEnableVertexAttribArray(0);
        GL30C.glVertexAttribIPointer(0, 4, GL11C.GL_INT, 16, 0L);
        GL33C.glVertexAttribDivisor(0, 1);
        this.emptyVao = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(prevVao);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, prevArray);
        MemoryUtil.memFree(idx);

        this.visualBuffer = GL15C.glGenBuffers();
        this.coverageTexture = GL11C.glGenTextures();
        int prevActive = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_AUX);
        GlStateManager._bindTexture(this.coverageTexture);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(prevActive);
    }

    /** Creates the renderer, or returns null (with a log line) if the GPU cannot run it. */
    public static LodRenderer create(long maxGeometryBytes) {
        GlCaps caps = GlCaps.probe();
        if (!caps.supported) {
            Vantage.LOGGER.warn("Vantage disabled on {}: {}", caps.renderer, caps.reason);
            return null;
        }
        Vantage.LOGGER.info("Vantage renderer on {} (reverse-Z depth: {})", caps.renderer, caps.clipControl);
        return new LodRenderer(caps, maxGeometryBytes);
    }

    public GeometryArena arena() {
        return this.arena;
    }

    public long gpuBytesUsed() {
        return this.arena.usedBytes();
    }

    public long gpuBytesReserved() {
        return this.arena.capacityBytes();
    }

    /** Uploads finished meshes. Call once per frame before {@link #render}. */
    public void upload(MeshManager meshes, long budgetBytes, long planId) {
        meshes.processResults(this.arena, budgetBytes, planId);
    }

    /**
     * Per-frame inputs.
     *
     * @param renderDistance LOD distance in blocks (horizontal)
     * @param fogStart       fraction of it where the edge fade begins
     * @param hazeDensity    haze per block at sea level
     * @param hazeHeight     scale height of the atmosphere in blocks
     * @param seaLevel       height where the air is densest
     * @param bendStart      horizontal distance where planet curvature starts
     * @param curvature      {@code 1 / (2 * planet radius)}, 0 for a flat world
     * @param vanillaFar     vanilla's far plane: it draws nothing beyond it
     * @param vanillaVertical vanilla draws no chunk section more than this many blocks above or
     *                        below the camera's
     * @param hazeColor      haze colour {@code 0xRRGGBB}, or -1 for the game's fog colour
     */
    public record Frame(Matrix4f modelView, Matrix4f projection, double camX, double camY, double camZ,
                        float renderDistance, float fogStart, float hazeDensity, float hazeHeight, float seaLevel,
                        float bendStart, float curvature, float vanillaFar, float vanillaVertical, int hazeColor) {
    }

    public void render(Frame frame, Planner.Plan plan, LodWorld world, VisualRegistry visuals, CoverageMap.Snapshot coverage) {
        if (NO_DRAW) {
            return;
        }
        long t0 = System.nanoTime();
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        this.ensureFramebuffer(main);
        this.updateVisuals(visuals);
        this.updateCoverage(coverage);

        int ax = (int) Math.floor(frame.camX());
        int ay = (int) Math.floor(frame.camY());
        int az = (int) Math.floor(frame.camZ());
        float fx = (float) (frame.camX() - ax);
        float fy = (float) (frame.camY() - ay);
        float fz = (float) (frame.camZ() - az);

        this.buildProjection(frame.projection());
        this.viewProj.set(this.lodProj).mul(frame.modelView());
        frame.modelView().positiveZ(this.viewDir).negate();
        this.cullMatrix.set(frame.projection());
        setDepthRows(this.cullMatrix, 1f, frame.renderDistance() * 2f + 4096f);
        this.cullMatrix.mul(frame.modelView());
        this.frustum.set(this.cullMatrix, false);

        long quads = this.buildCommands(plan, world, ax, ay, az, fx, fy, fz, frame.bendStart(), frame.curvature());
        this.lastBuildMillis = (System.nanoTime() - t0) / 1e6;
        this.lastCpuMillis = this.lastBuildMillis;
        if (this.lastDraws == 0) {
            return;
        }
        this.lastQuads = quads;
        this.draw(frame, main, ax, ay, az, fx, fy, fz);
        this.lastCpuMillis = (System.nanoTime() - t0) / 1e6;
    }

    private void buildProjection(Matrix4f vanilla) {
        this.lodProj.set(vanilla);
        if (this.reverseZ) {
            // Infinite far plane, depth = near / distance: float depth keeps full precision at any range.
            this.lodProj.m02(0f).m12(0f).m22(0f).m32(NEAR);
        } else {
            setDepthRows(this.lodProj, 8f, 1_000_000f);
        }
    }

    /**
     * Replaces the depth row of a perspective matrix (possibly pre-multiplied by view bobbing) so it
     * maps {@code [near, far]} to clip space, leaving x, y and w untouched.
     */
    static void setDepthRows(Matrix4f m, float near, float far) {
        float a = -(far + near) / (far - near);
        float b = -2f * far * near / (far - near);
        float w0 = m.m03(), w1 = m.m13(), w2 = m.m23(), w3 = m.m33();
        // Row 3 is -(view z row), so the view z row is -row3.
        m.m02(-a * w0).m12(-a * w1).m22(-a * w2).m32(-a * w3 + b);
    }

    private long buildCommands(Planner.Plan plan, LodWorld world, int ax, int ay, int az, float fx, float fy, float fz,
                               float bendStart, float curvature) {
        this.instances.clear();
        this.visibleEntries.clear();
        for (IntArrayList c : this.commands) {
            c.clear();
        }
        int n = plan.entries.length;
        for (int i = 0; i < n; i++) {
            MeshManager.Entry e = plan.entries[i];
            if (e.offset < 0) {
                continue;
            }
            long key = e.key;
            int level = SectionKey.level(key);
            int span = Lod.sectionBlocks(level);
            int ox = (int) ((long) SectionKey.x(key) * span - ax);
            int oy = (int) ((long) world.minY + (long) SectionKey.y(key) * span - ay);
            int oz = (int) ((long) SectionKey.z(key) * span - az);
            float mnx = ox + (e.minX << level) - fx, mny = oy + (e.minY << level) - fy, mnz = oz + (e.minZ << level) - fz;
            float mxx = ox + (e.maxX << level) - fx, mxy = oy + (e.maxY << level) - fy, mxz = oz + (e.maxZ << level) - fz;
            if (curvature > 0f) {
                // The vertex shader lowers terrain with distance; move the bounds the same way.
                float nx = Math.max(0f, Math.max(mnx, -mxx)), nz = Math.max(0f, Math.max(mnz, -mxz));
                float far = (float) Math.sqrt(Math.max(mnx * mnx, mxx * mxx) + Math.max(mnz * mnz, mxz * mxz));
                float near = (float) Math.sqrt(nx * nx + nz * nz);
                float bn = Math.max(near - bendStart, 0f), bf = Math.max(far - bendStart, 0f);
                mny -= bf * bf * curvature;
                mxy -= bn * bn * curvature;
            }
            if (!this.frustum.testAab(mnx, mny, mnz, mxx, mxy, mxz)) {
                continue;
            }
            boolean masked = plan.masked[i];
            this.instances.add(ox);
            this.instances.add(oy);
            this.instances.add(oz);
            this.instances.add(level | (masked ? 1 << 8 : 0));
            this.visibleEntries.add(i);
            // Which face directions can face the camera at all, from the section's bounds.
            this.faceMasks.add((mxy > -1f ? 1 : 0) | (mny < 1f ? 2 : 0) | (mxz > -1f ? 4 : 0)
                    | (mnz < 1f ? 8 : 0) | (mxx > -1f ? 16 : 0) | (mnx < 1f ? 32 : 0));
        }
        int sections = this.visibleEntries.size();
        long quads = 0;
        // Opaque front to back (early depth rejection), translucent back to front (blending order).
        for (int s = 0; s < sections; s++) {
            quads += this.emit(plan, s, false);
        }
        for (int s = sections - 1; s >= 0; s--) {
            quads += this.emit(plan, s, true);
        }
        this.faceMasks.clear();
        this.lastSections = sections;
        int draws = 0;
        for (IntArrayList c : this.commands) {
            draws += c.size() / 5;
        }
        this.lastDraws = draws;
        return quads;
    }

    /** Writes the commands of visible section {@code s} for one pass; returns quads emitted. */
    private long emit(Planner.Plan plan, int s, boolean translucent) {
        int i = this.visibleEntries.getInt(s);
        MeshManager.Entry e = plan.entries[i];
        boolean masked = plan.masked[i];
        int faces = this.faceMasks.getInt(s);
        IntArrayList list = this.commands[(translucent ? 2 : 0) + (masked ? 1 : 0)];
        long start = e.offset;
        long quads = 0;
        for (int g = 0; g < Mesher.GROUPS; g++) {
            int count = e.counts[g];
            if (count == 0) {
                continue;
            }
            boolean groupTranslucent = g >= Mesher.TRANSLUCENT_GROUP_OFFSET;
            if (groupTranslucent == translucent && (faces & (1 << (g % 6))) != 0) {
                list.add(count * 6);
                list.add(1);
                list.add(0);
                list.add((int) (start * 4));
                list.add(s);
                quads += count;
            }
            start += count;
        }
        return quads;
    }

    private void draw(Frame frame, RenderTarget main, int ax, int ay, int az, float fx, float fy, float fz) {
        int prevProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        int prevVao = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        int prevArray = GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING);
        int prevDrawFb = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevReadFb = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        int prevActive = GlStateManager._getActiveTexture();

        // Instance data and commands for this frame (buffer orphaning keeps the driver from stalling).
        int[] inst = this.instances.toIntArray();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, this.instanceBuffer);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, (long) inst.length * 4, GL15C.GL_STREAM_DRAW);
        GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, inst);
        int total = 0;
        for (IntArrayList c : this.commands) {
            total += c.size();
        }
        int[] cmds = new int[total];
        int[] starts = new int[4];
        int at = 0;
        for (int i = 0; i < 4; i++) {
            starts[i] = at;
            this.commands[i].getElements(0, cmds, at, this.commands[i].size());
            at += this.commands[i].size();
        }
        GL15C.glBindBuffer(GL40C.GL_DRAW_INDIRECT_BUFFER, this.commandBuffer);
        GL15C.glBufferData(GL40C.GL_DRAW_INDIRECT_BUFFER, (long) cmds.length * 4, GL15C.GL_STREAM_DRAW);
        GL15C.glBufferSubData(GL40C.GL_DRAW_INDIRECT_BUFFER, 0, cmds);

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.fbo);
        if (this.reverseZ) {
            GL45C.glClipControl(GL20C.GL_LOWER_LEFT, GL45C.GL_ZERO_TO_ONE);
        }
        GlStateManager._depthMask(true);
        GlStateManager._colorMask(true, true, true, true);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL30C.glClearBufferfv(GL11C.GL_DEPTH, 0, stack.floats(this.reverseZ ? 0f : 1f));
        }
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(this.reverseZ ? GL11C.GL_GREATER : GL11C.GL_LESS);
        GlStateManager._enableCull();
        GlStateManager._disableBlend();
        GlStateManager._disablePolygonOffset();

        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_LIGHTMAP);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_AUX);
        GlStateManager._bindTexture(this.coverageTexture);

        FogRenderer.levelFogColor();
        float[] fog = RenderSystem.getShaderFogColor();
        if (frame.hazeColor() >= 0) {
            int c = frame.hazeColor();
            fog = new float[]{(c >> 16 & 0xFF) / 255f, (c >> 8 & 0xFF) / 255f, (c & 0xFF) / 255f, 1f};
        }

        GL30C.glBindVertexArray(this.vao);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, this.arena.buffer());
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 1, this.visualBuffer);

        for (int pass = 0; pass < 2; pass++) {
            boolean translucent = pass == 1;
            if (translucent) {
                GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager._depthMask(false);
            }
            for (int m = 0; m < 2; m++) {
                int list = (translucent ? 2 : 0) + m;
                int count = this.commands[list].size() / 5;
                if (count == 0) {
                    continue;
                }
                ShaderProgram p = m == 1 ? this.maskedProgram : this.program;
                GL20C.glUseProgram(p.id);
                this.setUniforms(p, frame, fog, ax, ay, az, fx, fy, fz, translucent ? CoverageMap.LOADED : CoverageMap.BUILT);
                GL43C.glMultiDrawElementsIndirect(GL11C.GL_TRIANGLES, GL11C.GL_UNSIGNED_INT, (long) starts[list] * 4, count, 20);
            }
        }
        GlStateManager._disableBlend();
        GlStateManager._depthMask(true);

        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 0, 0);
        GL30C.glBindBufferBase(GL43C.GL_SHADER_STORAGE_BUFFER, 1, 0);
        GL15C.glBindBuffer(GL40C.GL_DRAW_INDIRECT_BUFFER, 0);
        if (this.reverseZ) {
            GL45C.glClipControl(GL20C.GL_LOWER_LEFT, GL45C.GL_NEGATIVE_ONE_TO_ONE);
        }

        // Write LOD depth into vanilla's depth buffer, in vanilla's projection.
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, main.frameBufferId);
        if (!NO_DEPTH_TRANSFER) {
            this.transferDepth(frame);
        }

        // Restore what Minecraft expects.
        GlStateManager._colorMask(true, true, true, true);
        GlStateManager._depthFunc(GL11C.GL_LEQUAL);
        GlStateManager._enableCull();
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_LIGHTMAP);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(prevActive);
        GL30C.glBindVertexArray(prevVao);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, prevArray);
        GL20C.glUseProgram(prevProgram);
        GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, prevDrawFb);
        GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, prevReadFb);
    }

    /** Re-projects LOD depth into the bound (vanilla) framebuffer's depth buffer. */
    private void transferDepth(Frame frame) {
        GlStateManager._colorMask(false, false, false, false);
        GlStateManager._depthFunc(GL11C.GL_ALWAYS);
        GlStateManager._disableCull();
        GL20C.glUseProgram(this.depthProgram.id);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_AUX);
        GlStateManager._bindTexture(this.depthTexture);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer mat = stack.mallocFloat(16);
            GL20C.glUniform1i(this.depthProgram.uniform("uDepth"), UNIT_AUX);
            GL20C.glUniformMatrix4fv(this.depthProgram.uniform("uInvLodProj"), false, new Matrix4f(this.lodProj).invert().get(mat));
            GL20C.glUniformMatrix4fv(this.depthProgram.uniform("uVanillaProj"), false, frame.projection().get(mat));
            GL20C.glUniform1i(this.depthProgram.uniform("uReverseZ"), this.reverseZ ? 1 : 0);
        }
        GL30C.glBindVertexArray(this.emptyVao);
        GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
    }

    private void setUniforms(ShaderProgram p, Frame frame, float[] fog, int ax, int ay, int az, float fx, float fy, float fz,
                             int coverageBit) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GL20C.glUniformMatrix4fv(p.uniform("uViewProj"), false, this.viewProj.get(stack.mallocFloat(16)));
        }
        GL20C.glUniform3f(p.uniform("uCamFrac"), fx, fy, fz);
        GL20C.glUniform1i(p.uniform("uLightmap"), UNIT_LIGHTMAP);
        GL20C.glUniform4f(p.uniform("uFogColor"), fog[0], fog[1], fog[2], fog[3]);
        GL20C.glUniform4f(p.uniform("uHaze"), frame.hazeDensity(), frame.hazeHeight(), frame.seaLevel(), (float) frame.camY());
        GL20C.glUniform2f(p.uniform("uEdge"), frame.renderDistance() * frame.fogStart(), frame.renderDistance());
        GL20C.glUniform2f(p.uniform("uBend"), frame.bendStart(), frame.curvature());
        GL20C.glUniform1f(p.uniform("uVanillaFar"), frame.vanillaFar());
        GL20C.glUniform2f(p.uniform("uVanillaRows"), (float) (Math.floor(frame.camY() / 16.0) * 16.0), frame.vanillaVertical());
        GL20C.glUniform3f(p.uniform("uViewDir"), this.viewDir.x, this.viewDir.y, this.viewDir.z);
        GL20C.glUniform3i(p.uniform("uAnchor"), ax, ay, az);
        int cov = p.uniform("uCoverage");
        if (cov >= 0) {
            CoverageMap.Snapshot c = this.uploadedCoverage;
            GL20C.glUniform1i(cov, UNIT_AUX);
            GL20C.glUniform4i(p.uniform("uCoverageInfo"), c.originX(), c.originZ(), c.size(), coverageBit);
        }
    }

    private void ensureFramebuffer(RenderTarget main) {
        int color = main.getColorTextureId();
        if (this.fbo != 0 && main.width == this.fbWidth && main.height == this.fbHeight && color == this.fbColor) {
            return;
        }
        if (this.fbo == 0) {
            this.fbo = GL30C.glGenFramebuffers();
        }
        if (this.depthTexture != 0) {
            GlStateManager._deleteTexture(this.depthTexture);
        }
        int prevActive = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_AUX);
        this.depthTexture = GL11C.glGenTextures();
        GlStateManager._bindTexture(this.depthTexture);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL14C.GL_TEXTURE_COMPARE_MODE, GL11C.GL_NONE);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_DEPTH_COMPONENT32F, main.width, main.height, 0,
                GL11C.GL_DEPTH_COMPONENT, GL11C.GL_FLOAT, (ByteBuffer) null);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(prevActive);

        int prevFb = GL11C.glGetInteger(GL30C.GL_FRAMEBUFFER_BINDING);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.fbo);
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, color, 0);
        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL11C.GL_TEXTURE_2D, this.depthTexture, 0);
        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, prevFb);
        if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            Vantage.LOGGER.error("Vantage framebuffer incomplete: 0x{}", Integer.toHexString(status));
        }
        this.fbWidth = main.width;
        this.fbHeight = main.height;
        this.fbColor = color;
    }

    private void updateVisuals(VisualRegistry visuals) {
        int size = visuals.size();
        int revision = visuals.colorRevision();
        if (size == this.visualCount && revision == this.visualRevision) {
            return;
        }
        int[] data = new int[Math.max(1, size) * 8];
        for (int vid = 0; vid < size; vid++) {
            VisualRegistry.Entry e = visuals.entry(vid);
            if (e == null) {
                continue;
            }
            System.arraycopy(e.colors(), 0, data, vid * 8, 6);
            data[vid * 8 + 6] = e.cls();
        }
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, this.visualBuffer);
        GL15C.glBufferData(GL31C.GL_COPY_WRITE_BUFFER, data, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
        this.visualCount = size;
        this.visualRevision = revision;
    }

    private void updateCoverage(CoverageMap.Snapshot coverage) {
        if (coverage.version() == this.coverageVersion) {
            return;
        }
        this.coverageVersion = coverage.version();
        this.uploadedCoverage = coverage;
        int size = Math.max(1, coverage.size());
        ByteBuffer buf = MemoryUtil.memAlloc(size * size);
        if (coverage.size() > 0) {
            buf.put(coverage.covered());
        } else {
            buf.put((byte) 0);
        }
        buf.flip();
        int prevActive = GlStateManager._getActiveTexture();
        int prevAlign = GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT);
        int prevRow = GL11C.glGetInteger(GL12C.GL_UNPACK_ROW_LENGTH);
        int prevSkipPx = GL11C.glGetInteger(GL12C.GL_UNPACK_SKIP_PIXELS);
        int prevSkipRows = GL11C.glGetInteger(GL12C.GL_UNPACK_SKIP_ROWS);
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 1);
        GL11C.glPixelStorei(GL12C.GL_UNPACK_ROW_LENGTH, 0);
        GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_PIXELS, 0);
        GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._activeTexture(GL13C.GL_TEXTURE0 + UNIT_AUX);
        GlStateManager._bindTexture(this.coverageTexture);
        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_R8UI, size, size, 0, GL30C.GL_RED_INTEGER, GL11C.GL_UNSIGNED_BYTE, buf);
        GlStateManager._bindTexture(0);
        GlStateManager._activeTexture(prevActive);
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, prevAlign);
        GL11C.glPixelStorei(GL12C.GL_UNPACK_ROW_LENGTH, prevRow);
        GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_PIXELS, prevSkipPx);
        GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_ROWS, prevSkipRows);
        MemoryUtil.memFree(buf);
    }

    @Override
    public void close() {
        this.program.close();
        this.maskedProgram.close();
        this.depthProgram.close();
        this.arena.close();
        GL30C.glDeleteVertexArrays(this.vao);
        GL30C.glDeleteVertexArrays(this.emptyVao);
        GL15C.glDeleteBuffers(this.indexBuffer);
        GL15C.glDeleteBuffers(this.instanceBuffer);
        GL15C.glDeleteBuffers(this.commandBuffer);
        GL15C.glDeleteBuffers(this.visualBuffer);
        GlStateManager._deleteTexture(this.coverageTexture);
        if (this.depthTexture != 0) {
            GlStateManager._deleteTexture(this.depthTexture);
        }
        if (this.fbo != 0) {
            GL30C.glDeleteFramebuffers(this.fbo);
        }
    }
}

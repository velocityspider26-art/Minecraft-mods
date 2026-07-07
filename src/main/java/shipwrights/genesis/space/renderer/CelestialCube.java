package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * Emits the six faces of a celestial cube. Each vertex is placed at {@code center + rot * localCorner}
 * (view space) and its normalised local corner (0..1 per axis) is handed to the sink — the sun /
 * planet shaders decode that back into the cube-local entry position they ray-march / light with.
 */
final class CelestialCube {

    private CelestialCube() {}

    @FunctionalInterface
    interface ColorVertexSink {
        void accept(BufferBuilder buf, Matrix4f pose, float x, float y, float z, float cr, float cg, float cb);
    }

    // 6 faces × 4 corners, in units of half-size (±1). Winding is irrelevant (cull is disabled).
    private static final int[][][] FACES = {
            {{ 1,-1,-1},{ 1, 1,-1},{ 1, 1, 1},{ 1,-1, 1}}, // +X
            {{-1,-1, 1},{-1, 1, 1},{-1, 1,-1},{-1,-1,-1}}, // -X
            {{-1, 1, 1},{ 1, 1, 1},{ 1, 1,-1},{-1, 1,-1}}, // +Y
            {{-1,-1,-1},{ 1,-1,-1},{ 1,-1, 1},{-1,-1, 1}}, // -Y
            {{-1,-1, 1},{ 1,-1, 1},{ 1, 1, 1},{-1, 1, 1}}, // +Z
            {{ 1,-1,-1},{-1,-1,-1},{-1, 1,-1},{ 1, 1,-1}}, // -Z
    };

    static void emit(BufferBuilder buf, Matrix4f pose, Vector3d center, Quaternionf rot, float half, ColorVertexSink sink) {
        for (int[][] face : FACES) {
            for (int[] c : face) {
                Vector3f local = new Vector3f(c[0] * half, c[1] * half, c[2] * half);
                Vector3f placed = rot.transform(new Vector3f(local));
                float px = (float) center.x + placed.x;
                float py = (float) center.y + placed.y;
                float pz = (float) center.z + placed.z;
                // normalised local corner -> [0,1] (shader: entry = Color*2*half - half)
                float cr = (c[0] + 1) * 0.5f;
                float cg = (c[1] + 1) * 0.5f;
                float cb = (c[2] + 1) * 0.5f;
                sink.accept(buf, pose, px, py, pz, cr, cg, cb);
            }
        }
    }
}

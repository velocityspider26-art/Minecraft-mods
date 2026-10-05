package com.vantage.client.render;

import com.vantage.util.RangeAllocator;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL31C;

/**
 * One GPU buffer holding the quads of every resident LOD mesh, sub-allocated in quad units.
 * Grows on demand up to a cap by copying into a larger buffer. Render thread only.
 */
final class GeometryArena implements AutoCloseable {
    static final int QUAD_BYTES = 8;
    private static final long INITIAL_QUADS = 1L << 21;

    private int buffer;
    private final RangeAllocator allocator;
    private final long maxQuads;

    GeometryArena(long maxBytes) {
        this.maxQuads = Math.max(INITIAL_QUADS, maxBytes / QUAD_BYTES);
        long initial = Math.min(INITIAL_QUADS, this.maxQuads);
        this.allocator = new RangeAllocator(initial);
        this.buffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, this.buffer);
        GL15C.glBufferData(GL31C.GL_COPY_WRITE_BUFFER, initial * QUAD_BYTES, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
    }

    int buffer() {
        return this.buffer;
    }

    long usedBytes() {
        return this.allocator.used() * QUAD_BYTES;
    }

    long capacityBytes() {
        return this.allocator.capacity() * QUAD_BYTES;
    }

    /** Offset in quads, or -1 if full even after growing to the cap. */
    long allocate(int quads) {
        long off = this.allocator.allocate(quads);
        while (off < 0 && this.allocator.capacity() < this.maxQuads) {
            this.grow(Math.min(this.maxQuads, Math.max(this.allocator.capacity() * 2, this.allocator.capacity() + quads)));
            off = this.allocator.allocate(quads);
        }
        return off;
    }

    void free(long offset, int quads) {
        this.allocator.free(offset, quads);
    }

    void upload(long offset, int[] quadInts) {
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, this.buffer);
        GL15C.glBufferSubData(GL31C.GL_COPY_WRITE_BUFFER, offset * QUAD_BYTES, quadInts);
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
    }

    private void grow(long newQuads) {
        long oldBytes = this.allocator.capacity() * QUAD_BYTES;
        int next = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, next);
        GL15C.glBufferData(GL31C.GL_COPY_WRITE_BUFFER, newQuads * QUAD_BYTES, GL15C.GL_DYNAMIC_DRAW);
        GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, this.buffer);
        GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, 0, 0, oldBytes);
        GL15C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, 0);
        GL15C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, 0);
        GL15C.glDeleteBuffers(this.buffer);
        this.buffer = next;
        this.allocator.grow(newQuads);
    }

    @Override
    public void close() {
        GL15C.glDeleteBuffers(this.buffer);
    }
}

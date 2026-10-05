package com.vantage.client.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLCapabilities;

/** OpenGL features Vantage depends on, probed once on the render thread. */
public final class GlCaps {
    public final boolean supported;
    public final boolean clipControl;
    public final long maxStorageBlockBytes;
    public final int maxTextureBufferTexels;
    public final String renderer;
    public final String reason;

    private GlCaps(boolean supported, boolean clipControl, long maxStorageBlockBytes, int maxTextureBufferTexels, String renderer, String reason) {
        this.supported = supported;
        this.clipControl = clipControl;
        this.maxStorageBlockBytes = maxStorageBlockBytes;
        this.maxTextureBufferTexels = maxTextureBufferTexels;
        this.renderer = renderer;
        this.reason = reason;
    }

    public static GlCaps probe() {
        GLCapabilities caps = GL.getCapabilities();
        String renderer = GL11C.glGetString(GL11C.GL_RENDERER) + " / " + GL11C.glGetString(GL11C.GL_VERSION);
        if (!caps.OpenGL43) {
            return new GlCaps(false, false, 0, 0, renderer, "OpenGL 4.3 is required (shader storage buffers and multi-draw-indirect)");
        }
        boolean clip = caps.OpenGL45 || caps.GL_ARB_clip_control;
        long ssbo = Integer.toUnsignedLong(GL11C.glGetInteger(GL43C.GL_MAX_SHADER_STORAGE_BLOCK_SIZE));
        int tbo = GL11C.glGetInteger(GL43C.GL_MAX_TEXTURE_BUFFER_SIZE);
        return new GlCaps(true, clip, ssbo, tbo, renderer, "");
    }
}

package com.vantage.client.render;

import com.vantage.Vantage;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** A linked GLSL program built from shader sources shipped in the mod jar. */
public final class ShaderProgram implements AutoCloseable {
    public final int id;
    private final Map<String, Integer> uniforms = new HashMap<>();

    private ShaderProgram(int id) {
        this.id = id;
    }

    public static ShaderProgram load(String vertex, String fragment, String defines) {
        int vs = compile(GL20C.GL_VERTEX_SHADER, read(vertex), defines, vertex);
        int fs = compile(GL20C.GL_FRAGMENT_SHADER, read(fragment), defines, fragment);
        int program = GL20C.glCreateProgram();
        GL20C.glAttachShader(program, vs);
        GL20C.glAttachShader(program, fs);
        GL20C.glLinkProgram(program);
        GL20C.glDeleteShader(vs);
        GL20C.glDeleteShader(fs);
        if (GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == 0) {
            String log = GL20C.glGetProgramInfoLog(program);
            GL20C.glDeleteProgram(program);
            throw new IllegalStateException("Failed to link " + vertex + " + " + fragment + ": " + log);
        }
        return new ShaderProgram(program);
    }

    private static int compile(int type, String source, String defines, String name) {
        int nl = source.indexOf('\n');
        String patched = source.substring(0, nl + 1) + defines + source.substring(nl + 1);
        int shader = GL20C.glCreateShader(type);
        GL20C.glShaderSource(shader, patched);
        GL20C.glCompileShader(shader);
        if (GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == 0) {
            String log = GL20C.glGetShaderInfoLog(shader);
            GL20C.glDeleteShader(shader);
            throw new IllegalStateException("Failed to compile " + name + ": " + log);
        }
        return shader;
    }

    private static String read(String path) {
        String full = "/assets/" + Vantage.MODID + "/shaders/" + path;
        try (InputStream in = ShaderProgram.class.getResourceAsStream(full)) {
            if (in == null) {
                throw new IllegalStateException("Missing shader " + full);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read shader " + full, e);
        }
    }

    public int uniform(String name) {
        return this.uniforms.computeIfAbsent(name, n -> GL20C.glGetUniformLocation(this.id, n));
    }

    @Override
    public void close() {
        GL20C.glDeleteProgram(this.id);
    }
}

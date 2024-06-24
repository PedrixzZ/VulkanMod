package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public abstract class VRenderSystem {
    private static long window;
    private static final float[] clearColor = new float[4];
    private static final float[] depthBias = new float[2];

    // Pipeline state constants
    public static boolean depthTest = true;
    public static boolean depthMask = true;
    public static int depthFun = 515;
    public static int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    public static int polygonMode = VK_POLYGON_MODE_FILL;
    public static boolean canSetLineWidth = false;
    public static int colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);
    public static boolean cull = true;
    public static boolean logicOp = false;
    public static int logicOpFun = 0;

    // Buffers
    public static final FloatBuffer clearColorBuffer = MemoryUtil.memCallocFloat(4);
    public static final MappedBuffer modelViewMatrix = new MappedBuffer(16 * 4);
    public static final MappedBuffer projectionMatrix = new MappedBuffer(16 * 4);
    public static final MappedBuffer MVP = new MappedBuffer(16 * 4);
    public static final MappedBuffer ChunkOffset = new MappedBuffer(3 * 4);
    public static final MappedBuffer lightDirection0 = new MappedBuffer(3 * 4);
    public static final MappedBuffer lightDirection1 = new MappedBuffer(3 * 4);
    public static final MappedBuffer shaderColor = new MappedBuffer(4 * 4);
    public static final MappedBuffer shaderFogColor = new MappedBuffer(4 * 4);
    public static final MappedBuffer screenSize = new MappedBuffer(2 * 4);

    // Constants
    public static final float clearDepth = 1.0f;
    public static float alphaCutout = 0.0f;

    public static void initRenderer() {
        RenderSystem.assertInInitPhase();
        Vulkan.initVulkan(window);
    }

    public static MappedBuffer getScreenSize() {
        updateScreenSize();
        return screenSize;
    }

    public static void updateScreenSize() {
        Window window = Minecraft.getInstance().getWindow();
        screenSize.putFloat(0, (float) window.getWidth());
        screenSize.putFloat(4, (float) window.getHeight());
    }

    public static void setWindow(long window) {
        VRenderSystem.window = window;
    }

    public static ByteBuffer getChunkOffset() {
        return ChunkOffset.buffer;
    }

    public static int maxSupportedTextureSize() {
        return DeviceManager.deviceProperties.limits().maxImageDimension2D();
    }

    public static void applyMVP(Matrix4f MV, Matrix4f P) {
        MV.get(modelViewMatrix.buffer);
        P.get(projectionMatrix.buffer);
        calculateMVP();
    }

    public static void calculateMVP() {
        Matrix4f MV = new Matrix4f(modelViewMatrix.buffer);
        Matrix4f P = new Matrix4f(projectionMatrix.buffer);
        P.mul(MV).get(MVP.buffer);
    }

    public static void setTextureMatrix(Matrix4f mat) {
        mat.get(TextureMatrix.buffer.asFloatBuffer());
    }

    public static MappedBuffer getTextureMatrix() {
        return TextureMatrix;
    }

    public static MappedBuffer getModelViewMatrix() {
        return modelViewMatrix;
    }

    public static MappedBuffer getProjectionMatrix() {
        return projectionMatrix;
    }

    public static MappedBuffer getMVP() {
        return MVP;
    }

    public static void setChunkOffset(float f1, float f2, float f3) {
        ChunkOffset.putFloat(0, f1);
        ChunkOffset.putFloat(4, f2);
        ChunkOffset.putFloat(8, f3);
    }

    public static void setShaderColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(shaderColor, f1, f2, f3, f4);
    }

    public static void setShaderFogColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(shaderFogColor, f1, f2, f3, f4);
    }

    public static MappedBuffer getShaderColor() {
        return shaderColor;
    }

    public static MappedBuffer getShaderFogColor() {
        return shaderFogColor;
    }

    public static void clearColor(float r, float g, float b, float a) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
        clearColor[3] = a;
    }

    public static void clear(int v) {
        Renderer.clearAttachments(v);
    }

    // Pipeline state methods

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void depthMask(boolean b) {
        depthMask = b;
    }

    public static void setPrimitiveTopologyGL(final int mode) {
        switch (mode) {
            case GL11.GL_LINES, GL11.GL_LINE_STRIP -> topology = VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case GL11.GL_TRIANGLE_FAN, GL11.GL_TRIANGLES, GL11.GL_TRIANGLE_STRIP -> topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            default -> throw new RuntimeException(String.format("Unknown GL primitive topology: %s", mode));
        };
    }

    public static void setPolygonModeGL(final int mode) {
        switch (mode) {
            case GL11.GL_POINT -> polygonMode = VK_POLYGON_MODE_POINT;
            case GL11.GL_LINE -> polygonMode = VK_POLYGON_MODE_LINE;
            case GL11.GL_FILL -> polygonMode = VK_POLYGON_MODE_FILL;
            default -> throw new RuntimeException(String.format("Unknown GL polygon mode: %s", mode));
        };
    }

    public static void setLineWidth(final float width) {
        if (canSetLineWidth) {
            Renderer.setLineWidth(width);
        }
    }

    public static void colorMask(boolean b, boolean b1, boolean b2, boolean b3) {
        colorMask = PipelineState.ColorMask.getColorMask(b, b1, b2, b3);
    }

    public static void enableDepthTest() {
        depthTest = true;
    }

    public static void enableCull() {
        cull = true;
    }

    public static void disableCull() {
        cull = false;
    }

    public static void depthFunc(int depthFun) {
        VRenderSystem.depthFun = depthFun;
    }

    public static void enableBlend() {
        PipelineState.blendInfo.enabled = true;
    }

    public static void disableBlend() {
        PipelineState.blendInfo.enabled = false;
    }

    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        PipelineState.blendInfo.setBlendFunction(sourceFactor, destFactor);
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        PipelineState.blendInfo.setBlendFunction(srcFactor, dstFactor);
    }

    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
        PipelineState.blendInfo.setBlendFuncSeparate(p_69417_, p_69418_, p_69419_, p_69420_);
    }

    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        PipelineState.blendInfo.setBlendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    public static void enableColorLogicOp() {
        logicOp = true;
    }

    public static void disableColorLogicOp() {
        logicOp = false;
    }

    public static void logicOp(GlStateManager.LogicOp logicOp) {
        logicOpFun = logicOp.value;
    }

    public static void polygonOffset(float v, float v1) {
        depthBias[0] = v;
        depthBias[1] = v1;
    }

    public static void enablePolygonOffset() {
        Renderer.setDepthBias(depthBias[0], depthBias[1]);
    }

    public static void disablePolygonOffset() {
        Renderer.setDepthBias(0.0F, 0.0F);
    }
}

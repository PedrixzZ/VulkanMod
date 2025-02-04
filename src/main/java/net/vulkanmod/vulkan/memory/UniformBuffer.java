package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.device.DeviceManager;

import static net.vulkanmod.vulkan.util.VUtil.align;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;

public class UniformBuffer extends Buffer {

    private static final int MIN_OFFSET = (int) DeviceManager.deviceProperties.limits().minUniformBufferOffsetAlignment();

    public static int getAlignedSize(int uploadSize) {
        return align(uploadSize, MIN_OFFSET);
    }

    public UniformBuffer(int size, MemoryType memoryType) {
        super(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, memoryType);
        this.createBuffer(size);
    }

    public void checkCapacity(int size) {
        int requiredCapacity = this.usedBytes + size;
        if (requiredCapacity > this.bufferSize) {
            resizeBuffer(Math.max(this.bufferSize * 2, requiredCapacity));
        }
    }

    public void updateOffset(int alignedSize) {
        usedBytes += alignedSize;
    }

    private void resizeBuffer(int newSize) {
        MemoryManager.getInstance().addToFreeable(this);
        createBuffer(newSize);
    }

    public long getPointer() {
        return this.data.get(0) + usedBytes;
    }
}

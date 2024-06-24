package net.vulkanmod.render.chunk;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.world.level.ChunkPos;

public class ChunkStatusMap {
    public static final byte DATA_READY = 0b1;
    public static final byte LIGHT_READY = 0b10;
    public static final byte NEIGHBOURS_READY = 0b100;
    public static final byte CHUNK_READY = DATA_READY | LIGHT_READY;
    public static final byte ALL_FLAGS = CHUNK_READY | NEIGHBOURS_READY;

    public static ChunkStatusMap INSTANCE;

    public static void createInstance(int renderDistance) {
        INSTANCE = new ChunkStatusMap(renderDistance);
    }

    private final int DIAMETER_FACTOR;
    private final Long2ByteOpenHashMap map;

    public ChunkStatusMap(int renderDistance) {
        int diameter = renderDistance * 2 + 1;
        DIAMETER_FACTOR = diameter * diameter;
        map = new Long2ByteOpenHashMap(DIAMETER_FACTOR);
        map.defaultReturnValue((byte) 0);
    }

    public void updateDistance(int renderDistance) {
        int diameter = renderDistance * 2 + 1;
        DIAMETER_FACTOR = diameter * diameter;
        this.map.ensureCapacity(DIAMETER_FACTOR);
    }

    public void setChunkStatus(int x, int z, byte flag) {
        long l = ChunkPos.asLong(x, z);

        byte current = map.get(l);
        current |= flag;
        map.put(l, current);

        // Verificação otimizada usando diretamente ALL_FLAGS ao invés de CHUNK_READY
        if ((current & ALL_FLAGS) == ALL_FLAGS)
            updateNeighbours(x, z);
    }

    public void resetChunkStatus(int x, int z, byte flag) {
        long l = ChunkPos.asLong(x, z);

        byte current = map.get(l);
        current &= ~flag; // Limpa o bit especificado
        map.put(l, current);

        if (current == 0) {
            map.remove(l);
        }

        updateNeighbours(x, z);
    }

    public void updateNeighbours(int x, int z) {
        for (int x1 = x - 1; x1 <= x + 1; ++x1) {
            for (int z1 = z - 1; z1 <= z + 1; ++z1) {
                if (checkNeighbours(x1, z1)) {
                    map.put(ChunkPos.asLong(x1, z1), ALL_FLAGS);
                } else {
                    long l = ChunkPos.asLong(x1, z1);
                    byte current = map.get(l);
                    byte n = (byte) (current & ~NEIGHBOURS_READY);
                    if (n == 0) {
                        map.remove(l);
                    } else if (current != n) {
                        map.put(l, n);
                    }
                }
            }
        }
    }

    public boolean checkNeighbours(int x, int z) {
        for (int x1 = x - 1; x1 <= x + 1; ++x1) {
            for (int z1 = z - 1; z1 <= z + 1; ++z1) {
                byte status = map.get(ChunkPos.asLong(x1, z1));
                if ((status & CHUNK_READY) != CHUNK_READY) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean chunkRenderReady(int x, int z) {
        byte status = map.get(ChunkPos.asLong(x, z));
        return status == ALL_FLAGS;
    }

    public void reset() {
        map.clear();
    }
}

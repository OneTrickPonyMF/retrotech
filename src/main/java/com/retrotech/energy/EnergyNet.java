package com.retrotech.energy;

import com.retrotech.block.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Red de energía simple al estilo IC2.
 *
 * Cada fuente (generador, BatBox...) busca, recorriendo los cables, todas las máquinas
 * que pueden recibir energía. El resultado se guarda en un caché que se invalida cuando
 * se pone o se saca un cable o una máquina, y se refresca solo cada 5 segundos.
 *
 * - Cada cable tiene una capacidad (EU/t máximo) y una pérdida por bloque.
 * - Si una fuente de un voltaje mayor alimenta a una máquina de un nivel menor, la máquina explota.
 */
public final class EnergyNet {
    private static final int MAX_SEARCH = 4096;
    private static final long REFRESH_TICKS = 100;
    private static int topologyVersion = 0;

    private EnergyNet() {
    }

    /** Llamar cuando cambia algo en la red (se pone/saca un cable o una máquina). */
    public static void invalidate() {
        topologyVersion++;
    }

    public record Target(BlockPos pos, Direction face, int distance, int capacity, double loss) {
    }

    private record Node(BlockPos pos, int distance, int capacity, double loss) {
    }

    /** Caché por fuente. Cada máquina que emite energía tiene uno. */
    public static final class Cache {
        private int version = -1;
        private long builtAt;
        private List<Target> targets = List.of();
        private int rotation;
    }

    /**
     * Reparte energía desde una fuente.
     *
     * @param available  energía disponible en la fuente
     * @param maxPacket  máximo de EU por tick que la fuente puede emitir
     * @param sourceTier voltaje de la fuente
     * @return energía total que salió de la fuente (incluida la perdida en los cables)
     */
    public static int distribute(Level level, BlockPos source, Collection<Direction> outputSides, Cache cache,
                                 int available, int maxPacket, EnergyTier sourceTier) {
        if (available <= 0 || maxPacket <= 0) return 0;

        long now = level.getGameTime();
        if (cache.version != topologyVersion || now - cache.builtAt > REFRESH_TICKS) {
            cache.targets = scan(level, source, outputSides);
            cache.version = topologyVersion;
            cache.builtAt = now;
        }

        List<Target> targets = cache.targets;
        if (targets.isEmpty()) return 0;

        int budget = Math.min(available, maxPacket);
        int consumed = 0;
        int count = targets.size();
        int start = Math.floorMod(cache.rotation++, count); // reparto rotativo, para no favorecer siempre al primero

        for (int i = 0; i < count && budget > 0; i++) {
            Target target = targets.get((start + i) % count);
            BlockEntity be = level.getBlockEntity(target.pos());
            if (be == null) continue;

            IEnergyStorage storage = be.getCapability(ForgeCapabilities.ENERGY, target.face()).orElse(null);
            if (storage == null || !storage.canReceive()) continue;

            int loss = (int) Math.floor(target.loss());
            int packet = Math.min(budget, target.capacity());
            if (packet <= loss) continue;
            int deliver = packet - loss;

            if (be instanceof ITieredEnergy tiered
                    && sourceTier.ordinal() > tiered.getEnergyTier().ordinal()
                    && storage.receiveEnergy(deliver, true) > 0) {
                explode(level, target.pos());
                continue;
            }

            int accepted = storage.receiveEnergy(deliver, false);
            if (accepted > 0) {
                int used = Math.min(budget, accepted + loss);
                consumed += used;
                budget -= used;
            }
        }
        return consumed;
    }

    private static void explode(Level level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.0F, Level.ExplosionInteraction.BLOCK);
    }

    private static List<Target> scan(Level level, BlockPos source, Collection<Direction> outputSides) {
        List<Target> result = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> targetPositions = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        visited.add(source);

        for (Direction side : outputSides) {
            BlockPos next = source.relative(side);
            if (!level.isLoaded(next)) continue;
            BlockState state = level.getBlockState(next);
            if (state.getBlock() instanceof CableBlock cable) {
                if (visited.add(next)) {
                    queue.add(new Node(next, 1, cable.getCapacity(), cable.getLossPerBlock()));
                }
            } else {
                tryAddTarget(level, next, side.getOpposite(), 0, Integer.MAX_VALUE, 0.0, result, targetPositions);
            }
        }

        while (!queue.isEmpty() && visited.size() < MAX_SEARCH) {
            Node node = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = node.pos().relative(dir);
                if (next.equals(source) || !level.isLoaded(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof CableBlock cable) {
                    if (visited.add(next)) {
                        queue.add(new Node(next, node.distance() + 1,
                                Math.min(node.capacity(), cable.getCapacity()),
                                node.loss() + cable.getLossPerBlock()));
                    }
                } else {
                    tryAddTarget(level, next, dir.getOpposite(), node.distance(), node.capacity(), node.loss(),
                            result, targetPositions);
                }
            }
        }
        return result;
    }

    private static void tryAddTarget(Level level, BlockPos pos, Direction face, int distance, int capacity, double loss,
                                     List<Target> result, Set<BlockPos> targetPositions) {
        if (targetPositions.contains(pos)) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;
        IEnergyStorage storage = be.getCapability(ForgeCapabilities.ENERGY, face).orElse(null);
        if (storage != null && storage.canReceive()) {
            targetPositions.add(pos);
            result.add(new Target(pos.immutable(), face, distance, capacity, loss));
        }
    }
}

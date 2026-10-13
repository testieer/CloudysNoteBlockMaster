package net.nayrus.noteblockmaster.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.nayrus.noteblockmaster.network.payload.ScheduleCoreSound;
import net.nayrus.noteblockmaster.network.payload.StopCoreSound;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

public class NetworkUtil {

    public static void broadcastPayload(
            List<ServerPlayer> players,
            @Nullable Player except,
            double x,
            double y,
            double z,
            double radius,
            ResourceKey<Level> dimension,
            CustomPacketPayload payload
    ) {
        for (ServerPlayer serverplayer : players) {
            if (serverplayer != except && serverplayer.level().dimension() == dimension) {
                double d0 = x - serverplayer.getX();
                double d1 = y - serverplayer.getY();
                double d2 = z - serverplayer.getZ();
                if (d0 * d0 + d1 * d1 + d2 * d2 < radius * radius) {
                    PacketDistributor.sendToPlayer(serverplayer, payload);
                }
            }
        }
    }

    public static void broadcastCoreSound(ServerLevel level, ScheduleCoreSound payload){
        BlockPos pos = payload.pos();
        broadcastPayload(level.players(), null, pos.getX(), pos.getY(), pos.getZ(), 64, level.dimension(), payload);
    }

    public static void broadcastCoreSoundStop(ServerLevel level, BlockPos pos){
        broadcastPayload(level.players(), null, pos.getX(), pos.getY(), pos.getZ(), 64, level.dimension(), new StopCoreSound(pos));
    }

}

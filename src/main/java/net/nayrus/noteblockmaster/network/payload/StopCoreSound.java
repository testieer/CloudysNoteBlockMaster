package net.nayrus.noteblockmaster.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nayrus.noteblockmaster.NoteBlockMaster;

public record StopCoreSound(BlockPos pos) implements CustomPacketPayload {

    public static final Type<StopCoreSound> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NoteBlockMaster.MOD_ID, "stopcoresound"));

    @Override
    public Type<StopCoreSound> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, StopCoreSound> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, StopCoreSound::pos,
                    StopCoreSound::new
            );
}

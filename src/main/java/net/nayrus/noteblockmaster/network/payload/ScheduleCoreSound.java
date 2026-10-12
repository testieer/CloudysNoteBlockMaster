package net.nayrus.noteblockmaster.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.nayrus.noteblockmaster.NoteBlockMaster;
import net.nayrus.noteblockmaster.sound.AdvancedInstrument;

public record ScheduleCoreSound(BlockPos pos, float volume, int data) implements CustomPacketPayload {

    public static final Type<ScheduleCoreSound> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoteBlockMaster.MOD_ID, "schedulecoresound"));

    @Override
    public Type<ScheduleCoreSound> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ScheduleCoreSound> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ScheduleCoreSound::pos,
            ByteBufCodecs.FLOAT, ScheduleCoreSound::volume,
            ByteBufCodecs.INT, ScheduleCoreSound::data,
            ScheduleCoreSound::new
    );

    public static ScheduleCoreSound of(
            BlockPos pos,
            int sustainIndex,               // 8 Bit
            int noteVal,                    // 8 Bit
            float volume,
            AdvancedInstrument instrument,  // 5 Bit
            boolean noDecay,                // 1 Bit
            int delay,                       // 5 Bit
            boolean heldByRedstone           // 1 Bit
    ){
        int data = (sustainIndex & 0xFF)
                | ((noteVal & 0xFF) << 8)
                | ((instrument.ordinal() & 0x1F) << 16)
                | ((noDecay ? 1 : 0) << 24)
                | ((delay & 0x1F) << 25)
                | ((heldByRedstone ? 1 : 0) << 30);
        return new ScheduleCoreSound(pos, volume, data);
    }
}

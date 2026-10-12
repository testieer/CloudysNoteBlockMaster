package net.nayrus.noteblockmaster.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.nayrus.noteblockmaster.NoteBlockMaster;
import net.nayrus.noteblockmaster.block.AdvancedNoteBlock;
import net.nayrus.noteblockmaster.block.TuningCore;
import net.nayrus.noteblockmaster.item.TunerItem;
import net.nayrus.noteblockmaster.network.data.ComposeData;
import net.nayrus.noteblockmaster.network.data.TunerData;
import net.nayrus.noteblockmaster.network.payload.*;
import net.nayrus.noteblockmaster.render.ANBInfoRender;
import net.nayrus.noteblockmaster.render.utils.RenderUtils;
import net.nayrus.noteblockmaster.setup.Registry;
import net.nayrus.noteblockmaster.setup.config.ClientConfig;
import net.nayrus.noteblockmaster.setup.config.StartupConfig;
import net.nayrus.noteblockmaster.sound.AdvancedInstrument;
import net.nayrus.noteblockmaster.sound.SubTickScheduler;
import net.nayrus.noteblockmaster.utils.FinalTuple;
import net.nayrus.noteblockmaster.utils.Utils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PacketHandler {
    @SubscribeEvent
    public static void registerNetwork(final RegisterPayloadHandlersEvent event){
        final PayloadRegistrar reg = event.registrar(NoteBlockMaster.MOD_ID);

        reg.playToClient(ConfigCheck.TYPE, ConfigCheck.STREAM_CODEC, PacketHandler::handleStartUpSync);
        reg.playToClient(ActionPing.TYPE, ActionPing.STREAM_CODEC, PacketHandler::handleActionPing);
        reg.playToClient(ScheduleCoreSound.TYPE, ScheduleCoreSound.STREAM_CODEC, PacketHandler::handleScheduleCoreSound);
        reg.playToClient(StopCoreSound.TYPE, StopCoreSound.STREAM_CODEC, PacketHandler::handleStopCoreSound);
        reg.playToClient(SyncBlockInfos.TYPE, SyncBlockInfos.STREAM_CODEC, RenderUtils::handleSyncPacket);
        reg.playToClient(RemoveBlockInfo.TYPE, RemoveBlockInfo.STREAM_CODEC, RenderUtils::handleRemovePacket);

        reg.playToServer(TunerData.TYPE, TunerData.TUNER_STREAM_CODEC, PacketHandler::handleTunerData);
        reg.playToServer(ComposeData.TYPE, ComposeData.STREAM_CODEC, PacketHandler::handleComposeData);
        reg.playToServer(TickSchedule.TYPE, TickSchedule.STREAM_CODEC, PacketHandler::handleTickSchedule);
        reg.playToServer(CoreUpdate.TYPE, CoreUpdate.STREAM_CODEC, PacketHandler::handleCoreUpdate);
        reg.playToServer(RequestBlockInfo.TYPE, RequestBlockInfo.STREAM_CODEC, PacketHandler::handleRequestBlockStates);
    }

    private static void handleStartUpSync(final ConfigCheck packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if(AdvancedNoteBlock.MIN_NOTE_VAL != packet.minNote() || AdvancedNoteBlock.MAX_NOTE_VAL != packet.maxNote()){
                ANBInfoRender.NOTE_OFF_SYNC = true;
                AdvancedNoteBlock.MIN_NOTE_VAL = packet.minNote();
                AdvancedNoteBlock.MAX_NOTE_VAL = packet.maxNote();
                AdvancedNoteBlock.TOTAL_NOTES = AdvancedNoteBlock.MAX_NOTE_VAL - AdvancedNoteBlock.MIN_NOTE_VAL;
            }
            if(AdvancedNoteBlock.SUBTICK_LENGTH != packet.subtickLength()){
                ANBInfoRender.SUBTICK_OFF_SYNC = true;
                AdvancedNoteBlock.SUBTICK_LENGTH = packet.subtickLength();
                AdvancedNoteBlock.SUBTICKS = (byte) (100.0F / AdvancedNoteBlock.SUBTICK_LENGTH);
            }
            if(ANBInfoRender.SUBTICK_OFF_SYNC || ANBInfoRender.NOTE_OFF_SYNC) Utils.sendDesyncWarning(context.player());
        });
    }

    private static void handleActionPing(final ActionPing packet, final IPayloadContext context){
        switch(ActionPing.Action.values()[packet.action()]){
            case SAVE_STARTUP_CONFIG -> {
                if(!StartupConfig.UPDATED) {
                    StartupConfig.updateStartUpAndSave();
                    context.player().sendSystemMessage(Component.translatable("text.config.updated")
                            .withColor(Color.GREEN.darker().getRGB()));
                }
            }
            case ACTIVATE_LOW_RES_RENDER -> {
                if(ClientConfig.LOW_RESOLUTION_RENDER.isFalse()) {
                    ClientConfig.LOW_RESOLUTION_RENDER.set(true);
                    context.player().sendSystemMessage(Component.translatable("text.lowres.enable"));
                    ClientConfig.CLIENT.save();
                }
            }
            case DEACTIVATE_LOW_RES_RENDER -> {
                if(ClientConfig.LOW_RESOLUTION_RENDER.isTrue()){
                    ClientConfig.LOW_RESOLUTION_RENDER.set(false);
                    context.player().sendSystemMessage(Component.translatable("text.lowres.disable"));
                    ClientConfig.CLIENT.save();
                }
            }
        }
    }

    private static void handleStopCoreSound(final StopCoreSound sound, final IPayloadContext context){
        context.enqueueWork(() -> SubTickScheduler.stopCoreSound(sound.pos()));
    }

    private static void handleScheduleCoreSound(final ScheduleCoreSound sound, final IPayloadContext context){
        int data = sound.data();
        int sustainIndex = data & 0xFF;                 // Extract 8 bits (0-7)
        int noteVal = (data >> 8) & 0xFF;               // Extract 8 bits (8-15)
        int instrumentOrdinal = (data >> 16) & 0xFF;    // Extract 8 bits (16-23)
        boolean noDecay = ((data >> 24) & 1) == 1;      // Extract 1 bit (24)
        int delay = (data >> 25) & 0x1F;                // Extract 5 bits (25-29)
        boolean heldByRedstone = ((data >> 30) & 1) == 1; // Extract bit 30

        AdvancedInstrument instrument = AdvancedInstrument.values()[instrumentOrdinal];
        SubTickScheduler.delayedCoredNoteBlockEvent(sound.pos(), sustainIndex, noteVal, sound.volume(), instrument, noDecay, delay, heldByRedstone);
    }

    private static void handleTunerData(final TunerData data, final IPayloadContext context){
        Player player = context.player();
        FinalTuple.ItemStackTuple items = FinalTuple.getHeldItems(player);
        if(!(items.contains(TunerItem.class))) return;
        ItemStack stack = data.isInOffhand() ? items.getB() : items.getA();
        stack.set(Registry.TUNER_DATA, data);
    }

    private static void handleComposeData(final ComposeData data, final IPayloadContext context) {
        Player player = context.player();
        FinalTuple.ItemStackTuple items = FinalTuple.getHeldItems(player);
        if(!(items.contains(Registry.COMPOSER.get()))) return;
        ItemStack stack = items.getFirst(Registry.COMPOSER.get());
        stack.set(Registry.COMPOSE_DATA, data);
    }

    private static void handleTickSchedule(final TickSchedule tickSchedule, final IPayloadContext context) {
        ServerLevel level = (ServerLevel) context.player().level();
        BlockPos pos = tickSchedule.pos();
        Utils.scheduleTick(level, pos, level.getBlockState(pos).getBlock(), tickSchedule.delay());
    }

    private static void handleCoreUpdate(final CoreUpdate coreUpdate, final IPayloadContext context) {
        Level level = context.player().level();
        BlockPos pos = coreUpdate.pos();
        level.setBlockAndUpdate(pos, level.getBlockState(pos)
                .setValue(TuningCore.VOLUME, coreUpdate.volume())
                .setValue(TuningCore.SUSTAIN, coreUpdate.sustain()));
    }

    private static void handleRequestBlockStates(final RequestBlockInfo request, final IPayloadContext context){
        ServerPlayer player = (ServerPlayer) context.player();

        context.enqueueWork(() -> {
            Map<BlockPos, ANBInfoRender.BlockInfo> statesToSend = new HashMap<>();
            ServerLevel level = (ServerLevel) player.level();

            for (BlockPos pos : request.positions()) {
                if (level.isLoaded(pos)) {
                    BlockState state = level.getBlockState(pos);

                    if (state.is(Registry.ADVANCED_NOTEBLOCK)) {
                        statesToSend.put(pos, new ANBInfoRender.BlockInfo(AdvancedNoteBlock.getNoteValue(state), state.getValue(AdvancedNoteBlock.SUBTICK)));
                    }
                }
            }

            if (!statesToSend.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new SyncBlockInfos(statesToSend));
            }
        });
    }
}

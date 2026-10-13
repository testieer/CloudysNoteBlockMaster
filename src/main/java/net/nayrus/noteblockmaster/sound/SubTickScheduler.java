package net.nayrus.noteblockmaster.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.nayrus.noteblockmaster.block.AdvancedNoteBlock;
import net.nayrus.noteblockmaster.block.TuningCore;
import net.nayrus.noteblockmaster.network.NetworkUtil;
import net.nayrus.noteblockmaster.network.payload.ScheduleCoreSound;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.*;

public class SubTickScheduler {

    public static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new SubtickThread());
    public static final HashMap<BlockPos, CoreSound> SUSTAINED_SOUNDS = new HashMap<>();
    public static final ConcurrentHashMap<BlockPos, ScheduledFuture<?>> PENDING_CORE_SOUNDS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<BlockPos, ScheduledFuture<?>> PENDING_RELEASES = new ConcurrentHashMap<>();
    public static final long RELEASE_TAIL_MS = 250L;
    public static final RandomSource RANDOM = RandomSource.create();

    public static void delayedNoteBlockEvent(BlockState state, Level level, BlockPos pos, AdvancedInstrument instrument, float volume){
        executor.schedule(() -> {
            int i = AdvancedNoteBlock.getNoteValue(state);
            level.addParticle(
                    ParticleTypes.NOTE, (double) pos.getX() + 0.5, (double) pos.getY() + 1.2, (double) pos.getZ() + 0.5, (i - 2f) / 29, 0.0, 0.0
            );
            level.playSound(
                    null,
                    pos,
                    instrument.getSoundEvent().value(),
                    SoundSource.RECORDS,
                    volume,
                    AdvancedNoteBlock.getPitchFromNote(i)
            );
        }, (long) state.getValue(AdvancedNoteBlock.SUBTICK) * AdvancedNoteBlock.SUBTICK_LENGTH, TimeUnit.MILLISECONDS);
    }

    public static void delayedCoredNoteBlockEvent(BlockState anb, BlockState core, Level level, BlockPos pos, AdvancedInstrument instrument){
        if(!(level instanceof ServerLevel serverLevel)) return;
        NetworkUtil.broadcastCoreSound(serverLevel, ScheduleCoreSound.of(
                pos,
                TuningCore.getSustain(core),
                AdvancedNoteBlock.getNoteValue(anb),
                (TuningCore.getVolume(core) / 20.0F),
                instrument,
                TuningCore.isMixing(core),
                anb.getValue(AdvancedNoteBlock.SUBTICK),
                anb.getValue(NoteBlock.POWERED)));
    }

    @OnlyIn(Dist.CLIENT)
    public static void delayedCoredNoteBlockEvent(
            BlockPos pos,
            int sustainIndex,
            int noteVal,
            float volume,
            AdvancedInstrument instrument,
            boolean noDecay,
            int delay,
            boolean heldByRedstone
    ){
        CoreSound instance = new CoreSound(
                instrument.getSustainedEvent(sustainIndex),
                SoundSource.RECORDS,
                volume,
                noteVal,
                RandomSource.create(RANDOM.nextLong()),
                pos,
                instrument.getSustainTime(sustainIndex),
                noDecay,
                heldByRedstone && sustainIndex > 0
        );

        ScheduledFuture<?> future = executor.schedule(() -> Minecraft.getInstance().execute(() -> {
            PENDING_CORE_SOUNDS.remove(pos.immutable());
            playSustainingSound(instance);
        }), (long) delay * AdvancedNoteBlock.SUBTICK_LENGTH, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> previous = PENDING_CORE_SOUNDS.put(pos.immutable(), future);
        if(previous != null) previous.cancel(false);
    }

    @OnlyIn(Dist.CLIENT)
    public static void playSustainingSound(CoreSound sound) {
        BlockPos pos = sound.getImmutablePos();
        ScheduledFuture<?> pendingRelease = PENDING_RELEASES.remove(pos);
        if(pendingRelease != null) pendingRelease.cancel(false);
        playbackStop(pos);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    @OnlyIn(Dist.CLIENT)
    public static void stopCoreSound(BlockPos pos) {
        BlockPos immutablePos = pos.immutable();
        ScheduledFuture<?> pending = PENDING_CORE_SOUNDS.remove(immutablePos);
        if(pending != null) pending.cancel(false);

        ScheduledFuture<?> previousRelease = PENDING_RELEASES.remove(immutablePos);
        if(previousRelease != null) previousRelease.cancel(false);

        if(!SUSTAINED_SOUNDS.containsKey(immutablePos)) return;

        ScheduledFuture<?> release = executor.schedule(() -> Minecraft.getInstance().execute(() -> {
            PENDING_RELEASES.remove(immutablePos);
            playbackStop(immutablePos);
        }), RELEASE_TAIL_MS, TimeUnit.MILLISECONDS);
        PENDING_RELEASES.put(immutablePos, release);
    }

    @OnlyIn(Dist.CLIENT)
    public static void playbackStop(BlockPos pos){
        SUSTAINED_SOUNDS.computeIfPresent(pos.immutable(), (p, sound) -> {
            if(sound.getChannel() != null && !sound.getChannel().stopped())
                Minecraft.getInstance().getSoundManager().stop(sound);
            if(sound.getParticle() != null && sound.getParticle().isAlive())
                sound.getParticle().remove();
            return null;
        });
    }

    static class SubtickThread implements ThreadFactory{

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("NBM Subtick Thread");
            return thread;
        }
    }
}

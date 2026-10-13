package net.nayrus.noteblockmaster.sound;

import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.nayrus.noteblockmaster.block.AdvancedNoteBlock;
import net.nayrus.noteblockmaster.render.particle.IInternalParticleAccessor;
import net.nayrus.noteblockmaster.render.particle.SustainedNoteOptions;

import javax.annotation.Nullable;

public class CoreSound extends SimpleSoundInstance {

    private final BlockPos immutablePos;
    private final int sustain;
    private final int noteVal;
    private final boolean mixing;
    private Channel channel;
    private Particle particle;

    public CoreSound(SoundEvent soundEvent, SoundSource source, float volume, int noteVal, RandomSource random, BlockPos pos, int sustain, boolean mixing, boolean heldByRedstone) {
        super(soundEvent, source, mixing ? volume : 3.0F, AdvancedNoteBlock.getPitchFromNote(noteVal), random, pos);
        this.immutablePos = pos.immutable();
        this.sustain = sustain;
        this.noteVal = noteVal;
        this.mixing = mixing;
        this.looping = false;
    }

    public void addNoteParticle(){
        if(!(Minecraft.getInstance().levelRenderer instanceof IInternalParticleAccessor levelRender)) return;
        this.particle = levelRender.nbm$addInternalParticle(
                new SustainedNoteOptions((int) (this.sustain * (1 / getPitch()))),
                false,
                this.getX(),
                this.getY() + 0.7,
                this.getZ(),
                (this.noteVal - 2) / 29.0F,
                0.0,
                0.0
        );
    }

    public BlockPos getImmutablePos() {
        return immutablePos;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public @Nullable Particle getParticle() {
        return this.particle;
    }
}

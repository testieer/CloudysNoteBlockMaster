package net.nayrus.noteblockmaster.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.nayrus.noteblockmaster.NoteBlockMaster;
import net.nayrus.noteblockmaster.sound.SoundRegistry;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;

import java.util.List;
import java.util.function.Supplier;

public class NBMSoundProvider extends SoundDefinitionsProvider {

    protected NBMSoundProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, NoteBlockMaster.MOD_ID, helper);
    }

    @Override
    public void registerSounds() {
        add(SoundRegistry.SMITHING, SoundDefinition.definition()
                .with(sound(ResourceLocation.withDefaultNamespace("block/smithing_table/smithing_table2")))
                .subtitle("sounds.noteblockmaster.noteblock_upgrade")
        );
        addBasicLocalSounds(SoundRegistry.SUSTAINED_HARP_SOUND.getA(), "sustained_harp");
        for(int i = 0; i < SoundRegistry.PIANO_SOUNDS.size(); i++) {
            String sampleName = SoundRegistry.PIANO_SAMPLE_NAMES[i];
            add(SoundRegistry.PIANO_SOUNDS.get(i), SoundDefinition.definition()
                    .with(sound(ResourceLocation.fromNamespaceAndPath(NoteBlockMaster.MOD_ID, "piano/" + sampleName)))
                    .subtitle("sounds.noteblockmaster.piano_" + sampleName));
        }
        addBasicLocalSounds(SoundRegistry.SUSTAINED_BASS_SOUND.getA(), "sustained_bass");
        addBasicLocalSound(SoundRegistry.SUSTAINED_BASEDRUM_SOUND, "sustained_basedrum");
        addBasicLocalSound(SoundRegistry.SUSTAINED_BANJO_SOUND, "sustained_banjo");
        addBasicLocalSounds(SoundRegistry.SUSTAINED_FLUTE_SOUND.getA(), "sustained_flute");
        addBasicLocalSound(SoundRegistry.SUSTAINED_SNARE_SOUND, "sustained_snare");
        addBasicLocalSound(SoundRegistry.SUSTAINED_HAT_SOUND, "sustained_hat");
        addBasicLocalSound(SoundRegistry.SUSTAINED_CHIME_SOUND, "sustained_chime");
        addBasicLocalSound(SoundRegistry.SUSTAINED_COW_BELL_SOUND, "sustained_cowbell");
        addBasicLocalSound(SoundRegistry.SUSTAINED_BIT_SOUND, "sustained_bit");
        addBasicLocalSound(SoundRegistry.SUSTAINED_PLING_SOUND, "sustained_pling");
        addBasicLocalSound(SoundRegistry.SUSTAINED_XYLOPHONE_SOUND, "sustained_xylophone");
        addBasicLocalSound(SoundRegistry.SUSTAINED_IRON_XYLOPHONE_SOUND, "sustained_ironxylophone");
        addBasicLocalSound(SoundRegistry.SUSTAINED_GUITAR_SOUND, "sustained_guitar");
        addBasicLocalSound(SoundRegistry.SUSTAINED_DIDGERIDOO_SOUND, "sustained_didgeridoo");
        addBasicLocalSound(SoundRegistry.SUSTAINED_BELL_SOUND, "sustained_bell");
    }

    private void addBasicLocalSound(Supplier<SoundEvent> sustainedSound, String name){
        add(sustainedSound, SoundDefinition.definition()
                .with(sound(ResourceLocation.fromNamespaceAndPath(NoteBlockMaster.MOD_ID, name)))
                .subtitle("sounds.noteblockmaster.".concat(name))
        );
    }

    private void addBasicLocalSounds(List<Supplier<SoundEvent>> soundList, String name){
        for(int i = 0; i < soundList.size(); i++) addBasicLocalSound(soundList.get(i), name+(i+1));
    }
}

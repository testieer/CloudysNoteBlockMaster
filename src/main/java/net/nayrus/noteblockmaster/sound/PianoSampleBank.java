package net.nayrus.noteblockmaster.sound;

import net.minecraft.sounds.SoundEvent;

public final class PianoSampleBank {

    // Note Block Master's note numbering treats C1 as 0.
    // Salamander samples are spaced in minor thirds, with A0/C8 included
    // so the edge notes stay close to a real recorded key.
    private static final int[] ROOT_NOTES = {
            -3,
            0, 3, 6, 9,
            12, 15, 18, 21,
            24, 27, 30, 33,
            36, 39, 42, 45,
            48, 51, 54, 57,
            60, 63, 66, 69,
            72, 75, 78, 81,
            84
    };

    private PianoSampleBank() {}

    public static Selection select(int noteVal) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < ROOT_NOTES.length; i++) {
            int distance = Math.abs(noteVal - ROOT_NOTES[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        int rootNote = ROOT_NOTES[bestIndex];
        float pitch = (float) Math.pow(2.0, (noteVal - rootNote) / 12.0);
        SoundEvent sound = SoundRegistry.PIANO_SOUNDS.get(bestIndex).get();
        return new Selection(sound, pitch, rootNote);
    }

    public record Selection(SoundEvent sound, float pitch, int rootNote) {}
}

package net.nayrus.noteblockmaster.setup.config;

import net.nayrus.noteblockmaster.block.AdvancedNoteBlock;
import net.nayrus.noteblockmaster.utils.Utils;
import net.neoforged.neoforge.common.ModConfigSpec;

public class StartupConfig
{
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Object> LOWER_NOTE_LIMIT = BUILDER
            .comment(" Lowest key possible.\n Full piano range: \"A0\" through \"C8\". Numeric key indices: 0 ~ 87 (88 keys).")
            .define("minNote", "A0", obj -> isNoteValue(obj, false));

    public static final ModConfigSpec.ConfigValue<Object> HIGHER_NOTE_LIMIT = BUILDER
            .comment(" Highest key possible.\n Full piano range: \"A0\" through \"C8\". Numeric key indices: 0 ~ 87 (88 keys).")
            .define("maxNote", "C8", obj -> isNoteValue(obj, true));

    public static final ModConfigSpec.IntValue SUBTICK_LENGTH = BUILDER
            .comment(" Time in ms per subtick. Amount of subticks equals 2 ticks (100ms) / subtick length")
            .defineInRange("subtickDelay", 5, 5, 25);

    public static final ModConfigSpec START_UP = BUILDER.build();

    public static boolean isNoteValue(Object str, boolean high){
        try{
            int val;
            if(str instanceof String configStr) val = AdvancedNoteBlock.noteStringAsInt(configStr, false);
            else if(str instanceof Integer intVal) val = intVal;
            else return false;

            return Utils.isIntInRange(val, 0, 87);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean UPDATED = false;
    public static void updateStartUpAndSave(){
        StartupConfig.LOWER_NOTE_LIMIT.set(AdvancedNoteBlock.MIN_NOTE_VAL);
        StartupConfig.HIGHER_NOTE_LIMIT.set(AdvancedNoteBlock.MAX_NOTE_VAL);
        StartupConfig.SUBTICK_LENGTH.set(AdvancedNoteBlock.SUBTICK_LENGTH);
        StartupConfig.START_UP.save();
        UPDATED = true;
    }
}

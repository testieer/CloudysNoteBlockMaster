package net.nayrus.noteblockmaster.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.nayrus.noteblockmaster.event.AdvancedNoteBlockEvent;
import net.nayrus.noteblockmaster.item.TunerItem;
import net.nayrus.noteblockmaster.network.data.TunerData;
import net.nayrus.noteblockmaster.render.ANBInfoRender;
import net.nayrus.noteblockmaster.setup.NBMTags;
import net.nayrus.noteblockmaster.setup.Registry;
import net.nayrus.noteblockmaster.setup.config.StartupConfig;
import net.nayrus.noteblockmaster.sound.AdvancedInstrument;
import net.nayrus.noteblockmaster.sound.SubTickScheduler;
import net.nayrus.noteblockmaster.utils.Utils;
import net.neoforged.neoforge.registries.NewRegistryEvent;

import javax.annotation.Nullable;
import java.awt.*;

public class AdvancedNoteBlock extends Block
{

    public static int SUBTICKS;
    public static int SUBTICK_LENGTH;
    public static final EnumProperty<AdvancedInstrument> INSTRUMENT = EnumProperty.create("advanced_instrument", AdvancedInstrument.class);
    public static final IntegerProperty SUBTICK = IntegerProperty.create("subtick",0, 19);
    public static final IntegerProperty NOTE = IntegerProperty.create("note", 0, 87);
    public static int MIN_NOTE_VAL;
    public static int MAX_NOTE_VAL;
    public static int TOTAL_NOTES;
    public static final int DEFAULT_NOTE = noteStringAsInt("F#3", false);

    public AdvancedNoteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(INSTRUMENT, AdvancedInstrument.HARP)
                        .setValue(NoteBlock.POWERED,false)
                        .setValue(SUBTICK, 0)
                        .setValue(NOTE, DEFAULT_NOTE)
        );
    }

    public static void loadPropertiesFromConfig(@Nullable final NewRegistryEvent ignoredEvent){
        SUBTICK_LENGTH = StartupConfig.SUBTICK_LENGTH.get();
        SUBTICKS = 100 / SUBTICK_LENGTH;
        //SUBTICK = IntegerProperty.create("subtick",0, SUBTICKS - 1); Dynmaic Property Settings breaks on dedicated Servers
        MIN_NOTE_VAL = StartupConfig.LOWER_NOTE_LIMIT.get() instanceof String ? noteStringAsInt((String) StartupConfig.LOWER_NOTE_LIMIT.get(), false) : (int) StartupConfig.LOWER_NOTE_LIMIT.get();
        MAX_NOTE_VAL = StartupConfig.HIGHER_NOTE_LIMIT.get() instanceof String ? noteStringAsInt((String) StartupConfig.HIGHER_NOTE_LIMIT.get(), false) : (int) StartupConfig.HIGHER_NOTE_LIMIT.get();
        //NOTE = IntegerProperty.create("note", MIN_NOTE_VAL, MAX_NOTE_VAL); We just have to load the maximum values everytime - we can keep the limtis

        TOTAL_NOTES = MAX_NOTE_VAL - MIN_NOTE_VAL;
    }

    public static void resetPropertiesToLoadedValues(){
        ANBInfoRender.NOTE_OFF_SYNC = false;
        ANBInfoRender.SUBTICK_OFF_SYNC = false;
        SUBTICKS = SUBTICK.getPossibleValues().size();
        SUBTICK_LENGTH = 100 / SUBTICKS;
        MIN_NOTE_VAL = NOTE.getPossibleValues().stream().toList().getFirst();
        MAX_NOTE_VAL = NOTE.getPossibleValues().stream().toList().getLast();
        TOTAL_NOTES = MAX_NOTE_VAL - MIN_NOTE_VAL;
    }

    public BlockState setInstrument(LevelAccessor level, BlockPos pos, BlockState state) {
        NoteBlockInstrument noteblockinstrument = level.getBlockState(pos.below()).instrument();
        AdvancedInstrument instrument = noteblockinstrument.worksAboveNoteBlock() ? AdvancedInstrument.HARP : AdvancedInstrument.values()[noteblockinstrument.ordinal()];
        BlockState above = level.getBlockState(pos.above());
        if(above.is(Registry.TUNINGCORE) && above.getValue(TuningCore.SUSTAIN) > instrument.getSustains()) level.setBlock(pos.above(), above.setValue(TuningCore.SUSTAIN, instrument.getSustains()), 3);
        return state.setValue(INSTRUMENT, instrument);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if(!level.isClientSide()) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new AdvancedNoteBlockEvent.Removed(level, pos, state));
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if(!level.isClientSide()) net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new AdvancedNoteBlockEvent.Placed(level, pos, state));
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if(player.getWeaponItem().is(NBMTags.Items.TUNERS)){
            if(!level.isClientSide())
                attack(state, level, pos, player);
            return false;
        }
        if(!level.isClientSide() && level.getBlockState(pos.above()).getBlock() instanceof TuningCore core)
            Utils.scheduleTick((ServerLevel) level, pos.above(), core, 0);
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected void spawnDestroyParticles(Level level, Player player, BlockPos pos, BlockState state) {
        if(!player.getWeaponItem().is(NBMTags.Items.TUNERS)) super.spawnDestroyParticles(level, player, pos, state);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return player.getWeaponItem().is(NBMTags.Items.TUNERS) ? 0.0f : super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if(!stack.is(NBMTags.Items.TUNERS) && !stack.is(Registry.COMPOSER) && !stack.is(NBMTags.Items.CORES))
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            int val = getNoteValue(state);
            int _new = ((val+1) >= (DEFAULT_NOTE + 25) || (val+1) < DEFAULT_NOTE) ? DEFAULT_NOTE : val+1;
            return onNoteChange(level, player, state, pos, _new);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean flag = level.hasNeighborSignal(pos);
        if (flag != state.getValue(NoteBlock.POWERED)) {
            if (flag) {
                this.playNote(null, level, pos);
            }

            level.setBlockAndUpdate(pos, state.setValue(NoteBlock.POWERED, flag));
        }
    }

    public void playNote(@Nullable Entity entity, Level level, BlockPos pos) {
        BlockState stateAbove = level.getBlockState(pos.above());
        boolean isTuned = stateAbove.is(Registry.TUNINGCORE);
        if (!((isTuned || stateAbove.isAir()))) return;
        level.blockEvent(pos, this, isTuned ? 1 : 0, 0);
        level.gameEvent(entity, GameEvent.NOTE_BLOCK_PLAY, pos);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            ItemStack item = player.getWeaponItem();
            if(!item.is(NBMTags.Items.TUNERS)) {
                this.playNote(player, level, pos);
                player.awardStat(Stats.PLAY_NOTEBLOCK);
            }
            else{
                TunerData data = TunerItem.getTunerData(item);
                if(item.is(Registry.NOTETUNER)){
                    if(!player.isShiftKeyDown()) {
                        player.displayClientMessage(Component.literal(Utils.NOTE_STRING[getNoteValue(state)])
                                .withColor(getColor(state, Utils.PROPERTY.NOTE).getRGB()), true);
                        this.playNote(player, level, pos);
                        player.awardStat(Stats.PLAY_NOTEBLOCK);
                    }
                    else{
                        int new_val = data.isSetmode() ? data.value() + MIN_NOTE_VAL : this.changeNoteValueBy(state, -data.value());
                        this.onNoteChange(level, player, state, pos, new_val);
                    }
                }
                if(item.is(Registry.TEMPOTUNER)){
                    if(!player.isShiftKeyDown())
                        player.displayClientMessage(Component.literal( "("+state.getValue(SUBTICK) * SUBTICK_LENGTH+" ms)")
                            .withColor(AdvancedNoteBlock.getColor(state, Utils.PROPERTY.TEMPO).getRGB()), true);
                    else{
                        int new_val;
                        if(data.isSetmode()) new_val = data.value();
                        else{
                            int diff = state.getValue(SUBTICK) - data.value();
                            new_val = diff < 0 ? (diff + SUBTICKS) : diff;
                        }
                        this.onSubtickChange(level, player, state, pos, new_val, false);
                    }
                }
            }
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.setInstrument(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        boolean flag = facing.getAxis() == Direction.Axis.Y;
        return flag ? this.setInstrument(level, currentPos, state) : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INSTRUMENT, NoteBlock.POWERED, SUBTICK, NOTE);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        AdvancedNoteBlockEvent.Play e = new AdvancedNoteBlockEvent.Play(level, pos, state, getNoteValue(state), state.getValue(INSTRUMENT));
        if (net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(e).isCanceled()) return false;
        AdvancedInstrument instrument = e.getInstrument();
        if(id == 0) SubTickScheduler.delayedNoteBlockEvent(state, level, pos, instrument, 3.0F);
        if(id >= 1) SubTickScheduler.delayedCoredNoteBlockEvent(state, level.getBlockState(pos.above()), level, pos, instrument);
        return true;
    }

    public static int noteStringAsInt(String note, boolean validate){
        try{
            int num = Integer.parseInt(note);
            if(validate && !Utils.isIntInRange(num, MIN_NOTE_VAL, MAX_NOTE_VAL))
                throw new IllegalArgumentException("Note " + note + " is out of limited range " + MIN_NOTE_VAL + " ~ " + MAX_NOTE_VAL);
            if(num < 0 || num >= Utils.NOTE_STRING.length)
                throw new IllegalArgumentException("Note " + note + " is out of 88-key piano range");
            return num;
        } catch (NumberFormatException e) {
            String normalized = note.toUpperCase().replace('♯', '#').trim();
            if(normalized.length() < 2 || normalized.length() > 3)
                throw new IllegalArgumentException("Unexpected note format: " + note);

            char key = normalized.charAt(0);
            int semitone = switch (key) {
                case 'C' -> 0;
                case 'D' -> 2;
                case 'E' -> 4;
                case 'F' -> 5;
                case 'G' -> 7;
                case 'A' -> 9;
                case 'B' -> 11;
                default -> throw new IllegalArgumentException("Unknown key '" + key + "'");
            };

            int octaveIndex = normalized.length() - 1;
            if(normalized.length() == 3){
                if(normalized.charAt(1) != '#')
                    throw new IllegalArgumentException("Unexpected note format: " + note);
                semitone++;
            }

            char octaveChar = normalized.charAt(octaveIndex);
            if(octaveChar < '0' || octaveChar > '8')
                throw new IllegalArgumentException("Unexpected octave in note: " + note);
            int octave = octaveChar - '0';

            int midi = (octave + 1) * 12 + (semitone % 12);
            int val = midi - 21; // A0 = MIDI 21 = index 0

            if(val < 0 || val >= Utils.NOTE_STRING.length)
                throw new IllegalArgumentException("Note " + note + " is out of 88-key piano range");
            if(validate && !Utils.isIntInRange(val, MIN_NOTE_VAL, MAX_NOTE_VAL))
                throw new IllegalArgumentException("Note " + note + " is out of limited range " + MIN_NOTE_VAL + " ~ " + MAX_NOTE_VAL);
            return val;
        }
    }

    public static int noteStringAsInt(String note){
        return noteStringAsInt(note, true);
    }

    public static int getNoteValue(BlockState state){
        return state.getValue(NOTE);
    }

    public BlockState setNoteValue(BlockState state, int value){
        value %= Utils.NOTE_STRING.length;
        return state.setValue(NOTE, value);
    }

    public static float getPitchFromNote(int note) {
        return (float)Math.pow(2.0, (double)(note - 45) / 12.0);
    }

    public static Color getColor(int noteVal, int tickVal, Utils.PROPERTY info){
        float rgbVal = switch (info){
            case NOTE -> (noteVal- 2) / 29.0F;
            case TEMPO -> tickVal / (SUBTICKS - 1.0F);
        };

        float rCol = Math.max(0.0F, Mth.sin((rgbVal + 0.0F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
        float gCol = Math.max(0.0F, Mth.sin((rgbVal + 0.33333334F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
        float bCol = Math.max(0.0F, Mth.sin((rgbVal + 0.6666667F) * (float) (Math.PI * 2)) * 0.65F + 0.35F);
        return new Color(rCol, gCol, bCol);
    }

    public static Color getColor(BlockState state, Utils.PROPERTY info){
        return getColor(getNoteValue(state), state.getValue(SUBTICK), info);
    }

    public int changeNoteValueBy(BlockState state, int value){
        int range = MAX_NOTE_VAL - MIN_NOTE_VAL + 1;
        int current = getNoteValue(state) - MIN_NOTE_VAL;
        return MIN_NOTE_VAL + Math.floorMod(current + value, range);
    }

    public InteractionResult onNoteChange(Level level, Player player, BlockState state, BlockPos pos, int new_val){
        int old_val = AdvancedNoteBlock.getNoteValue(state);
        int _new = AdvancedNoteBlockEvent.onNoteChange(level, pos, state, old_val, new_val);
        if (_new == -1) return InteractionResult.FAIL;
        state = this.setNoteValue(state, _new);
        level.setBlockAndUpdate(pos, state);
        this.playNote(player, level, pos);
        player.awardStat(Stats.TUNE_NOTEBLOCK);
        return InteractionResult.SUCCESS;
    }

    public InteractionResult onSubtickChange(Level level, Player player, BlockState state, BlockPos pos, int new_val, boolean add){
        int old_val = state.getValue(AdvancedNoteBlock.SUBTICK);
        int _new = AdvancedNoteBlockEvent.onSubtickChange(level, pos, state, old_val, new_val);
        if (_new == -1) return InteractionResult.FAIL;
        state = state.setValue(AdvancedNoteBlock.SUBTICK, new_val);
        level.setBlockAndUpdate(pos, state);
        level.playSound(null,
                pos,
                add ? SoundEvents.WOODEN_BUTTON_CLICK_ON : SoundEvents.WOODEN_BUTTON_CLICK_OFF,
                SoundSource.RECORDS,
                1.0F,
                1.0F);
        player.displayClientMessage(Component.literal( "("+new_val * AdvancedNoteBlock.SUBTICK_LENGTH+" ms)")
                .withColor(AdvancedNoteBlock.getColor(state, Utils.PROPERTY.TEMPO).getRGB()), true);
        return InteractionResult.SUCCESS;
    }

}

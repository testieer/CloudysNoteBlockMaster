package net.nayrus.noteblockmaster.screen.base;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.nayrus.noteblockmaster.NoteBlockMaster;
import net.nayrus.noteblockmaster.block.TuningCore;
import net.nayrus.noteblockmaster.network.payload.CoreUpdate;
import net.nayrus.noteblockmaster.screen.widget.IntegerEditBox;
import net.nayrus.noteblockmaster.screen.widget.ValueSlider;
import net.neoforged.neoforge.network.PacketDistributor;

public class BaseCoreScreen extends BaseScreen {

    protected IntegerEditBox volBox;
    protected IntegerEditBox sustainBox;
    protected ValueSlider volSlider;
    protected ValueSlider sustainSlider;
    protected int volume = -1;
    protected int sustain = -1;
    private final BlockPos pos;

    protected BaseCoreScreen(BlockState state, BlockPos pos) {
        super(ResourceLocation.fromNamespaceAndPath(NoteBlockMaster.MOD_ID, "textures/gui/tunerscreen.png"), 176, 31);
        if(TuningCore.isMixing(state)) this.volume = TuningCore.getVolume(state);
        if(TuningCore.isSustaining(state)) this.sustain = TuningCore.getSustain(state);
        this.pos = pos;
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new CoreUpdate(this.pos,
                this.sustain != -1 ? this.sustain : 0,
                this.volume != -1 ? this.volume : 0));
        super.onClose();
    }
}

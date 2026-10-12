package net.nayrus.noteblockmaster.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.nayrus.noteblockmaster.screen.base.BaseCoreScreen;
import net.nayrus.noteblockmaster.screen.widget.IntegerEditBox;
import net.nayrus.noteblockmaster.screen.widget.ValueSlider;

public class CoreScreen extends BaseCoreScreen {

    private static final int COMP_HEIGHT = 19;

    public CoreScreen(BlockState state, BlockPos pos, net.nayrus.noteblockmaster.sound.AdvancedInstrument instrument, float pitch) {
        super(state, pos);
    }

    @Override
    protected void init() {
        super.init();

        boolean isMixing = this.volume != -1;

        this.volBox = new IntegerEditBox(this.font, getRelX() + 138, getRelY() + 6, 27, COMP_HEIGHT, 100, true);
        this.volBox.setEditable(isMixing);
        this.volBox.setValue(isMixing ? Integer.toString(this.volume * 5) : "100");
        this.volBox.setMaxLength(3);
        this.volBox.setResponder(s -> {
            if(!s.isEmpty()) {
                int _new = Integer.parseInt(s) / 5;
                if(_new < 1) _new = 1;
                this.volume = _new;
                this.volSlider.setValue((_new - 1) / 19.0);
            }
        });

        this.volBox.setFocused(false);

        this.volSlider = new ValueSlider(getRelX() + 10, getRelY() + 6, 118, COMP_HEIGHT,
                isMixing ? (this.volume - 1) / 19.0 : 1.0, !isMixing ? null : (val) -> {
            this.volume = (int) (val * 19 + 1);
            this.volBox.setValue(Integer.toString(this.volume * 5));
        });
        this.volSlider.active = isMixing;
        this.volSlider.setMessage(Component.literal("Volume"));

        addRenderableWidget(this.volBox);
        addRenderableWidget(this.volSlider);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Options op = Minecraft.getInstance().options;
        if(op.keyUp.matches(keyCode, scanCode)
                || op.keyDown.matches(keyCode, scanCode)
                || op.keyRight.matches(keyCode, scanCode)
                || op.keyLeft.matches(keyCode, scanCode)
                || op.keyShift.matches(keyCode, scanCode)
                || op.keyJump.matches(keyCode, scanCode))
            this.onClose();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

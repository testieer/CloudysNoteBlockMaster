package net.nayrus.noteblockmaster.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Arrays;

public class Utils {

    public static final String[] NOTE_STRING = {"A0", "A♯0", "B0", "C1", "C♯1", "D1", "D♯1", "E1", "F1", "F♯1", "G1", "G♯1", "A1", "A♯1", "B1", "C2", "C♯2", "D2", "D♯2", "E2", "F2", "F♯2", "G2", "G♯2", "A2", "A♯2", "B2", "C3", "C♯3", "D3", "D♯3", "E3", "F3", "F♯3", "G3", "G♯3", "A3", "A♯3", "B3", "C4", "C♯4", "D4", "D♯4", "E4", "F4", "F♯4", "G4", "G♯4", "A4", "A♯4", "B4", "C5", "C♯5", "D5", "D♯5", "E5", "F5", "F♯5", "G5", "G♯5", "A5", "A♯5", "B5", "C6", "C♯6", "D6", "D♯6", "E6", "F6", "F♯6", "G6", "G♯6", "A6", "A♯6", "B6", "C7", "C♯7", "D7", "D♯7", "E7", "F7", "F♯7", "G7", "G♯7", "A7", "A♯7", "B7", "C8"};
    public static final float PI = (float) Math.PI;

    public static boolean isPartOfNoteString(String s){
        String t = s.replace('#','♯').toUpperCase();
        return Arrays.stream(NOTE_STRING).anyMatch(r -> r.contains(t));
    }

    public static boolean isIntInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    public static Vec3 sphereVec(float radius){
        return new Vec3(radius, radius, radius);
    }

    public enum PROPERTY {NOTE, TEMPO}

    public static void sendDesyncWarning(Player player){
        player.sendSystemMessage(Component.translatable("text.config.desync_info")
                .withColor(Color.GREEN.getRGB())
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nbm saveconfig"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("text.config.sync_save_hovertext").withColor(Color.LIGHT_GRAY.getRGB())))));
    }

    public static void removeItemsFromInventory(Inventory inv, Item item, int amount){
        if(amount<=0) return;
        int removed = 0;
        for (int i = inv.getContainerSize() - 1; i >=0 ; i--) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack.getItem().equals(item)) {
                removed += inv.removeItem(i, amount - removed).getCount();
                if(removed == amount) break;
            }
        }
    }

    public static void playFailUse(Level level, Player player, BlockPos pos){
        level.playSound(player, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    public static float exponentialFloor(float start, float max, float current, float power){
        return Math.max(0.05F, start - (float)((Math.pow(current, power) * start) / Math.pow(max, power)));
    }

    public static float getRotationToX(Vec3 vec){
        Vec3 vecXZ = vec.subtract(0, vec.y(), 0).normalize();
        double dotX = vecXZ.dot(new Vec3(1,0,0));
        double dotZ = vecXZ.dot(new Vec3(0,0,1));
        return (float) (Math.acos(dotX) * (dotZ < 0 ? -1 : 1));
    }

    public static void scheduleTick(ServerLevel level, BlockPos pos, Block block, int delay){
        if(!level.getBlockTicks().hasScheduledTick(pos, block))
            level.scheduleTick(pos, block, delay);
    }

    public static InteractionResult swingHelper(Player player, InteractionHand hand, boolean client){
        if(hand.equals(InteractionHand.MAIN_HAND)) return InteractionResult.SUCCESS;
        if(client) player.swing(hand); return  InteractionResult.CONSUME;
    }

    public static float normalizeAngle(float angle) {
        return (angle % (2 * (float) Math.PI) + (2 * (float) Math.PI)) % (2 * (float) Math.PI);
    }
}

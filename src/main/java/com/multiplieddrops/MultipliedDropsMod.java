package com.multiplieddrops;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class MultipliedDropsMod implements ModInitializer {

    public static boolean enabled = true; // ON by default
    public static int multiplier = 1;
    public static int pendingMultiplier = 0;

    @Override
    public void onInitialize() {
        System.out.println("Multiplied Drops loaded!");

        // ===== COMMANDS =====
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("md")
                            .then(literal("on")
                                    .executes(ctx -> {
                                        enabled = true;
                                        ctx.getSource().sendFeedback(() -> Text.of("Multiplied Drops enabled!"), false);
                                        return 1;
                                    }))
                            .then(literal("off")
                                    .executes(ctx -> {
                                        enabled = false;
                                        ctx.getSource().sendFeedback(() -> Text.of("Multiplied Drops disabled!"), false);
                                        return 1;
                                    }))
                            .then(literal("reset")
                                    .executes(ctx -> {
                                        multiplier = 1;
                                        ctx.getSource().sendFeedback(() -> Text.of("Multiplier reset to 1x!"), false);
                                        return 1;
                                    }))
                            .then(literal("confirm")
                                    .executes(ctx -> {
                                        if (pendingMultiplier > 0) {
                                            multiplier = pendingMultiplier;
                                            pendingMultiplier = 0;
                                            ctx.getSource().sendFeedback(() -> Text.of("Multiplier set to high value!"), false);
                                        } else {
                                            ctx.getSource().sendFeedback(() -> Text.of("No pending multiplier to confirm."), false);
                                        }
                                        return 1;
                                    }))
                            .then(argument("value", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        int value = IntegerArgumentType.getInteger(ctx, "value");
                                        if (value > 1280) {
                                            pendingMultiplier = value;
                                            ctx.getSource().sendFeedback(() -> Text.of(
                                                    "Warning: drop multipliers this high may lag servers/computers! " +
                                                            "Type /md confirm to proceed."
                                            ), false);
                                        } else {
                                            multiplier = value;
                                            ctx.getSource().sendFeedback(() -> Text.of("Multiplier set to " + value + "x!"), false);
                                        }
                                        return 1;
                                    }))
            );
        });

        // ===== MULTIPLIED DROPS ON BLOCK BREAK =====
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!enabled) return true; // no client check needed here for server

            ItemStack drop = new ItemStack(state.getBlock(), multiplier);
            state.getBlock().dropStack(world, pos, drop);

            // remove the block so it doesn’t stay there
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);

            return false; // cancel vanilla drop
        });
    }
}

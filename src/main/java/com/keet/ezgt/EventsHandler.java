package com.keet.ezgt;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.ItemList;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTRecipe;

public class EventsHandler {

    private boolean modifiedRecipes = false;

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        modifyRecipesOnce("client tick");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        modifyRecipesOnce("server tick");
    }

    private void modifyRecipesOnce(String source) {
        if (modifiedRecipes) return;
        modifiedRecipes = true;
        EZGT.LOG.info("Applying configured EZGT recipe updates from {}", source);
        modifyCasingRecipes();
        modifyCasingCraftingRecipes();
        modifyCircuitRecipes();
        modifyMotorRecipes();

        // TEMP DEBUG
        RecipeMap<?> asl = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.fakeAssemblylineProcess");
        if (asl != null) {
            EZGT.LOG.info(
                "ASL recipe map found, recipe count: {}",
                asl.getAllRecipes()
                    .size());
            asl.getAllRecipes()
                .stream()
                .findFirst()
                .ifPresent(r -> {
                    EZGT.LOG.info(
                        "ASL first recipe class: {}",
                        r.getClass()
                            .getName());
                    if (r.mOutputs != null && r.mOutputs.length > 0 && r.mOutputs[0] != null)
                        EZGT.LOG.info("ASL first recipe output: {}", r.mOutputs[0].getDisplayName());
                });
        }
    }

    private void modifyCasingRecipes() {
        if (ModConfig.Rates.gtCasingAssemblerOutputs == 1.0f) return;

        RecipeMap<?> assembler = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.assembler");
        if (assembler == null) return;

        for (GTRecipe recipe : assembler.getAllRecipes()) {
            if (recipe.mOutputs == null || recipe.mOutputs.length == 0) continue;
            ItemStack output = recipe.mOutputs[0];
            if (output == null) continue;
            if (isGtCasingOutput(output)) {
                output.stackSize = Math.min(
                    Math.max(1, (int) (output.stackSize * ModConfig.Rates.gtCasingAssemblerOutputs)),
                    output.getMaxStackSize());
            }
        }
    }

    private boolean isGtCasingOutput(ItemStack output) {
        for (int id : OreDictionary.getOreIDs(output)) {
            String oreName = OreDictionary.getOreName(id);
            if (isBlockCasingsName(oreName)) return true;
        }
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(output.getItem());
        return identifier != null && isBlockCasingsName(identifier.toString());
    }

    private boolean isBlockCasingsName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("blockcasings5")) {
            return ModConfig.Rates.easyCoils;
        }
        return lower.contains("blockcasings");
    }

    private void modifyCasingCraftingRecipes() {
        if (ModConfig.Rates.gtCasingAssemblerOutputs == 1.0f) return;

        List<?> recipes = net.minecraft.item.crafting.CraftingManager.getInstance()
            .getRecipeList();
        int count = 0;

        for (Object obj : recipes) {
            if (!(obj instanceof net.minecraft.item.crafting.IRecipe)) continue;
            net.minecraft.item.crafting.IRecipe recipe = (net.minecraft.item.crafting.IRecipe) obj;
            ItemStack output = recipe.getRecipeOutput();
            if (output == null) continue;
            if (isGtCasingOutput(output)) {
                output.stackSize = Math.min(
                    Math.max(1, (int) (output.stackSize * ModConfig.Rates.gtCasingAssemblerOutputs)),
                    output.getMaxStackSize());
                count++;
            }
        }

        EZGT.LOG.info("Modified {} GT casing crafting recipes", count);
    }

    private void modifyCircuitRecipes() {
        if (!ModConfig.Rates.easyCircuits) return;

        RecipeMap<?> circuitAssembler = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.circuitassembler");
        RecipeMap<?> cal = RecipeMap.ALL_RECIPE_MAPS.get("bw.recipe.cal");
        if (circuitAssembler == null && cal == null) return;

        ItemStack ic2Basic = GTModHandler.getIC2Item("electronicCircuit", 1L);

        ItemStack[] processorOutputs = { ic2Basic, ItemList.Circuit_Basic.get(1L),
            ItemList.Circuit_Microprocessor.get(2L), ItemList.Circuit_Processor.get(1L),
            ItemList.Circuit_Nanoprocessor.get(1L), ItemList.Circuit_Quantumprocessor.get(1L),
            ItemList.Circuit_Crystalprocessor.get(1), ItemList.Circuit_Neuroprocessor.get(1) };

        ItemStack[] assemblyOutputs = { ItemList.Circuit_Integrated_Good.get(1L), ItemList.Circuit_Computer.get(1L),
            ItemList.Circuit_Nanocomputer.get(1L), ItemList.Circuit_Quantumcomputer.get(1L),
            ItemList.Circuit_Crystalcomputer.get(1), ItemList.Circuit_Wetwarecomputer.get(1) };

        ItemStack[] socInputs = { ItemList.Circuit_Chip_Simple_SoC.get(1L), ItemList.Circuit_Chip_SoC.get(1L),
            ItemList.Circuit_Chip_SoC2.get(1L), ItemList.Circuit_Chip_CrystalSoC.get(1L) };

        int[] counts = { 0, 0, 0 };

        if (circuitAssembler != null)
            applyCircuitMultipliers(circuitAssembler, processorOutputs, assemblyOutputs, socInputs, 2, 4, 2, counts);
        if (cal != null) applyCircuitMultipliers(cal, processorOutputs, assemblyOutputs, socInputs, 32, 32, 32, counts);

        EZGT.LOG.info(
            "Applied easy circuit output ratios to {} processor recipes ({} SoC) and {} assembly recipes",
            counts[0],
            counts[1],
            counts[2]);
    }

    private void applyCircuitMultipliers(RecipeMap<?> map, ItemStack[] processorOutputs, ItemStack[] assemblyOutputs,
        ItemStack[] socInputs, int processorAmt, int socProcessorAmt, int assemblyAmt, int[] counts) {
        for (GTRecipe recipe : map.getAllRecipes()) {
            if (recipe.mOutputs == null || recipe.mOutputs.length == 0) continue;
            ItemStack output = recipe.mOutputs[0];
            if (output == null) continue;

            if (matchesAny(output, processorOutputs)) {
                boolean isSocRecipe = hasInput(recipe, socInputs);
                output.stackSize = clampedStackSize(output, isSocRecipe ? socProcessorAmt : processorAmt);
                counts[0]++;
                if (isSocRecipe) counts[1]++;
            } else if (matchesAny(output, assemblyOutputs)) {
                output.stackSize = clampedStackSize(output, assemblyAmt);
                counts[2]++;
            }
        }
    }

    private void modifyMotorRecipes() {
        EZGT.LOG.info("Assembly line recipe count: {}", GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes.size());

        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if (recipe.mOutput == null) continue;
            if (recipe.mOutput.getDisplayName()
                .toLowerCase()
                .contains("motor")) {
                EZGT.LOG.info(
                    "ASL motor found: {} stackSize: {}",
                    recipe.mOutput.getDisplayName(),
                    recipe.mOutput.stackSize);
            }
        }

        if (ModConfig.Rates.easyMotors == 1.0f) return;

        ItemStack[] motorOutputs = { ItemList.Electric_Motor_LV.get(1L), ItemList.Electric_Motor_MV.get(1L),
            ItemList.Electric_Motor_HV.get(1L), ItemList.Electric_Motor_EV.get(1L), ItemList.Electric_Motor_IV.get(1L),
            ItemList.Electric_Motor_LuV.get(1L), ItemList.Electric_Motor_ZPM.get(1L),
            ItemList.Electric_Motor_UV.get(1L), ItemList.Electric_Motor_UHV.get(1L),
            ItemList.Electric_Motor_UEV.get(1L), ItemList.Electric_Motor_UIV.get(1L),
            ItemList.Electric_Motor_UMV.get(1L), ItemList.Electric_Motor_UXV.get(1L), };

        int assemblerCount = 0;
        int craftingCount = 0;

        RecipeMap<?> assembler = RecipeMap.ALL_RECIPE_MAPS.get("gt.recipe.assembler");
        if (assembler != null) {
            for (GTRecipe recipe : assembler.getAllRecipes()) {
                if (recipe.mOutputs == null || recipe.mOutputs.length == 0) continue;
                ItemStack output = recipe.mOutputs[0];
                if (output == null) continue;
                if (matchesAny(output, motorOutputs)) {
                    output.stackSize = Math.min(
                        Math.max(1, (int) (output.stackSize * ModConfig.Rates.easyMotors)),
                        output.getMaxStackSize());
                    assemblerCount++;
                }
            }
        }

        for (Object obj : net.minecraft.item.crafting.CraftingManager.getInstance()
            .getRecipeList()) {
            if (!(obj instanceof net.minecraft.item.crafting.IRecipe)) continue;
            net.minecraft.item.crafting.IRecipe recipe = (net.minecraft.item.crafting.IRecipe) obj;
            ItemStack output = recipe.getRecipeOutput();
            if (output == null) continue;
            if (matchesAny(output, motorOutputs)) {
                output.stackSize = Math
                    .min(Math.max(1, (int) (output.stackSize * ModConfig.Rates.easyMotors)), output.getMaxStackSize());
                craftingCount++;
            }
        }
    }

    private boolean hasInput(GTRecipe recipe, ItemStack[] inputs) {
        return recipe.mInputs != null && Arrays.stream(recipe.mInputs)
            .anyMatch(input -> matchesAny(input, inputs));
    }

    private boolean matchesAny(ItemStack stack, ItemStack[] targets) {
        if (stack == null) return false;
        return Arrays.stream(targets)
            .anyMatch(target -> matchesItem(stack, target));
    }

    private boolean matchesItem(ItemStack stack, ItemStack target) {
        if (target == null || stack.getItem() != target.getItem()) return false;
        return target.getItemDamage() == OreDictionary.WILDCARD_VALUE
            || stack.getItemDamage() == target.getItemDamage();
    }

    private int clampedStackSize(ItemStack output, int stackSize) {
        return Math.min(Math.max(1, stackSize), output.getMaxStackSize());
    }
}

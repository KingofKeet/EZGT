package com.keet.ezgt.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.keet.ezgt.ModConfig;

import gregtech.common.OreMixBuilder;

@Mixin(value = OreMixBuilder.class, remap = false)
public class OreMixBuilderMixin {

    @Inject(method = "density", at = @At("HEAD"), cancellable = true)
    private void multiplyDensity(int density, CallbackInfoReturnable<OreMixBuilder> cir) {
        if (ModConfig.Rates.veinRichness != 1.0f) {
            int newDensity = Math.max(1, (int) (density * ModConfig.Rates.veinRichness));
            ((OreMixBuilder) (Object) this).density = newDensity;
            cir.setReturnValue((OreMixBuilder) (Object) this);
        }
    }
}

package shipwrights.genesis.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true)
    public void getDepthFarMixin(CallbackInfoReturnable<Float> cir) {
        Float original = cir.getReturnValue();
        float finiteFarPlane = original != null && Float.isFinite(original)
                ? Math.max(original, 4_096.0f)
                : 4_096.0f;
        cir.setReturnValue(finiteFarPlane);
    }
}

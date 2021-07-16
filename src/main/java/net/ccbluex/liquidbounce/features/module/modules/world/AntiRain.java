/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory

public class AntiRain
extends Module {
    public AntiRain() {
        super("AntiRain", Category.World);
    }

    @EventTarget
    public void onUpdate(EventUpdate e) {
        if (AntiRain.mc.world.isRaining()) {
            AntiRain.mc.world.setRainStrength(0.0f);
            AntiRain.mc.world.setThunderStrength(0.0f);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}

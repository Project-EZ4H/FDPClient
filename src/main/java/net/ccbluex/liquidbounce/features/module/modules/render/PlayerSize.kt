/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.FloatValue
@ModuleInfo(
        name = "PlayerSize",
        description = "Edit Fov",
        category = ModuleCategory.RENDER
)
class PlayerSize : Module() {
    val playerSizeValue = FloatValue("PlayerSize", 0.5F, 0.01F, 5F)
}

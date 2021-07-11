/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.Teams
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.EaseUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.potion.Potion
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.util.*
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import net.minecraft.client.renderer.entity.RenderManager
import org.lwjgl.util.glu.Cylinder


@ModuleInfo(name = "KillAura", description = "Automatically attacks targets around you.",
    category = ModuleCategory.COMBAT, keyBind = Keyboard.KEY_R)
class KillAura : Module() {

    /**
     * OPTIONS
     */

    // CPS - Attack speed
    private val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 12, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minCPS.get()
            if (i > newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), this.get())
        }
    }

    private val minCPS: IntegerValue = object : IntegerValue("MinCPS", 9, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxCPS.get()
            if (i < newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(this.get(), maxCPS.get())
        }
    }

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    // Range
    val rangeValue = FloatValue("Range", 4.2F, 1f, 8f)
    private val blockRangeValue = FloatValue("BlockRange", 6.0F, 1.0F, 8.0F)
    private val throughWallsRangeValue = FloatValue("ThroughWallsRange", 2.75F, 0f, 8f)
    private val switchDelayValue = IntegerValue("SwitchDelay", 350, 0, 1000)
    private val circleRadiusValue = FloatValue("CircleRadius", 1.0F,0.5F, 3.0F)
    private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f)

    // Modes
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime","Armor"), "Distance")
    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val blockModeValue = ListValue("Block", arrayOf("Packet","Normal"), "Normal")
    private val rotationModeValue = ListValue("Rotation", arrayOf("Normal","AntiCheat","BackTrace"), "Normal")
    private val markModeValue = ListValue("Mark", arrayOf("Jello","Circle","Plat","Box","Off"), "Jello")
    private val boundingBoxModeValue = ListValue("LockLocation", arrayOf("Head","Auto"), "Auto")

    // Bypass
    private val swingValue = ListValue("Swing", arrayOf("Normal", "Packet", "None"), "Normal")
    private val keepSprintValue = BoolValue("KeepSprint", true)

    // AutoBlock
    private val autoBlockValue = BoolValue("AutoBlock", false)
    private val interactAutoBlockValue = BoolValue("InteractAutoBlock", true)
    private val delayedBlockValue = BoolValue("DelayedBlock", true)
    private val autoBlockFacing = BoolValue("AutoBlockFacing",false)
    private val showTargetValue = BoolValue("ShowTarget", true)
    private val blockRate = IntegerValue("BlockRate", 100, 1, 100)

    // Raycast
    private val raycastValue = BoolValue("RayCast", true)
    private val raycastIgnoredValue = BoolValue("RayCastIgnored", false)
    private val livingRaycastValue = BoolValue("LivingRayCast", true)

    // Bypass
    private val aacValue = BoolValue("AAC", true)

    // Turn Speed
    private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minTurnSpeed.get()
            if (v > newValue) set(v)
        }
    }

    private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxTurnSpeed.get()
            if (v < newValue) set(v)
        }
    }

    private val silentRotationValue = BoolValue("SilentRotation", true)
    private val rotationStrafeValue = ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Slient")
    private val strafeOnlyGroundValue = BoolValue("StrafeOnlyGround",true)
    private val randomCenterValue = BoolValue("RandomCenter", false)
    private val outborderValue = BoolValue("Outborder", false)
    private val hitableValue = BoolValue("AlwaysHitable",true)
    private val fovValue = FloatValue("FOV", 180f, 0f, 180f)

    // Predict
    private val predictValue = BoolValue("Predict", true)

    private val maxPredictSize: FloatValue = object : FloatValue("MaxPredictSize", 1f, 0.1f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minPredictSize.get()
            if (v > newValue) set(v)
        }
    }

    private val minPredictSize: FloatValue = object : FloatValue("MinPredictSize", 1f, 0.1f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxPredictSize.get()
            if (v < newValue) set(v)
        }
    }

    // Bypass
    private val failRateValue = FloatValue("FailRate", 0f, 0f, 100f)
    private val fakeSwingValue = BoolValue("FakeSwing", true)
    private val noInventoryAttackValue = BoolValue("NoInvAttack", false)
    private val noInventoryDelayValue = IntegerValue("NoInvDelay", 200, 0, 500)
    private val limitedMultiTargetsValue = IntegerValue("LimitedMultiTargets", 0, 0, 50)

    // Visuals
    private val markValue = BoolValue("Mark", true)
    private val fakeSharpValue = BoolValue("FakeSharp", true)

    /**
     * MODULE
     */

    // Target
    var target: EntityLivingBase? = null
    private var markEntity: EntityLivingBase? = null
    private val markTimer=MSTimer()
    private var currentTarget: EntityLivingBase? = null
    private var hitable = false
    private val prevTargetEntities = mutableListOf<Int>()
    var noBlock = false

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0
    private val markTimer = MSTimer()
    private var markEntity: EntityLivingBase? = null

    // Container Delay
    private var containerOpen = -1L

    // Fake block status
    var blockingStatus = false
    private var espAnimation = 0.0
    private var isUp = true

    /**
     * Enable kill aura module
     */
    override fun onEnable() {
        mc.thePlayer ?: return
        mc.theWorld ?: return

        updateTarget()
    }

    /**
     * Disable kill aura module
     */
    override fun onDisable() {
        target = null
        currentTarget = null
        hitable = false
        prevTargetEntities.clear()
        attackTimer.reset()
        clicks = 0

        stopBlocking()
    }

    private fun esp(entity : EntityLivingBase, partialTicks : Float, radius : Float) {
        GL11.glPushMatrix()
        GL11.glDisable(3553)
        GLUtils.startSmooth()
        GL11.glDisable(2929)
        GL11.glDepthMask(false)
        GL11.glLineWidth(1.0F)
        GL11.glBegin(3)
        val x: Double = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - mc.renderManager.viewerPosX
        val y: Double = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - mc.renderManager.viewerPosY
        val z: Double = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - mc.renderManager.viewerPosZ
        for (i in 0..360) {
            val rainbow = Color(Color.HSBtoRGB((mc.thePlayer.ticksExisted / 70.0 + sin(i / 50.0 * 1.75)).toFloat() % 1.0f, 0.7f, 1.0f))
            GL11.glColor3f(rainbow.red / 255.0f, rainbow.green / 255.0f, rainbow.blue / 255.0f)
            GL11.glVertex3d(x + radius * cos(i * 6.283185307179586 / 45.0), y + espAnimation, z + radius * sin(i * 6.283185307179586 / 45.0))
        }
        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(2929)
        GLUtils.endSmooth()
        GL11.glEnable(3553)
        GL11.glPopMatrix()
    }

    private fun drawESP(entity: EntityLivingBase, color: Int, e: Render3DEvent) {
        val x: Double =
            entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * e.partialTicks.toDouble() - mc.renderManager.renderPosX
        val y: Double =
            entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * e.partialTicks.toDouble() - mc.renderManager.renderPosY
        val z: Double =
            entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * e.partialTicks.toDouble() - mc.renderManager.renderPosZ
        val radius = 0.15f
        val side = 4
        GL11.glPushMatrix()
        GL11.glTranslated(x, y + 2, z)
        GL11.glRotatef(-entity.width, 0.0f, 1.0f, 0.0f)
        RenderUtils.glColor(color)
        RenderUtils.enableSmoothLine(1.5F)
        val c = Cylinder()
        GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f)
        c.drawStyle = 100012
        RenderUtils.glColor(Color(80,255,80,200))
        c.draw(0F, radius, 0.3f, side, 1)
        c.drawStyle = 100012
        GL11.glTranslated(0.0, 0.0, 0.3)
        c.draw(radius, 0f, 0.3f, side, 1)
        GL11.glRotatef(90.0f, 0.0f, 0.0f, 1.0f)
        c.drawStyle = 100011
        GL11.glTranslated(0.0, 0.0, -0.3)
        RenderUtils.glColor(color)
        c.draw(0F, radius, 0.3f, side, 1)
        c.drawStyle = 100011
        GL11.glTranslated(0.0, 0.0, 0.3)
        c.draw(radius, 0F, 0.3f, side, 1)
        RenderUtils.disableSmoothLine()
        GL11.glPopMatrix()
    }
    /**
     * Motion event
     */
    @EventTarget
   fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            if (blockTarget != null && target == null)
                if ((mc.thePlayer.isRiding || canBlock) && !blockingStatus && autoBlockValue.get() && Random().nextInt(100) <= blockRate.get())
                    startBlocking(blockTarget!!)
            target ?: return
            currentTarget ?: return
           
            // Update hitable
            updateHitable()

            // AutoBlock
           if (autoBlockValue.get() && delayedBlockValue.get() && (canBlock || mc.thePlayer.isBlocking) && !blockingStatus && Random().nextInt(100) <= blockRate.get())
                startBlocking(currentTarget!!)
            return
        }
        blockTarget = getBlockTarget()
        if (rotationStrafeValue.get().equals("Off", true))
            update()

      private fun getBlockTarget(): EntityLivingBase? {
        val targets = ArrayList<EntityLivingBase>()
        mc.theWorld.loadedEntityList.forEach { if(it is EntityLivingBase && isEnemy(it) && mc.thePlayer.getDistanceToEntityBox(it) <= this.blockRangeValue.get()) {
            targets.add(it)
            this.blockTarget = it
         } }
        return if (targets.isEmpty()) null else targets[0]
      }
     }
    } 

    /**
     * Strafe event
     */
    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (rotationStrafeValue.get().equals("Off", true) && !mc.thePlayer.isRiding || LiquidBounce.moduleManager[Scaffold::class.java]!!.state)
            return

        update()

        if(strafeOnlyGroundValue.get()&&!mc.thePlayer.onGround || LiquidBounce.moduleManager[Scaffold::class.java]!!.state)
            return

        if (currentTarget != null && RotationUtils.targetRotation != null) {
            when (rotationStrafeValue.get().toLowerCase()) {
                "strict" -> {
                    val (yaw) = RotationUtils.targetRotation ?: return
                    var strafe = event.strafe
                    var forward = event.forward
                    val friction = event.friction

                    var f = strafe * strafe + forward * forward

                    if (f >= 1.0E-4F) {
                        f = MathHelper.sqrt_float(f)

                        if (f < 1.0F)
                            f = 1.0F

                        f = friction / f
                        strafe *= f
                        forward *= f

                        val yawSin = MathHelper.sin((yaw * Math.PI / 180F).toFloat())
                        val yawCos = MathHelper.cos((yaw * Math.PI / 180F).toFloat())

                        mc.thePlayer.motionX += strafe * yawCos - forward * yawSin
                        mc.thePlayer.motionZ += forward * yawCos + strafe * yawSin
                    }
                    event.cancelEvent()
                }
                "silent" -> {
                    update()

                    RotationUtils.targetRotation.applyStrafeToPlayer(event)
                    event.cancelEvent()
                }
            }
        }
    }

    fun update() {
        if (cancelRun || (noInventoryAttackValue.get() && (mc.currentScreen is GuiContainer ||
                    System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())))
            return

        // Update target
        updateTarget()

        if (target == null) {
            stopBlocking()
            return
        }

        // Target
        currentTarget = target

        if (!targetModeValue.get().equals("Switch", ignoreCase = true) && isEnemy(currentTarget))
            target = currentTarget
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && (mc.currentScreen is GuiContainer ||
                    System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        if (!rotationStrafeValue.get().equals("Off", true) && !mc.thePlayer.isRiding)
            return

        if (mc.thePlayer.isRiding)
            update()

        if (target != null && currentTarget != null) {
            while (clicks > 0) {
                runAttack()
                clicks--
                attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
            }
        }
    }
      /**
     * Render event
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
        }
        if (currentTarget != null && attackTimer.hasTimePassed(attackDelay) &&
            currentTarget!!.hurtTime <= hurtTimeValue.get()) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
     /**
     * Render2D Event
     */
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (showTargetValue.get()) {
            val sr2 = ScaledResolution(mc)
            if (target != null) {
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
//                FontManager..drawStringWithShadow(target!!.name, (sr2.scaledWidth / 2 - FontManager.Chinese18.getStringWidth(target!!.name) / 2).toFloat(), (sr2.scaledHeight / 2 - 40).toFloat(), 16777215)
                FontManager.Chinese18.drawStringWithShadow(target!!.name, (sr2.scaledWidth / 2 - FontManager.Chinese18.getStringWidth(target!!.name) / 2).toFloat(), (sr2.scaledHeight / 2 - 40).toFloat(), 16777215)
                mc.textureManager.bindTexture(ResourceLocation("textures/gui/icons.png"))
                var i2 = 0
                var i3 = 0
                while (i2 < target!!.maxHealth / 2) {
                    mc.ingameGUI.drawTexturedModalRect(((sr2.scaledWidth / 2) - target!!.maxHealth / 2.0f * 10.0f / 2.0f + (i2 * 10)).toInt(), (sr2.scaledHeight / 2 - 20), 16, 0, 9, 9);
                    ++i2
                }
                i2 = 0
                while (i2 < target!!.health / 2.0){
                    mc.ingameGUI.drawTexturedModalRect(((sr2.scaledWidth / 2) - target!!.maxHealth / 2.0f * 10.0f / 2.0f + (i2 * 10)).toInt(), (sr2.scaledHeight / 2 - 20), 52, 0, 9, 9);
                    ++i2
                }
                while (i3 < 20 / 2.0f) {
                    mc.ingameGUI.drawTexturedModalRect(((sr2.scaledWidth / 2) - target!!.maxHealth / 2.0f * 10.0f / 2.0f + (i3 * 10)).toInt(), (sr2.scaledHeight / 2 - 30), 16, 9, 9, 9);
                    ++i3;
                }
                i3 = 0;
                while (i3 < target!!.totalArmorValue / 2.0f) {
                    mc.ingameGUI.drawTexturedModalRect(((sr2.scaledWidth / 2) - target!!.maxHealth / 2.0f * 10.0f / 2.0f + (i3 * 10)).toInt(), (sr2.scaledHeight / 2 - 30), 34, 9, 9, 9);
                    ++i3;
                }
            }
        }
   
    private fun renderESP() {
        if (markEntity!=null){
            if(markTimer.hasTimePassed(500) || markEntity!!.isDead){
                markEntity=null
                return
            }
            //can mark
            val drawTime = (System.currentTimeMillis() % 2000).toInt()
            val drawMode=drawTime>1000
            var drawPercent=drawTime/1000F
            //true when goes up
            if(!drawMode){
                drawPercent=1-drawPercent
            }else{
                drawPercent-=1
            }
            val points = mutableListOf<Vec3>()
            val bb=markEntity!!.entityBoundingBox
            val radius=bb.maxX-bb.minX
            val height=bb.maxY-bb.minY
            val posX = markEntity!!.lastTickPosX + (markEntity!!.posX - markEntity!!.lastTickPosX) * mc.timer.renderPartialTicks
            var posY = markEntity!!.lastTickPosY + (markEntity!!.posY - markEntity!!.lastTickPosY) * mc.timer.renderPartialTicks
            if(drawMode){
                posY-=0.5
            }else{
                posY+=0.5
            }
            val posZ = markEntity!!.lastTickPosZ + (markEntity!!.posZ - markEntity!!.lastTickPosZ) * mc.timer.renderPartialTicks
            for(i in 0..360 step 7){
                points.add(Vec3(posX - sin(i * Math.PI / 180F) * radius,posY+height*drawPercent,posZ + cos(i * Math.PI / 180F) * radius))
            }
            points.add(points[0])
            //draw
            mc.entityRenderer.disableLightmap()
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glBegin(GL11.GL_LINE_STRIP)
            for(i in 0..20) {
                var moveFace=(height/60F)*i
                if(drawMode){
                    moveFace=-moveFace
                }
                val firstPoint=points[0]
                GL11.glVertex3d(
                    firstPoint.xCoord - mc.renderManager.viewerPosX, firstPoint.yCoord - moveFace - mc.renderManager.viewerPosY,
                    firstPoint.zCoord - mc.renderManager.viewerPosZ
                )
                GL11.glColor4f(1F, 1F, 1F, 0.7F*(i/20F))
                for (vec3 in points) {
                    GL11.glVertex3d(
                        vec3.xCoord - mc.renderManager.viewerPosX, vec3.yCoord - moveFace - mc.renderManager.viewerPosY,
                        vec3.zCoord - mc.renderManager.viewerPosZ
                    )
                }
                GL11.glColor4f(0F,0F,0F,0F)
            }
            GL11.glEnd()
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glPopMatrix()
        }
    }

    /**
     * Render event
     */
     @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && (mc.currentScreen is GuiContainer ||
                        System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        target ?: return
//        val targetList = mutableListOf<EntityLivingBase>()
//        mc.theWorld.loadedEntityList.forEach {
//            for(i in prevTargetEntities)
//                if(it is EntityLivingBase && it.entityId == i)
//                    targetList.add(it)
//        }

        if (!targetModeValue.get().equals("Multi", ignoreCase = true)) {
            when (markModeValue.get()) {
                "Jello" -> {
                    renderESP()
                    drawESP(target!!, Color(80, 255, 80).rgb, event)
                }
                "Circle" -> {
                    if (espAnimation > target!!.eyeHeight + 0.4 || espAnimation < 0) {
                        isUp = !isUp
                    }
                    if (isUp) {
                        espAnimation += 0.05 * 60 / Minecraft.getDebugFPS()
                    } else {
                        espAnimation -= 0.05 * 60 / Minecraft.getDebugFPS()
                    }
                    if (isUp) {
                        esp(target!!, event.partialTicks, circleRadiusValue.get())
                    } else {
                        esp(target!!, event.partialTicks, circleRadiusValue.get())
                    }
                }
                "Plat" -> RenderUtils.drawPlatform(
                    target!!,
                    if (hitable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70)
                )
                "Box" -> {
                    val renderManager = mc.renderManager
                    val x = (target!!.lastTickPosX
                            + (target!!.posX - target!!.lastTickPosX) * mc.timer.renderPartialTicks
                            - renderManager.renderPosX)
                    val y = (target!!.lastTickPosY
                            + (target!!.posY - target!!.lastTickPosY) * mc.timer.renderPartialTicks
                            - renderManager.renderPosY)
                    val z = (target!!.lastTickPosZ
                            + (target!!.posZ - target!!.lastTickPosZ) * mc.timer.renderPartialTicks
                            - renderManager.renderPosZ)
                    val width = target!!.entityBoundingBox.maxX - target!!.entityBoundingBox.minX
                    val height = (target!!.entityBoundingBox.maxY - target!!.entityBoundingBox.minY
                            + 0.25)
                    -target!!.entityBoundingBox.minY + 0.25
                    val red = if (target!!.hurtTime > 0) 1.0f else 0.0f
                    val green = if (target!!.hurtTime > 0) 0.2f else 1.0f
                    val blue = if (target!!.hurtTime > 0) 0.0f else 0.0f
                    val alpha = 0.2f
                    val lineRed = if (target!!.hurtTime > 0) 1.0f else 0.0f
                    val lineGreen = if (target!!.hurtTime > 0) 0.2f else 1.0f
                    val lineBlue = if (target!!.hurtTime > 0) 0.0f else 0.0f
                    val lineAlpha = 1.0f
                    val lineWidth = 2.0f
                    RenderUtils.drawEntityKillAuraESP(
                        x, y, z, width, height, red, green, blue, alpha, lineRed, lineGreen,
                        lineBlue, lineAlpha, lineWidth
                    )
                }
            }
//            for(i in targetList) {
//                when (markModeValue.get()) {
//                    "New" -> drawESP(i, Color(80, 255, 80).rgb, event)
//                    "Circle" -> {
//                        if (espAnimation > i.eyeHeight + 0.4 || espAnimation < 0) {
//                            isUp = !isUp
//                        }
//                        if (isUp) {
//                            espAnimation += 0.05 * 60 / Minecraft.getDebugFPS()
//                        } else {
//                            espAnimation -= 0.05 * 60 / Minecraft.getDebugFPS()
//                        }
//                        if (isUp) {
//                            esp(i, event.partialTicks, circleRadiusValue.get())
//                        } else {
//                            esp(i, event.partialTicks, circleRadiusValue.get())
//                        }
//                    }
//                    "Plat" -> RenderUtils.drawPlatform(
//                        i,
//                        if (hitable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70)
//                    )
//                    "Box" -> {
//                        val renderManager = mc.renderManager
//                        val x = (i.lastTickPosX
//                                + (i.posX - i.lastTickPosX) * mc.timer.renderPartialTicks
//                                - renderManager.renderPosX)
//                        val y = (i.lastTickPosY
//                                + (i.posY - i.lastTickPosY) * mc.timer.renderPartialTicks
//                                - renderManager.renderPosY)
//                        val z = (i.lastTickPosZ
//                                + (i.posZ - i.lastTickPosZ) * mc.timer.renderPartialTicks
//                                - renderManager.renderPosZ)
//                        val width = i.entityBoundingBox.maxX - i.entityBoundingBox.minX
//                        val height = (i.entityBoundingBox.maxY - i.entityBoundingBox.minY
//                                + 0.25)
//                        -i.entityBoundingBox.minY + 0.25
//                        val red = if (i.hurtTime > 0) 1.0f else 0.0f
//                        val green = if (i.hurtTime > 0) 0.2f else 1.0f
//                        val blue = if (i.hurtTime > 0) 0.0f else 0.0f
//                        val alpha = 0.2f
//                        val lineRed = if (i.hurtTime > 0) 1.0f else 0.0f
//                        val lineGreen = if (i.hurtTime > 0) 0.2f else 1.0f
//                        val lineBlue = if (i.hurtTime > 0) 0.0f else 0.0f
//                        val lineAlpha = 1.0f
//                        val lineWidth = 2.0f
//                        RenderUtils.drawEntityKillAuraESP(
//                            x, y, z, width, height, red, green, blue, alpha, lineRed, lineGreen,
//                            lineBlue, lineAlpha, lineWidth
//                        )
//                    }
//                }
//            }
        }
        if (currentTarget != null && attackTimer.hasTimePassed(attackDelay) &&
                currentTarget!!.hurtTime <= hurtTimeValue.get()) {
            clicks++
            attackTimer.reset()
        }
    }

    /**
     * Handle entity move
     */
    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        val movedEntity = event.movedEntity

        if (target == null || movedEntity != currentTarget)
            return

        updateHitable()
    }

    /**
     * Attack enemy
     */
    private fun runAttack() {
        target ?: return
        currentTarget ?: return

        // Settings
        val failRate = failRateValue.get()
        val swing = swingValue.get()
        val multi = targetModeValue.get().equals("Multi", ignoreCase = true)
        val openInventory = aacValue.get() && mc.currentScreen is GuiInventory
        val failHit = failRate > 0 && Random().nextInt(100) <= failRate

        // Close inventory when open
        if (openInventory)
            mc.netHandler.addToSendQueue(C0DPacketCloseWindow())

        // Check is not hitable or check failrate
        if (!hitable || failHit) {
            if (!swing.equals("none",true) && (fakeSwingValue.get() || failHit)) {
                if(swing.equals("packet",true)){
                    mc.netHandler.addToSendQueue(C0APacketAnimation())
                }else{
                    mc.thePlayer.swingItem()
                }
            }
        } else {
            // Attack
            if (!multi) {
                attackEntity(currentTarget!!)
            } else {
                var targets = 0

                for (entity in mc.theWorld.loadedEntityList) {
                    val distance = mc.thePlayer.getDistanceToEntityBox(entity)

                    if (entity is EntityLivingBase && isEnemy(entity) && distance <= getRange(entity)) {
                        attackEntity(entity)

                        targets += 1

                        if (limitedMultiTargetsValue.get() != 0 && limitedMultiTargetsValue.get() <= targets)
                            break
                }
             }
          }
          if(switchTimer.hasTimePassed(switchDelayValue.get().toLong()) || targetModeValue.get() != "Switch") {
            prevTargetEntities.add(if (aacValue.get()) target!!.entityId else currentTarget!!.entityId)
            switchTimer.reset()
          }
            if (target == currentTarget)
                target = null
        }

        // Open inventory
        if (openInventory)
            mc.netHandler.addToSendQueue(C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT))
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        // Reset fixed target to null
        target = null

        // Settings
        val hurtTime = hurtTimeValue.get()
        val fov = fovValue.get()
        val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)

        // Find possible targets
        val targets = mutableListOf<EntityLivingBase>()

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isEnemy(entity) || (switchMode && prevTargetEntities.contains(entity.entityId)))
                continue

            val distance = mc.thePlayer.getDistanceToEntityBox(entity)
            val entityFov = RotationUtils.getRotationDifference(entity)

            if (distance <= maxRange && (fov == 180F || entityFov <= fov) && entity.hurtTime <= hurtTime)
                targets.add(entity)
        }

        // Cleanup last targets when no targets found and try again
        if (targets.isEmpty()) {
            if (prevTargetEntities.isNotEmpty()) {
                prevTargetEntities.clear()
                updateTarget()
            }

            return
        }

        // Sort targets by priority
       when (priorityValue.get().toLowerCase()) {
            "distance" -> targets.sortBy { mc.thePlayer.getDistanceToEntityBox(it) } // Sort by distance
            "health" -> targets.sortBy { it.health } // Sort by health
            "direction" -> targets.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
            "livingtime" -> targets.sortBy { -it.ticksExisted } // Sort by existence
            "armor" -> targets.sortBy { it.totalArmorValue }
        }

        // Find best target
        for (entity in targets) {
            // Update rotations to current target
            if (!updateRotations(entity)) // when failed then try another target
                continue

            // Set target to current entity
            target = entity
            return
         }   
            
            // Cleanup last targets when no target found and try again
        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Check if [entity] is selected as enemy with current target options and other modules
     */
    private fun isEnemy(entity: Entity?): Boolean {
        if (entity is EntityLivingBase && (EntityUtils.targetDead || isAlive(entity)) && entity != mc.thePlayer) {
            if (!EntityUtils.targetInvisible && entity.isInvisible())
                return false

            if (EntityUtils.targetPlayer && entity is EntityPlayer) {
                if (entity.isSpectator || AntiBot.isBot(entity))
                    return false

                if (EntityUtils.isFriend(entity))
                    return false

                val teams = LiquidBounce.moduleManager[Teams::class.java] as Teams

                return !teams.state || !teams.isInYourTeam(entity)
            }

            return EntityUtils.targetMobs && EntityUtils.isMob(entity) || EntityUtils.targetAnimals &&
                    EntityUtils.isAnimal(entity)
        }

        return false
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase) {
        // Stop blocking
        if (mc.thePlayer.isBlocking || blockingStatus) {
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                BlockPos.ORIGIN, EnumFacing.DOWN))
            blockingStatus = false
        }

        // Call attack event
        LiquidBounce.eventManager.callEvent(AttackEvent(entity))
        markEntity = entity
        markTimer.reset()

        // Attack target
        val swing=swingValue.get()
        if(swing.equals("packet",true)){
            mc.netHandler.addToSendQueue(C0APacketAnimation())
        }else if(swing.equals("normal",true)){
            mc.thePlayer.swingItem()
        }

        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))

        if (keepSprintValue.get()) {
            // Critical Effect
            if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder &&
                !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && !mc.thePlayer.isRiding)
                mc.thePlayer.onCriticalHit(entity)

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, entity.creatureAttribute) > 0F)
                mc.thePlayer.onEnchantmentCritical(entity)
        } else {
            if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR)
                mc.thePlayer.attackTargetEntityWithCurrentItem(entity)
        }

        // Extra critical effects
        val criticals = LiquidBounce.moduleManager[Criticals::class.java] as Criticals

        for (i in 0..2) {
            // Critical Effect
            if (mc.thePlayer.fallDistance > 0F && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder && !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(Potion.blindness) && mc.thePlayer.ridingEntity == null || criticals.state && criticals.msTimer.hasTimePassed(criticals.delayValue.get().toLong()) && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava && !mc.thePlayer.isInWeb)
                mc.thePlayer.onCriticalHit(target)

            // Enchant Effect
            if (EnchantmentHelper.getModifierForCreature(mc.thePlayer.heldItem, target!!.creatureAttribute) > 0.0f || fakeSharpValue.get())
                mc.thePlayer.onEnchantmentCritical(target)
        }

        // Start blocking after attack
        if (mc.thePlayer.isBlocking || (autoBlockValue.get() && canBlock())) {
            if (!(blockRate.get() > 0 && Random().nextInt(100) <= blockRate.get()))
                return

            if (delayedBlockValue.get())
                return

            startBlocking(entity, interactAutoBlockValue.get())
        }
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        if(maxTurnSpeed.get() <= 0F)
            return true

        var boundingBox = entity.entityBoundingBox

        if (predictValue.get())
            boundingBox = boundingBox.offset(
                (entity.posX - entity.prevPosX) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                (entity.posY - entity.prevPosY) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                (entity.posZ - entity.prevPosZ) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get())
            )

        val (_, rotation) = RotationUtils.searchCenter(
            boundingBox,
            outborderValue.get() && !attackTimer.hasTimePassed(attackDelay / 2),
            randomCenterValue.get(),
            predictValue.get(),
            mc.thePlayer.getDistanceToEntityBox(entity) < throughWallsRangeValue.get()
        ) ?: return false

        val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation,
            (Math.random() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get()).toFloat())

        if (silentRotationValue.get())
            RotationUtils.setTargetRotation(limitedRotation, if (aacValue.get()) 15 else 0)
        else
            limitedRotation.toPlayer(mc.thePlayer)

        return true
    }

    /**
     * Check if enemy is hitable with current rotations
     */
    private fun updateHitable() {
        if(hitableValue.get()){
            hitable = true
            return
        }
        // Disable hitable check if turn speed is zero
        if(maxTurnSpeed.get() <= 0F) {
            hitable = true
            return
        }

        val reach = maxRange.toDouble()

        if (raycastValue.get()) {
            val raycastedEntity = RaycastUtils.raycastEntity(reach) {
                (!livingRaycastValue.get() || it is EntityLivingBase && it !is EntityArmorStand) &&
                        (isEnemy(it) || raycastIgnoredValue.get() || aacValue.get() && mc.theWorld.getEntitiesWithinAABBExcludingEntity(it, it.entityBoundingBox).isNotEmpty())
            }

            if (raycastValue.get() && raycastedEntity is EntityLivingBase
                && !EntityUtils.isFriend(raycastedEntity))
                currentTarget = raycastedEntity

            hitable = if(maxTurnSpeed.get() > 0F) currentTarget == raycastedEntity else true
        } else
            hitable = RotationUtils.isFaced(currentTarget, reach)
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: Entity, interact: Boolean) {
        if (interact) {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, interactEntity.positionVector))
            mc.netHandler.addToSendQueue(C02PacketUseEntity(interactEntity, C02PacketUseEntity.Action.INTERACT))
        }

        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
        blockingStatus = true
    }


    /**
     * Stop blocking
     */
    private fun stopBlocking() {
        if (blockingStatus) {
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            blockingStatus = false
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun: Boolean
        get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer)
                || LiquidBounce.moduleManager[Blink::class.java]!!.state || LiquidBounce.moduleManager[FreeCam::class.java]!!.state

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0 ||
            aacValue.get() && entity.hurtTime > 3


    /**
     * Check if player is able to block
     */
    private fun canBlock(): Boolean {
        return if(mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword){
            if(autoBlockFacing.get()&&(target!!.getDistanceToEntityBox(mc.thePlayer)<maxRange)){
                target!!.rayTrace(maxRange.toDouble(),1F).typeOfHit != MovingObjectPosition.MovingObjectType.MISS
            }else{
                true
            }
        }else{
            false
        }
    }

    /**
     * Range
     */
    private val maxRange: Float
        get() = max(rangeValue.get(), throughWallsRangeValue.get())

    private fun getRange(entity: Entity) =
        (if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRangeValue.get()) rangeValue.get() else throughWallsRangeValue.get()) - if (mc.thePlayer.isSprinting) rangeSprintReducementValue.get() else 0F

    /**
     * HUD Tag
     */
    override val tag: String
        get() = targetModeValue.get()
}

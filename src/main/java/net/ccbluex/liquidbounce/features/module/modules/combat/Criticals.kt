/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

@ModuleInfo(name = "Criticals", description = "Automatically deals critical hits.", category = ModuleCategory.COMBAT)
class Criticals : Module() {

    val modeValue = ListValue("Mode", arrayOf("Packet", "NcpPacket", "AACPacket", "NoGround", "Visual", "RedeSkySmartGround", "RedeSkyLowHop", "RedeSkyPacket", "Hop", "TPHop", "Jump", "LowJump"), "packet")
    val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val lookValue = BoolValue("UseC06Packet", false)
    private val debugValue = BoolValue("DebugMessage", false)

    val msTimer = MSTimer()

    private val rsGroundTimer = MSTimer()
    private var rsCritChange = 0
    private var target = 0;

    override fun onEnable() {
        if (modeValue.get().equals("NoGround", ignoreCase = true))
            mc.thePlayer.jump()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            val entity = event.targetEntity
            target = entity.entityId

            if (!mc.thePlayer.onGround || mc.thePlayer.isOnLadder || mc.thePlayer.isInWeb || mc.thePlayer.isInWater ||
                    mc.thePlayer.isInLava || mc.thePlayer.ridingEntity != null || entity.hurtTime > hurtTimeValue.get() ||
                    LiquidBounce.moduleManager[Fly::class.java]!!.state || !msTimer.hasTimePassed(delayValue.get().toLong()))
                return

            val x = mc.thePlayer.posX
            val y = mc.thePlayer.posY
            val z = mc.thePlayer.posZ
            val yaw = mc.thePlayer.rotationYaw
            val pitch = mc.thePlayer.rotationPitch

            when (modeValue.get().toLowerCase()) {
                "packet" -> {
                    if(lookValue.get()){
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.0625, z, yaw, pitch, true))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 1.1E-5, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y, z, yaw, pitch, false))
                    }else{
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.0625, z, true))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 1.1E-5, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y, z, false))
                    }
                    mc.thePlayer.onCriticalHit(entity)
                }

                "ncppacket" -> {
                    if(lookValue.get()){
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.11, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.1100013579, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.0000013579, z, yaw, pitch, false))
                    }else{
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.11, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.1100013579, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.0000013579, z, false))
                    }
                    mc.thePlayer.onCriticalHit(entity)
                }

                "aacpacket" -> {
                    if(lookValue.get()){
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.05250000001304, z, yaw, pitch, true))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.00150000001304, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.01400000001304, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.00150000001304, z, yaw, pitch, false))
                    }else{
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.05250000001304,z, true))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.01400000001304, z, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                    }
                    mc.thePlayer.onCriticalHit(entity)
                }

                "hop" -> {
                    mc.thePlayer.motionY = 0.1
                    mc.thePlayer.fallDistance = 0.1f
                    mc.thePlayer.onGround = false
                }

                "tphop" -> {
                    if(lookValue.get()){
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.02, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(x, y + 0.01, z, yaw, pitch, false))
                    }else{
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.02, z, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(x, y + 0.01, z, yaw, pitch, false))
                    }
                    mc.hePlayer.setPosition(x, y + 0.01, z)
                }

                "redeskypacket" -> {
                    if(lookValue.get()){
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(mc.thePlayer.posX - mc.thePlayer.motionX * 1.5, mc.thePlayer.posY + 3e-14, mc.thePlayer.posZ - mc.thePlayer.motionZ / 1.5, yaw, pitch, false))
                        mc.netHandler.addToSendQueue(C06PacketPlayerPosLook(mc.thePlayer.posX - mc.thePlayer.motionX / 3, mc.thePlayer.posY + 8e-15, mc.thePlayer.posZ - mc.thePlayer.motionZ / 3, yaw, pitch, false))
                    }else{
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(mc.thePlayer.posX - mc.thePlayer.motionX * 1.5, mc.thePlayer.posY + 3e-14, mc.thePlayer.posZ - mc.thePlayer.motionZ / 1.5, false))
                        mc.netHandler.addToSendQueue(C04PacketPlayerPosition(mc.thePlayer.posX - mc.thePlayer.motionX / 3, mc.thePlayer.posY + 8e-15, mc.thePlayer.posZ - mc.thePlayer.motionZ / 3, false))
                    }
                    mc.thePlayer.motionX *= 0.0
                    mc.thePlayer.motionZ *= 0.0
                }
                
                "visual" -> mc.thePlayer.onCriticalHit(entity)
                "jump" -> mc.thePlayer.motionY = 0.42
                "lowjump" -> mc.thePlayer.motionY = 0.3425
                "redeskylowhop" -> mc.thePlayer.motionY = 0.35
            }
            msTimer.reset()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer){
            when (modeValue.get().toLowerCase()) {
                "noground" -> packet.onGround = false
                "redeskysmartground" -> {
                    if(rsGroundTimer.hasTimePassed(1000)){
                        packet.onGround = LiquidBounce.combatManager.inCombat
                        if(rsGroundTimer.hasTimePassed(1200))
                            rsGroundTimer.reset()
                    }else{
                        packet.onGround = !LiquidBounce.combatManager.inCombat
                    }
                    if(!packet.onGround && mc.thePlayer.onGround && LiquidBounce.combatManager.inCombat && (packet is C04PacketPlayerPosition || packet is C06PacketPlayerPosLook)){
                        when(rsCritChange){
                            0 -> packet.y += 0.00000000000003
                            1 -> packet.y += 0.00000000000001
                            2 -> packet.y += 0.000000000000008
                        }
                        rsCritChange++
                        if(rsCritChange == 3)
                            rsCritChange = 0
                    }
                }
            }
        }
        if(packet is S0BPacketAnimation&&debugValue.get()){
            if(packet.animationType==4&&packet.entityID==target){
                chat("CRIT")
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}

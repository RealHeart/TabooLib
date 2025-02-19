@file:Isolated
@file:Suppress("DEPRECATION")

package taboolib.module.nms

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import org.tabooproject.reflex.Reflex.Companion.invokeMethod
import taboolib.common.Isolated
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.util.unsafeLazy
import taboolib.library.xseries.XMaterial
import java.util.concurrent.ConcurrentHashMap

fun Player.inputSign(lines: Array<String> = arrayOf(), function: (lines: Array<String>) -> Unit) {
    val location = location
    location.y = 0.0
    try {
        sendBlockChange(location, XMaterial.OAK_WALL_SIGN.parseMaterial()!!.createBlockData())
    } catch (t: NoSuchMethodError) {
        sendBlockChange(location, XMaterial.OAK_WALL_SIGN.parseMaterial()!!, 0.toByte())
    }
    sendSignChange(location, lines.format())
    SignsListener.inputs[name] = function
    nmsGeneric.openSignEditor(this, location.block)
}

private fun Array<String>.format(): Array<String> {
    val list = toMutableList()
    while (list.size < 4) {
        list.add("")
    }
    while (list.size > 4) {
        list.removeLast()
    }
    return list.toTypedArray()
}

@Isolated
@PlatformSide([Platform.BUKKIT])
private object SignsListener {

    val inputs = ConcurrentHashMap<String, (Array<String>) -> Unit>()

    val classChatSerializer by unsafeLazy {
        nmsClass("IChatBaseComponent\$ChatSerializer")
    }

    @SubscribeEvent
    fun onQuit(e: PlayerQuitEvent) {
        inputs.remove(e.player.name)
    }

    @SubscribeEvent
    fun onReceive(e: PacketReceiveEvent) {
        if (e.packet.name == "PacketPlayInUpdateSign" && inputs.containsKey(e.player.name)) {
            val function = inputs.remove(e.player.name) ?: return
            val lines = when {
                MinecraftVersion.majorLegacy > 11700 -> {
                    e.packet.read<Array<String>>("lines")!!
                }
                MinecraftVersion.majorLegacy > 10900 -> {
                    e.packet.read<Array<String>>("b")!!
                }
                else -> {
                    e.packet.read<Array<Any>>("b")!!.map { classChatSerializer.invokeMethod<String>("a", it, isStatic = true)!! }.toTypedArray()
                }
            }
            submit { function.invoke(lines) }
        }
    }
}
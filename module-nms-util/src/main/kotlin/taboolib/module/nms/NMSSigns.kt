package taboolib.module.nms

import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.Isolated
import taboolib.common.platform.Platform
import taboolib.common.platform.PlatformSide
import taboolib.common.platform.SubscribeEvent
import taboolib.common.platform.submit
import taboolib.common.reflect.Reflex.Companion.invokeMethod
import taboolib.common.util.Vector
import taboolib.library.xseries.XMaterial
import java.util.concurrent.ConcurrentHashMap

fun Player.inputSign(lines: Array<String> = arrayOf(), function: (lines: Array<String>) -> Unit) {
    val location = location
    location.y = 0.0
    try {
        sendBlockChange(location, XMaterial.OAK_WALL_SIGN.parseMaterial()!!, 0.toByte())
        sendSignChange(location, lines.format())
    } catch (t: Throwable) {
        t.printStackTrace()
    }
    inputs[name] = function
    nmsProxy<NMSGeneric>().openSignEditor(this, location.block)
}

private val inputs = ConcurrentHashMap<String, (Array<String>) -> Unit>()

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

@PlatformSide([Platform.BUKKIT])
internal object SignsListener {

    private val classChatSerializer by lazy {
        nmsClass("IChatBaseComponent\$ChatSerializer")
    }

    @SubscribeEvent
    fun e(e: PlayerQuitEvent) {
        inputs.remove(e.player.name)
    }

    @SubscribeEvent
    fun e(e: PacketReceiveEvent) {
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
                    e.packet.read<Array<Any>>("b")!!.map { classChatSerializer.invokeMethod<String>("a", it, fixed = true)!! }.toTypedArray()
                }
            }
            submit { function.invoke(lines) }
        }
    }
}
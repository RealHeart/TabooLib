package taboolib.module.nms

import io.netty.channel.Channel
import net.minecraft.network.NetworkManager
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.PacketListenerPlayOut
import net.minecraft.server.network.ServerConnection
import org.bukkit.Bukkit
import org.tabooproject.reflex.Reflex.Companion.getProperty
import org.tabooproject.reflex.Reflex.Companion.invokeMethod
import taboolib.common.io.isDevelopmentMode
import taboolib.common.platform.function.dev
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap

/**
 * TabooLib
 * taboolib.module.nms.ConnectionGetterImpl
 *
 * @author 坏黑
 * @since 2023/1/31 20:50
 */
class ConnectionGetterImpl : ConnectionGetter() {

    val major = MinecraftVersion.major
    val addressUsed = ConcurrentHashMap<InetSocketAddress, Any>()

    override fun getConnection(address: InetAddress, first: Boolean): Any {
        // 获取服务器中的所有连接
        val serverConnections = when (major) {
            // 1.8, 1.9, 1.10, 1.11, 1.12 -> List<NetworkManager> h
            0, 1, 2, 3, 4 -> {
                ((Bukkit.getServer() as CraftServer8).server as NMS8MinecraftServer).serverConnection.getProperty<List<Any>>("h")
            }
            // 1.13 -> List<NetworkManager> g
            5 -> {
                ((Bukkit.getServer() as CraftServer8).server as NMS8MinecraftServer).serverConnection.getProperty<List<Any>>("g")
            }
            // 1.14 -> List<NetworkManager> g
            // java.lang.NoSuchMethodError: 'net.minecraft.server.v1_16_R3.MinecraftServer org.bukkit.craftbukkit.v1_16_R3.CraftServer.getServer()'
            6 -> {
                ((Bukkit.getServer() as CraftServer16).server as NMS16MinecraftServer).serverConnection?.getProperty<List<Any>>("g")
            }
            // 1.15, 1.16 -> List<NetworkManager> connectedChannels
            7, 8 -> {
                ((Bukkit.getServer() as CraftServer16).server as NMS16MinecraftServer).serverConnection?.getProperty<List<Any>>("connectedChannels")
            }
            // 1.17 -> List<NetworkManager> getConnections()
            // 傻逼项目引入依赖天天出问题，滚去反射吧
            9 -> {
                ((Bukkit.getServer() as CraftServer19).server as NMSMinecraftServer).invokeMethod<ServerConnection>("getServerConnection")?.connections
            }
            // 1.18, 1.19 -> List<NetworkManager> getConnections()
            // 这个版本开始获取 ServerConnection 的方法变更为 getConnection()
            10, 11 -> {
                ((Bukkit.getServer() as CraftServer19).server as NMSMinecraftServer).connection?.connections
            }
            // 不支持
            else -> error("Unsupported Minecraft version: $major")
        } ?: error("Unable to get connections from ${Bukkit.getServer()}")
        // 获取相同 IP 的连接
        val connections = serverConnections.filter { getAddress(it).address == address }
        // 没有相同 IP 的连接
        if (connections.isEmpty()) {
            warning("Unable to get player connection (${address})")
            warning("Server connections:")
            serverConnections.forEach { conn -> warning("- ${getAddress(conn)}") }
            throw IllegalStateException()
        }
        // 打印信息
        if (isDevelopmentMode) {
            info("Player connection ($address)")
            info("Server connections:")
            serverConnections.forEach { conn -> info("- ${getAddress(conn)}") }
        }
        // 首次进入服务器
        val connection = if (first) {
            // 获取未被使用的连接
            connections.first { !addressUsed.containsKey(getAddress(it)) }.also { addressUsed[getAddress(it)] = it }
        } else {
            // 获取已使用的连接
            connections.first { conn -> addressUsed[getAddress(conn)] == conn }
        }
        dev("Player connection ($address) -> ${getAddress(connection)} (first=$first)")
        return connection
    }

    override fun getChannel(connection: Any): Channel {
        return (connection as NMS8NetworkManager).channel
    }

    override fun release(address: InetSocketAddress) {
        addressUsed.remove(address)
    }

    @Suppress("UNCHECKED_CAST")
    override fun newBundlePacket(iterator: List<Any>): Any {
        return ClientboundBundlePacket(iterator.asIterable() as Iterable<Packet<PacketListenerPlayOut>>)
    }

    private fun getAddress(connection: Any): InetSocketAddress {
        // 这种方式无法在 BungeeCord 中获取到正确的地址：
        // return (getChannel(connection).remoteAddress() as? InetSocketAddress)?.address
        // 因此要根据不同的版本获取不同的 SocketAddress 字段：
        return when (major) {
            // 1.8, 1.9, 1.10, 1.11, 1.12
            // public SocketAddress l;
            0, 1, 2, 3, 4 -> ((connection as NMS8NetworkManager).l as InetSocketAddress)
            // 1.13, 1.14, 1.15, 1.16
            // public SocketAddress socketAddress;
            5, 6, 7, 8 -> ((connection as NMS13NetworkManager).socketAddress as InetSocketAddress)
            // 1.17, 1.18, 1.19
            // public SocketAddress address;
            9, 10, 11 -> ((connection as NetworkManager).address as InetSocketAddress)
            // 不支持
            else -> error("Unsupported Minecraft version: $major")
        }
    }
}

typealias CraftServer8 = org.bukkit.craftbukkit.v1_8_R3.CraftServer

typealias CraftServer16 = org.bukkit.craftbukkit.v1_16_R2.CraftServer

typealias CraftServer19 = org.bukkit.craftbukkit.v1_19_R3.CraftServer

typealias NMS16MinecraftServer = net.minecraft.server.v1_16_R2.MinecraftServer

typealias NMS8MinecraftServer = net.minecraft.server.v1_8_R3.MinecraftServer

typealias NMSMinecraftServer = net.minecraft.server.MinecraftServer

typealias NMS8NetworkManager = net.minecraft.server.v1_8_R3.NetworkManager

typealias NMS13NetworkManager = net.minecraft.server.v1_13_R2.NetworkManager
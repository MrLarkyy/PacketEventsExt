package gg.aquatic.packeteventsext

import com.github.retrooper.packetevents.wrapper.PacketWrapper

class DynamicPacket(val wrapper: PacketWrapper<*>) {

    internal fun write(player: PacketUser) {
        wrapper.writeDynamic(player.silentContext)
    }
    internal fun writeAndFlush(player: PacketUser) {
        player.channel.eventLoop().execute {
            write(player)
            player.silentContext.flush()
        }
    }
}
/*
fun Collection<DynamicPacket>.write(player: PacketUser) {
    player.channel.eventLoop().execute {
        for (packet in this) {
            player.writeConstSilently(packet.buffer)
        }
    }
}

fun Collection<DynamicPacket>.writeAndFlush(player: PacketUser) {
    player.channel.eventLoop().execute {
        for (packet in this) {
            player.writeConstSilently(packet.buffer)
        }
        player.flush()
    }
}
 */
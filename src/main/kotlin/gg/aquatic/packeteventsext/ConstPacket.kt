package gg.aquatic.packeteventsext

import com.github.retrooper.packetevents.wrapper.PacketWrapper
import io.netty.buffer.ByteBuf

/**
 * The `ConstPacket` class represents a packet with a constant byte buffer.
 *
 * @property buffer The byte buffer associated with this packet.
 * @property immortal Indicates if the packet is immortal (should not be released).
 *
 * @constructor Creates a `ConstPacket` with the given buffer and immortality flag.
 * @constructor Creates a `ConstPacket` from a `PacketWrapper` instance.
 */
class ConstPacket(val buffer: ByteBuf, val immortal: Boolean = false) {

    var released = false
        internal set

    constructor(wrapper: PacketWrapper<*>, immortal: Boolean) : this(wrapper.constBuffer(immortal))

    companion object {
        private fun createByteBufs(vararg wrappers: PacketWrapper<*>): Array<ByteBuf> {
            return wrappers.map { it.constBuffer() }.toTypedArray()
        }
    }

    internal fun write(player: PacketUser) {
        if (released) return
        player.writeConstSilently(buffer)
    }

    /**
     * Writes the packet to the specified player and flushes the channel to ensure
     * all data is sent. If the packet is not immortal, it will be released after
     * writing and flushing.
     *
     * @param player The `PacketUser` to which the packet will be written and flushed.
     */
    fun writeAndFlush(player: PacketUser) {
        if (released) return
        player.channel.eventLoop().execute {
            write(player)
            player.flush()
            if (!immortal) {
                released = true
                buffer.unwrap().release()
            }
        }
    }
}

fun Collection<PacketWrapper<*>>.constPackets(immortal: Boolean = false): Collection<ConstPacket> {
    return this.map { ConstPacket(it, immortal) }
}
fun Array<PacketWrapper<*>>.constPackets(immortal: Boolean = false): Array<ConstPacket> {
    return this.map { ConstPacket(it, immortal) }.toTypedArray()
}

/*
internal fun Collection<ConstPacket>.write(player: PacketUser) {
    player.channel.eventLoop().execute {
        for (packet in this) {
            player.writeConstSilently(packet.buffer)
        }
    }
}

internal fun Collection<ConstPacket>.writeAndFlush(player: PacketUser) {
    player.channel.eventLoop().execute {
        for (packet in this) {
            player.writeConstSilently(packet.buffer)
        }
        player.flush()
    }
}
 */
package gg.aquatic.packeteventsext

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.User
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import org.bukkit.entity.Player
import java.util.function.Consumer


class PacketUser(
    val player: Player,
    val user: User,
) {
    val silentContext: ChannelHandlerContext = PacketUtils.getSilentContext(this.channel)

    val channel: Channel
        get() {
            return silentContext.channel()
        }

    fun writeAndOccasionallyFlushSilently(buffers: Array<ByteBuf>) {
        this.writeAndFlushWithThresholdSilently(buffers, 100)
    }

    fun writeAndFlushWithThresholdSilently(buffers: Array<ByteBuf>, threshold: Int) {
        this.writeAndFlushWithThresholdSilently(
            buffers,
            threshold
        ) { buffer: ByteBuf -> this.writeSilently(buffer) }
    }

    fun writeAndFlushWithThresholdSilently(
        buffers: Array<ByteBuf>,
        threshold: Int,
        writeFunction: Consumer<ByteBuf>
    ) {
        for (c in 1..buffers.size) {
            val buf = buffers[c - 1]
            writeFunction.accept(buf)
            if (c % threshold == 0) this.flush()
        }
        if (buffers.size % threshold != 0)  //last loop was not a flush
            this.flush()
    }

    fun writeDynamicSilently(packet: PacketWrapper<*>) {
        packet.writeDynamic(this.silentContext)
    }

    fun writeAndFlushDynamicSilently(packet: PacketWrapper<*>) {
        packet.writeAndFlushDynamic(this.silentContext)
    }

    fun writeSilently(buffer: ByteBuf) {
        this.silentContext.write(buffer)
    }

    fun writeAndFlushSilently(buffer: ByteBuf) {
        this.silentContext.writeAndFlush(buffer)
    }

    fun flush() {
        //this.silentContext().flush();
        this.channel.flush()
    }

    fun writeAllConstAndThenFlushSilently(bufs: Array<ByteBuf>) {
        for (buf in bufs) this.writeConstSilently(buf)
        this.flush()
    }

    fun writeConstSilently(buf: ByteBuf) {
        PacketUtils.writeConst(this.silentContext, buf)
    }

    fun writeAndFlushConstSilently(buf: ByteBuf) {
        PacketUtils.writeAndFlushConst(this.silentContext, buf)
    }
}

fun Player.toPacketUser(): PacketUser {
    return PacketUser(this, PacketEvents.getAPI().playerManager.getUser(this))
}
package gg.aquatic.packeteventsext

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.packeteventsext.PacketUtils.dynamic
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.Unpooled
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import org.bukkit.entity.Player


object PacketUtils {

    private val ALLOC: ByteBufAllocator = UnpooledByteBufAllocator(true, true)

    fun constBuffer(constBuffer: ByteBuf): ByteBuf {
        return Unpooled.unreleasableBuffer(constBuffer.asReadOnly())
    }

    fun buffer(): ByteBuf {
        return directBuffer()
    }
    fun directBuffer(): ByteBuf {
        return ALLOC.directBuffer()
    }
    fun directBuffer(capacity: Int): ByteBuf {
        return ALLOC.directBuffer(capacity)
    }

    fun writeConst(context: ChannelHandlerContext, constByteBuf: ByteBuf) {
        context.write(prepareConstToSend(constByteBuf))
    }

    @Suppress("UnstableApiUsage")
    fun getSilentContext(channel: Channel): ChannelHandlerContext {
        return channel.pipeline().context(PacketEvents.ENCODER_NAME)
    }

    fun writeAndFlushConst(context: ChannelHandlerContext, constByteBuf: ByteBuf): ChannelFuture? {
        return context.writeAndFlush(prepareConstToSend(constByteBuf))
    }
    private fun prepareConstToSend(byteBuf: ByteBuf): ByteBuf {
        return byteBuf.duplicate()
    }

    fun dynamic(wrapper: PacketWrapper<*>, context: ChannelHandlerContext): ByteBuf {
        return wrapper.createBuffer0(context.alloc().buffer())
    }
}

fun PacketWrapper<*>.constBuffer(direct: Boolean = true): ByteBuf {
    return PacketUtils.constBuffer(createBuffer(direct))
}

fun PacketWrapper<*>.createBuffer(direct: Boolean = true): ByteBuf {
    return createBuffer0(if (direct) PacketUtils.directBuffer() else PacketUtils.buffer())
}

fun Collection<PacketWrapper<*>>.constBuffers(direct: Boolean = true): Collection<ByteBuf> {
    return this.map { it.constBuffer(direct) }
}

fun PacketWrapper<*>.writeAndFlushDynamic(context: ChannelHandlerContext) {
    context.writeAndFlush(dynamic(this, context))
}

fun PacketWrapper<*>.writeDynamic(context: ChannelHandlerContext) {
    context.write(dynamic(this, context))
}

fun Collection<PacketWrapper<*>>.sendOptimized(vararg players: Player) {
    if (players.isEmpty()) return
    if (players.size == 1) {
        val player = players.first()
        val user = player.toPacketUser()
        user.channel.eventLoop().execute {
            this.forEach { it.writeDynamic(user.silentContext) }
            user.flush()
        }
        return
    }
    val consts = this.map { ConstPacket(it, false) }
    for (player in players) {
        val user = player.toPacketUser()
        user.channel.eventLoop().execute {
            for (packet in consts) {
                packet.write(user)
            }
            user.flush()
        }
    }
    for (const in consts) {
        const.tryRelease()
    }
}
fun Set<PacketWrapper<*>>.sendOptimized(vararg players: Player) = this.toList().sendOptimized(*players)
fun Array<PacketWrapper<*>>.sendOptimized(vararg players: Player) = this.toList().sendOptimized(*players)

@Suppress("UnstableApiUsage")
private fun PacketWrapper<*>.createBuffer0(emptyByteBuf: ByteBuf): ByteBuf {
    if (buffer != null) throw Exception("Incorrect invocation of PacketWrapper#createBuffer - buffer exists")

    val packetId: Int = packetTypeData.nativePacketId
    if (packetId < 0) throw Exception(
        ("Failed to create packet " + this::class.java.getSimpleName() + " in server version "
                + PacketEvents.getAPI().serverManager.version + "! Contact the developer and show him this error! " +
                "Otherwise Alix will not work as intended!")
    )

    buffer = emptyByteBuf
    writeVarInt(packetId)
    write()

    return emptyByteBuf //(ByteBuf) wrapper.buffer;
}
package gg.aquatic.packeteventsext

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import gg.aquatic.packeteventsext.PacketUtils.dynamic
import io.netty.buffer.*
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import org.bukkit.entity.Player


object PacketUtils {

    private val POOLED_ALLOC: ByteBufAllocator = PooledByteBufAllocator(true)
    private val UNPOOLED_ALLOC: ByteBufAllocator = UnpooledByteBufAllocator(true, true)
    //private val ALLOC: ByteBufAllocator = UnpooledByteBufAllocator(true, true)

    fun constBuffer(immortal: Boolean, constBuffer: ByteBuf): ByteBuf {
        //return Unpooled.unreleasableBuffer(constBuffer.asReadOnly())
        return if (immortal) {
            Unpooled.unreleasableBuffer(constBuffer.asReadOnly())
        } else {
            constBuffer
        }
    }

    /*
    fun buffer(): ByteBuf {
        return directBuffer()
    }

    fun directBuffer(): ByteBuf {
        return ALLOC.directBuffer()
    }
     */

    fun unpooledBuffer(): ByteBuf {
        return UNPOOLED_ALLOC.directBuffer()
    }
    fun pooledBuffer(): ByteBuf {
        return POOLED_ALLOC.directBuffer()
    }

    /*
    fun directBuffer(capacity: Int): ByteBuf {
        return ALLOC.directBuffer(capacity)
    }
     */

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

fun PacketWrapper<*>.constBuffer(immortal: Boolean): ByteBuf {
    return PacketUtils.constBuffer(immortal, createBuffer(immortal))
}

fun PacketWrapper<*>.createBuffer(immortal: Boolean): ByteBuf {
    return createBuffer0(if (immortal) PacketUtils.unpooledBuffer() else PacketUtils.pooledBuffer())
}
/*
fun Collection<PacketWrapper<*>>.constBuffers(immortal: Boolean = false): Collection<ByteBuf> {
    return this.map { it.constBuffer(immortal) }
}
 */

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
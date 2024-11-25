
# PacketEventsExt

Simple library to optimize the PacketEvents Netty usage.


## Usage/Examples

**Basic Usage**

For a basic use, you can use following method

```kotlin
val players: List<PacketPlayer> = ...
val packets: HashSet<PacketWrapper<*>> = ...
packets.sendOptimized(*players.toTypedArray())
```

Getting PacketPlayer instance

```kotlin
val player: Player = Bukkit.getPlayer("MrLarkyy_")
val packetPlayer = player.toPacketPlayer()
```

**Advanced Usage**

If you have got pre-defined constants of Packets we do recommend following usage:

ConstPacket creation:
```kotlin
val packets: HashSet<PacketWrapper<*>> = ...
val constPackets = packets.constPackets(true)
```

"true" value defines the immortality of ByteBuf. When the value is set to true, the Buffer uses Unpool allocater and makes the packet unreleasable


Sending the immortal packets:
```kotlin
companion object {
    val constPackets: Collection<ConstPacket> = ...
}

fun sendStaticPackets(packetPlayer: PacketPlayer) {
    packetPlayer.channel.eventLoop().execute {
        for (packet in constPackets) {
            packet.write(packetPlayer)
        }
        packetPlayer.flush()
    }
}

```






## Authors

- [@ShadowOfHeaven-Me](https://github.com/ShadowOfHeaven-Me)
- [@MrLarkyy](https://github.com/MrLarkyy)


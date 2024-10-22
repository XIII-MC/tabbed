package com.keenant.tabbed.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Some generic-ish packet utils.
 */
public class Packets {
    /**
     * Creates a PLAYER_INFO packet from the params.
     * @param action
     * @param data
     * @return
     */
    public static WrapperPlayServerPlayerInfo getPacket(WrapperPlayServerPlayerInfo.Action action, WrapperPlayServerPlayerInfo.PlayerData data) {
        return getPacket(action, Collections.singletonList(data));
    }
    /**
     * Creates a PLAYER_INFO packet from the params.
     * @param action
     * @param data
     * @return
     */

    public static WrapperPlayServerPlayerInfo getPacket(WrapperPlayServerPlayerInfo.Action action, List<WrapperPlayServerPlayerInfo.PlayerData> data) {
        return new WrapperPlayServerPlayerInfo(action, data);
    }

    /**
     * Sends a list of PacketEvents packets to a player.
     * @param player
     * @param packets
     * @return
     */
    public static void send(Player player, List<WrapperPlayServerPlayerInfo> packets) {
        for (PacketWrapper<?> packet : packets)
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}

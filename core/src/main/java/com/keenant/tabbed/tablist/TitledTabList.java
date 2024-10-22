package com.keenant.tabbed.tablist;


import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;


/**
 * A very basic tab list. It doesn't modify the items, only the header/footer.
 */
public class TitledTabList implements TabList {
    protected final Player player;
    private String header;
    private String footer;

    public TitledTabList(Player player) {
        this.player = player;
    }

    @Override
    public TitledTabList enable() {
        return this;
    }

    @Override
    public TitledTabList disable() {
        resetHeaderFooter();
        return this;
    }

    public void setHeaderFooter(String header, String footer) {
        setHeader(header);
        setFooter(footer);
    }

    public void resetHeaderFooter() {
        resetHeader();
        resetFooter();
    }

    public void setHeader(String header) {
        this.header = header;
        updateHeaderFooter();
    }

    public void resetHeader() {
        setHeader(null);
    }

    public void setFooter(String footer) {
        this.footer = footer;
        updateHeaderFooter();
    }

    public void resetFooter() {
        setFooter(null);
    }

    private void updateHeaderFooter() {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPlayerListHeaderAndFooter(Component.text(this.header == null ? "" : this.header), Component.text(this.footer == null ? "" : this.footer)));
    }

    public Player getPlayer() {
        return player;
    }

    public String getFooter() {
        return footer;
    }

    public String getHeader() {
        return header;
    }
}

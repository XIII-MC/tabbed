package com.keenant.tabbed.item;

import com.github.retrooper.packetevents.PacketEvents;
import com.keenant.tabbed.util.Skin;
import com.keenant.tabbed.util.Skins;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

/**
 * A tab item that represents a player.
 */
public class PlayerTabItem implements TabItem {
    private final Player player;
    private final PlayerProvider<String> textProvider;
    private final PlayerProvider<Skin> skinProvider;
    private String text;
    private int ping;
    private Skin skin;

    public PlayerTabItem(Player player, PlayerProvider<String> textProvider, PlayerProvider<Skin> skinProvider) {
        this.player = player;
        this.textProvider = textProvider;
        this.skinProvider = skinProvider;
        this.text = textProvider.get(player);
        this.ping = getNewPing();
        this.skin = skinProvider.get(player);
        updateText();
        updatePing();
        updateSkin();
    }

    public PlayerTabItem(Player player, PlayerProvider<String> textProvider) {
        this(player, textProvider, SKIN_PROVIDER);
    }

    public PlayerTabItem(Player player) {
        this(player, LIST_NAME_PROVIDER);
    }

    @Override
    public boolean updateText() {
        if (!this.player.isOnline() || !this.player.isValid())
            return false;

        String newText = this.textProvider.get(this.player);
        boolean update = this.text == null || !newText.equals(this.text);
        this.text = newText;
        return update;
    }

    @Override
    public boolean updatePing() {
        if (!this.player.isOnline() || !this.player.isValid())
            return false;

        int newPing = getNewPing();
        boolean update = newPing != ping;
        this.ping = newPing;
        return update;
    }

    @Override
    public boolean updateSkin() {
        if (!this.player.isOnline() || !this.player.isValid())
            return false;

        Skin newSkin = this.skinProvider.get(this.player);
        boolean update = this.skin == null || !newSkin.equals(this.skin);
        this.skin = newSkin;
        return update;
    }

    private int getNewPing() {
        return PacketEvents.getAPI().getPlayerManager().getPing(player);
    }

    private static final PlayerProvider<String> NAME_PROVIDER = HumanEntity::getName;

    private static final PlayerProvider<String> DISPLAY_NAME_PROVIDER = Player::getDisplayName;

    private static final PlayerProvider<String> LIST_NAME_PROVIDER = Player::getPlayerListName;

    private static final PlayerProvider<Skin> SKIN_PROVIDER = Skins::getPlayer;

    public interface PlayerProvider<T> {
        T get(Player player);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PlayerTabItem))
            return false;
        PlayerTabItem other = (PlayerTabItem) object;
        return this.text.equals(other.getText()) && this.skin.equals(other.getSkin()) && this.ping == other.getPing();
    }

    public Player getPlayer() {
        return player;
    }

    public String getText() {
        return text;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    @Override
    public int getPing() {
        return ping;
    }
}

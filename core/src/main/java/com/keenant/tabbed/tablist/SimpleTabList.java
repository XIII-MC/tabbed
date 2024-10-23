package com.keenant.tabbed.tablist;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.google.common.base.Preconditions;
import com.keenant.tabbed.Tabbed;
import com.keenant.tabbed.item.TabItem;
import com.keenant.tabbed.util.Packets;
import com.keenant.tabbed.util.Skin;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Map.Entry;

/**
 * A simple implementation of a custom tab list that supports batch updates.
 */
public class SimpleTabList extends TitledTabList implements CustomTabList {
    public static int MAXIMUM_ITEMS = 4 * 20; // client maximum is 4x20 (4 columns, 20 rows)

    protected final Tabbed tabbed;
    protected final Map<Integer,TabItem> items;
    private final int maxItems;
    private final int minColumnWidth;
    private final int maxColumnWidth;

    private boolean batchEnabled;
    private final Map<Integer,TabItem> clientItems;

    private static final Map<Skin, Map<Integer, UserProfile>> PROFILE_INDEX_CACHE = new HashMap<>();

    public SimpleTabList(Tabbed tabbed, Player player, int maxItems, int minColumnWidth, int maxColumnWidth) {
        super(player);
        Preconditions.checkArgument(maxItems <= MAXIMUM_ITEMS, "maxItems cannot exceed client maximum of " + MAXIMUM_ITEMS);
        Preconditions.checkArgument(minColumnWidth <= maxColumnWidth || maxColumnWidth < 0, "minColumnWidth cannot be greater than maxColumnWidth");

        this.tabbed = tabbed;
        this.maxItems = maxItems < 0 ? MAXIMUM_ITEMS : maxItems;
        this.minColumnWidth = minColumnWidth;
        this.maxColumnWidth = maxColumnWidth;
        this.clientItems = new HashMap<>();
        this.items = new HashMap<>();
    }

    public int getMaxItems() {
        return maxItems;
    }

    @Override
    public SimpleTabList enable() {
        super.enable();
        return this;
    }

    @Override
    public SimpleTabList disable() {
        super.disable();
        return this;
    }

    /**
     * Sends the batch update to the player and resets the batch.
     */
    public void batchUpdate() {
        update(this.clientItems, this.items, true);
        this.clientItems.clear();
        this.clientItems.putAll(this.items);
    }

    /**
     * Reset the existing batch.
     */
    public void batchReset() {
        this.items.clear();
        this.items.putAll(this.clientItems);
    }

    /**
     * Enable batch processing of tab items. Modifications to the tab list
     * will not be sent to the client until {@link #batchUpdate()} is called.
     * @param batchEnabled
     */
    public void setBatchEnabled(boolean batchEnabled) {
        if (this.batchEnabled == batchEnabled)
            return;
        this.batchEnabled = batchEnabled;
        this.clientItems.clear();

        if (this.batchEnabled)
            this.clientItems.putAll(this.items);
    }

    public void add(TabItem item) {
        set(getNextIndex(), item);
    }

    public void add(int index, TabItem item) {
        validateIndex(index);
        Map<Integer,TabItem> current = new HashMap<>();
        current.putAll(this.items);

        Map<Integer,TabItem> map = new HashMap<>();
        for (int i = index; i < getMaxItems(); i++) {
            if (!contains(i))
                break;
            TabItem move = get(i);
            map.put(i + 1, move);
        }
        map.put(index, item);
        update(current, map);
    }

    public TabItem set(int index, TabItem item) {
        Map<Integer,TabItem> items = new HashMap<>(1);
        items.put(index, item);
        return set(items).get(index);
    }

    public Map<Integer,TabItem> set(Map<Integer,TabItem> items) {
        for (Entry<Integer,TabItem> entry : items.entrySet())
            validateIndex(entry.getKey());

        Map<Integer, TabItem> oldItems = new HashMap<>(this.items);
        update(oldItems, items);
        return oldItems;
    }

    public TabItem remove(int index) {
        validateIndex(index);
        TabItem removed = this.items.remove(index);
        update(index, removed, null);
        return removed;
    }

    public <T extends TabItem> T remove(T item) {
        Iterator<Entry<Integer,TabItem>> iterator = this.items.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<Integer,TabItem> entry = iterator.next();
            if (entry.getValue().equals(item))
                remove(entry.getKey());
        }
        return item;
    }

    public boolean contains(int index) {
        validateIndex(index);
        return this.items.containsKey(index);
    }

    public TabItem get(int index) {
        validateIndex(index);
        return this.items.get(index);
    }

    public void update() {
        update(this.items, this.items);
    }

    public void update(int index) {
        Map<Integer,TabItem> map = new HashMap<>();
        map.put(index, get(index));
        update(index, get(index), get(index));
    }

    public int getNextIndex() {
        for (int index = 0; index < getMaxItems(); index++) {
            if (!contains(index))
                return index;
        }
        // tablist is full
        return -1;
    }

    protected void update(int index, TabItem oldItem, TabItem newItem) {
        Map<Integer,TabItem> oldItems = new HashMap<>(1);
        oldItems.put(index, oldItem);

        Map<Integer,TabItem> newItems = new HashMap<>(1);
        newItems.put(index, newItem);

        update(oldItems, newItems);
    }

    protected void update(Map<Integer,TabItem> oldItems, Map<Integer,TabItem> items) {
        update(oldItems, items, false);
    }

    private void validateIndex(int index) {
        Preconditions.checkArgument(index > 0 || index < getMaxItems(), "index not in allowed range");
    }

    private boolean put(int index, TabItem item) {
        if (index < 0 || index >= getMaxItems())
            return false;
        if (item == null) {
            this.items.remove(index);
            return true;
        }
        this.items.put(index, item);
        return true;
    }

    private Map<Integer,TabItem> putAll(Map<Integer,TabItem> items) {
        HashMap<Integer,TabItem> result = new HashMap<>(items.size());
        for (Entry<Integer,TabItem> entry : items.entrySet())
            if (put(entry.getKey(), entry.getValue()))
                result.put(entry.getKey(), entry.getValue());
        return result;
    }

    private void update(Map<Integer,TabItem> oldItems, Map<Integer,TabItem> items, boolean isBatch) {

        if (this.batchEnabled && !isBatch) {
            this.items.putAll(items);
            return;
        }

        Map<Integer,TabItem> newItems = putAll(items);

        Packets.send(this.player, getUpdate(oldItems, newItems));
    }

    private List<PacketWrapper<?>> getUpdate(Map<Integer,TabItem> oldItems, Map<Integer,TabItem> newItems) {
        List<PacketWrapper<?>> removePlayer = new ArrayList<>();
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> addPlayer = new ArrayList<>();
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> displayChanged = new ArrayList<>();
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> pingUpdated = new ArrayList<>();

        for (Entry<Integer, TabItem> entry : newItems.entrySet()) {
            int index = entry.getKey();
            TabItem oldItem = oldItems.get(index);
            TabItem newItem = entry.getValue();

            if (newItem == null && oldItem != null) { // TabItem has been removed.
                removePlayer.add(getPlayerInfoData(index, oldItem));
                continue;
            }

            boolean skinChanged = oldItem == null || newItem.updateSkin() || !newItem.getSkin().equals(oldItem.getSkin());
            boolean textChanged = oldItem == null || newItem.updateText() || !newItem.getText().equals(oldItem.getText());
            boolean pingChanged = oldItem == null || newItem.updatePing() || oldItem.getPing() != newItem.getPing();

            if (skinChanged) {
                if (oldItem != null)
                    removePlayer.add(getPlayerInfoData(index, oldItem));
                addPlayer.add(getPlayerInfoUpdateData(index, newItem));
            } else if (pingChanged) {
                pingUpdated.add(getPlayerInfoUpdateData(index, newItem));
            }

            if (textChanged)
                displayChanged.add(getPlayerInfoUpdateData(index, newItem));
        }

        List<PacketWrapper<?>> result = new ArrayList<>(4);

        if (removePlayer != null || addPlayer.size() > 0) {
            if (removePlayer != null)
                result.addAll(removePlayer);
            result.add(Packets.getPacketUpdate(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER, addPlayer));
        }
        if (displayChanged.size() > 0)
            result.add(Packets.getPacketUpdate(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME, displayChanged));
        if (pingUpdated.size() > 0)
            result.add(Packets.getPacketUpdate(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY, pingUpdated));

        return result;
    }

    private WrapperPlayServerPlayerInfoRemove getPlayerInfoData(int index, TabItem item) {
        UserProfile profile = getGameProfile(index, item);
        return getPlayerInfoData(profile, item.getPing(), item.getText());
    }

    private WrapperPlayServerPlayerInfoRemove getPlayerInfoData(UserProfile profile, int ping, String displayName) {
        if (displayName != null) {
            // min width
            while (displayName.length() < this.minColumnWidth)
                displayName += " ";

            // max width
            if (this.maxColumnWidth > 0)
                while (displayName.length() > this.maxColumnWidth)
                    displayName = displayName.substring(0, displayName.length() - 1);
        }

        return new WrapperPlayServerPlayerInfoRemove(profile.getUUID());
    }

    private WrapperPlayServerPlayerInfoUpdate.PlayerInfo getPlayerInfoUpdateData(int index, TabItem item) {
        UserProfile profile = getGameProfile(index, item);
        return getPlayerInfoUpdateData(profile, item.getPing(), item.getText());
    }

    private WrapperPlayServerPlayerInfoUpdate.PlayerInfo getPlayerInfoUpdateData(UserProfile profile, int ping, String displayName) {
        if (displayName != null) {
            // min width
            while (displayName.length() < this.minColumnWidth)
                displayName += " ";

            // max width
            if (this.maxColumnWidth > 0)
                while (displayName.length() > this.maxColumnWidth)
                    displayName = displayName.substring(0, displayName.length() - 1);
        }

        return new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(profile, true, ping, GameMode.ADVENTURE, displayName == null ? null : AdventureSerializer.fromLegacyFormat(displayName), null);
    }

    private UserProfile getGameProfile(int index, TabItem item) {
        final Skin skin = item.getSkin();
        if (!PROFILE_INDEX_CACHE.containsKey(skin)) // Cached by skins, so if you change the skins a lot, it still works while being efficient.
            PROFILE_INDEX_CACHE.put(skin, new HashMap<>());
        final Map<Integer, UserProfile> indexCache = PROFILE_INDEX_CACHE.get(skin);

        if (!indexCache.containsKey(index)) { // Profile is not cached, generate and cache one.
            final String name = String.format("%03d", index) + "|UpdateMC"; // Starts with 00 so they are sorted in alphabetical order and appear in the right order.
            final UUID uuid = UUID.nameUUIDFromBytes(name.getBytes());

            final UserProfile profile = new UserProfile(uuid, name); // Create a profile to cache by skin and index.
            final List<TextureProperty> listTextureProperty = new ArrayList<>();
            listTextureProperty.add(item.getSkin().getProperty());
            profile.setTextureProperties(listTextureProperty);
            indexCache.put(index, profile); // Cache the profile.
        }

        return indexCache.get(index);
    }

    public boolean getBatchEnabled() {
        return batchEnabled;
    }
}

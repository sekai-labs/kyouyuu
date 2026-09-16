package com.sekailabs.kyouyuu.inventory;

import com.sekailabs.kyouyuu.model.Channel;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

public class ChannelInventoryHolder implements InventoryHolder {

    private final Channel channel;
    private Inventory inventory;

    public ChannelInventoryHolder(Channel channel) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getChannelId() {
        return channel.id();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

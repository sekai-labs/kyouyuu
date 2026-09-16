package com.sekailabs.kyouyuu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedChest {
    @NonNull
    private String channelId;
    @NonNull
    private String worldName;
    private UUID worldUid;
    private int x;
    private int y;
    private int z;

    public String channelId() {
        return channelId;
    }

    public String worldName() {
        return worldName;
    }

    public UUID worldUid() {
        return worldUid;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public ChestLocation toLocation() {
        return new ChestLocation(worldName, x, y, z);
    }
}

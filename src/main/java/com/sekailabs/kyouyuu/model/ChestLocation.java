package com.sekailabs.kyouyuu.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChestLocation {
    @NonNull
    private String worldName;
    private int x;
    private int y;
    private int z;

    public String worldName() {
        return worldName;
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

    @Override
    public String toString() {
        return worldName + "[" + x + "," + y + "," + z + "]";
    }
}

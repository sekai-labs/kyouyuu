package com.sekailabs.kyouyuu.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Channel {
    @NonNull
    private String id;
    @NonNull
    private String name;
    private int size;
    @NonNull
    private Instant createdAt;
    @NonNull
    private Instant updatedAt;

    public static Channel create(String id, String name, int size) {
        if (id == null) throw new NullPointerException("id must not be null");
        if (name == null) throw new NullPointerException("name must not be null");
        if (size <= 0 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Channel size must be a multiple of 9 between 9 and 54, got: " + size);
        }
        Instant now = Instant.now();
        return new Channel(id.toLowerCase().trim(), name.trim(), size, now, now);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int size() {
        return size;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Channel withSize(int newSize) {
        return toBuilder().size(newSize).updatedAt(Instant.now()).build();
    }

    public Channel withName(String newName) {
        return toBuilder().name(newName.trim()).updatedAt(Instant.now()).build();
    }

    public Channel touch() {
        return toBuilder().updatedAt(Instant.now()).build();
    }
}

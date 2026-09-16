package com.sekailabs.kyouyuu.inventory;

import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ItemSerializer {

    private static final byte FORMAT_VERSION = 1;
    private static final byte EMPTY_SLOT_MARKER = 0x00;
    private static final byte OCCUPIED_SLOT_MARKER = 0x01;

    public interface ItemCodec {
        byte[] serialize(ItemStack item) throws Exception;
        ItemStack deserialize(byte[] bytes) throws Exception;
    }

    public static final ItemCodec PAPER_CODEC = new ItemCodec() {
        @Override
        public byte[] serialize(ItemStack item) {
            return item.serializeAsBytes();
        }

        @Override
        public ItemStack deserialize(byte[] bytes) {
            return ItemStack.deserializeBytes(bytes);
        }
    };

    private final ItemCodec codec;

    public ItemSerializer() {
        this(PAPER_CODEC);
    }

    public ItemSerializer(ItemCodec codec) {
        this.codec = codec;
    }

    public byte[] serialize(ItemStack[] contents) throws IOException {
        if (contents == null) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeByte(FORMAT_VERSION);
            dos.writeInt(contents.length);

            for (ItemStack item : contents) {
                if (isSlotEmpty(item)) {
                    dos.writeByte(EMPTY_SLOT_MARKER);
                } else {
                    try {
                        byte[] itemBytes = codec.serialize(item);
                        dos.writeByte(OCCUPIED_SLOT_MARKER);
                        dos.writeInt(itemBytes.length);
                        dos.write(itemBytes);
                    } catch (Exception e) {
                        throw new IOException("Failed to serialize item: " + item, e);
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    public ItemStack[] deserialize(byte[] data, int targetSize) throws IOException {
        ItemStack[] items = new ItemStack[targetSize];
        if (data == null || data.length == 0) {
            return items;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (DataInputStream dis = new DataInputStream(bais)) {
            byte version = dis.readByte();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported inventory format version: " + version);
            }

            int savedSlotCount = dis.readInt();
            for (int i = 0; i < savedSlotCount; i++) {
                byte marker = dis.readByte();
                if (marker == OCCUPIED_SLOT_MARKER) {
                    int len = dis.readInt();
                    byte[] itemBytes = new byte[len];
                    dis.readFully(itemBytes);

                    if (i < targetSize) {
                        try {
                            items[i] = codec.deserialize(itemBytes);
                        } catch (Exception e) {
                            throw new IOException("Failed to deserialize item at slot " + i, e);
                        }
                    }
                }
            }
        }
        return items;
    }

    public boolean hasItemsBeyondSlot(byte[] data, int slotThreshold) throws IOException {
        if (data == null || data.length == 0) {
            return false;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (DataInputStream dis = new DataInputStream(bais)) {
            byte version = dis.readByte();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported inventory format version: " + version);
            }

            int savedSlotCount = dis.readInt();
            for (int i = 0; i < savedSlotCount; i++) {
                byte marker = dis.readByte();
                if (marker == OCCUPIED_SLOT_MARKER) {
                    int len = dis.readInt();
                    dis.skipBytes(len);
                    if (i >= slotThreshold) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasAnyItems(ItemStack[] items) {
        if (items == null) return false;
        for (ItemStack item : items) {
            if (!isSlotEmpty(item)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSlotEmpty(ItemStack item) {
        if (item == null) {
            return true;
        }
        if (item.getAmount() <= 0) {
            return true;
        }
        try {
            return item.getType().name().endsWith("AIR");
        } catch (Throwable ignored) {
            return false;
        }
    }
}

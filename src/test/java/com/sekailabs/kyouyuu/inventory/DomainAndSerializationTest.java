package com.sekailabs.kyouyuu.inventory;

import com.sekailabs.kyouyuu.model.Channel;
import com.sekailabs.kyouyuu.model.ChestLocation;
import com.sekailabs.kyouyuu.model.LinkedChest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DomainAndSerializationTest {

    @Test
    void testChannelValidation() {
        assertDoesNotThrow(() -> Channel.create("valid-id", "Valid Name", 9));
        assertDoesNotThrow(() -> Channel.create("valid_123", "Valid Name", 54));

        assertThrows(IllegalArgumentException.class, () -> Channel.create("id", "Name", 7));
        assertThrows(IllegalArgumentException.class, () -> Channel.create("id", "Name", 0));
        assertThrows(IllegalArgumentException.class, () -> Channel.create("id", "Name", 63));
        assertThrows(NullPointerException.class, () -> Channel.create(null, "Name", 27));
    }

    @Test
    void testLinkedChestAndLocation() {
        UUID uid = UUID.randomUUID();
        LinkedChest chest = new LinkedChest("main", "world_nether", uid, 50, 70, -30);
        assertEquals("main", chest.channelId());
        assertEquals("world_nether", chest.worldName());
        assertEquals(uid, chest.worldUid());

        ChestLocation loc = chest.toLocation();
        assertEquals("world_nether", loc.worldName());
        assertEquals(50, loc.x());
        assertEquals(70, loc.y());
        assertEquals(-30, loc.z());
        assertEquals("world_nether[50,70,-30]", loc.toString());
    }

    @Test
    void testItemSerializerRoundtripWithCustomCodec() throws IOException {
        ItemSerializer.ItemCodec testCodec = new ItemSerializer.ItemCodec() {
            @Override
            public byte[] serialize(ItemStack item) {
                return (item.getType().name() + ":" + item.getAmount()).getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public ItemStack deserialize(byte[] bytes) {
                String str = new String(bytes, StandardCharsets.UTF_8);
                String[] parts = str.split(":");
                ItemStack mock = Mockito.mock(ItemStack.class);
                Material mat = Material.valueOf(parts[0]);
                when(mock.getType()).thenReturn(mat);
                when(mock.getAmount()).thenReturn(Integer.parseInt(parts[1]));
                return mock;
            }
        };

        ItemSerializer serializer = new ItemSerializer(testCodec);

        ItemStack item1 = Mockito.mock(ItemStack.class);
        when(item1.getType()).thenReturn(Material.DIAMOND);
        when(item1.getAmount()).thenReturn(64);

        ItemStack item2 = Mockito.mock(ItemStack.class);
        when(item2.getType()).thenReturn(Material.GOLD_INGOT);
        when(item2.getAmount()).thenReturn(32);

        ItemStack[] original = new ItemStack[9];
        original[0] = item1;
        original[1] = null; 
        original[2] = item2;

        byte[] serialized = serializer.serialize(original);
        assertNotNull(serialized);
        assertTrue(serialized.length > 0);

        ItemStack[] restored = serializer.deserialize(serialized, 9);
        assertEquals(9, restored.length);
        assertNotNull(restored[0]);
        assertEquals(Material.DIAMOND, restored[0].getType());
        assertEquals(64, restored[0].getAmount());

        assertNull(restored[1]);

        assertNotNull(restored[2]);
        assertEquals(Material.GOLD_INGOT, restored[2].getType());
        assertEquals(32, restored[2].getAmount());

        assertFalse(serializer.hasItemsBeyondSlot(serialized, 3));
        assertTrue(serializer.hasItemsBeyondSlot(serialized, 2));
    }

    @Test
    void testChannelMethods() throws InterruptedException {
        Channel channel = Channel.create("test-channel", "Original Name", 27);
        assertEquals("test-channel", channel.id());
        assertEquals("Original Name", channel.name());
        assertEquals(27, channel.size());
        assertNotNull(channel.createdAt());
        assertNotNull(channel.updatedAt());

        Channel withName = channel.withName("New Name");
        assertEquals("test-channel", withName.id());
        assertEquals("New Name", withName.name());
        assertEquals(27, withName.size());
        assertEquals(channel.createdAt(), withName.createdAt());
        assertTrue(!withName.updatedAt().isBefore(channel.updatedAt()));

        Channel withSize = channel.withSize(54);
        assertEquals("test-channel", withSize.id());
        assertEquals("Original Name", withSize.name());
        assertEquals(54, withSize.size());
        assertEquals(channel.createdAt(), withSize.createdAt());
        assertTrue(!withSize.updatedAt().isBefore(channel.updatedAt()));

        Channel touched = channel.touch();
        assertEquals(channel.id(), touched.id());
        assertEquals(channel.name(), touched.name());
        assertEquals(channel.size(), touched.size());
        assertEquals(channel.createdAt(), touched.createdAt());
        assertTrue(!touched.updatedAt().isBefore(channel.updatedAt()));

        Instant now = Instant.now();
        Channel c1 = new Channel("chan", "Chan", 18, now, now);
        Channel c2 = new Channel("chan", "Chan", 18, now, now);
        Channel c3 = new Channel("other", "Chan", 18, now, now);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);
        assertNotEquals(c1, null);
        assertNotEquals(c1, "chan");

        String str = c1.toString();
        assertTrue(str.contains("chan"));
        assertTrue(str.contains("Chan"));
        assertTrue(str.contains("18"));
    }

    @Test
    void testItemSerializerSlotChecks() {
        assertTrue(ItemSerializer.isSlotEmpty(null));

        ItemStack emptyAmountItem = Mockito.mock(ItemStack.class);
        when(emptyAmountItem.getAmount()).thenReturn(0);
        assertTrue(ItemSerializer.isSlotEmpty(emptyAmountItem));

        ItemStack negativeAmountItem = Mockito.mock(ItemStack.class);
        when(negativeAmountItem.getAmount()).thenReturn(-1);
        assertTrue(ItemSerializer.isSlotEmpty(negativeAmountItem));

        ItemStack airItem = Mockito.mock(ItemStack.class);
        when(airItem.getAmount()).thenReturn(1);
        when(airItem.getType()).thenReturn(Material.AIR);
        assertTrue(ItemSerializer.isSlotEmpty(airItem));

        ItemStack caveAirItem = Mockito.mock(ItemStack.class);
        when(caveAirItem.getAmount()).thenReturn(1);
        when(caveAirItem.getType()).thenReturn(Material.CAVE_AIR);
        assertTrue(ItemSerializer.isSlotEmpty(caveAirItem));

        ItemStack voidAirItem = Mockito.mock(ItemStack.class);
        when(voidAirItem.getAmount()).thenReturn(1);
        when(voidAirItem.getType()).thenReturn(Material.VOID_AIR);
        assertTrue(ItemSerializer.isSlotEmpty(voidAirItem));

        ItemStack stoneItem = Mockito.mock(ItemStack.class);
        when(stoneItem.getAmount()).thenReturn(5);
        when(stoneItem.getType()).thenReturn(Material.STONE);
        assertFalse(ItemSerializer.isSlotEmpty(stoneItem));

        ItemStack exceptionItem = Mockito.mock(ItemStack.class);
        when(exceptionItem.getAmount()).thenReturn(1);
        when(exceptionItem.getType()).thenThrow(new RuntimeException("Error"));
        assertFalse(ItemSerializer.isSlotEmpty(exceptionItem));

        assertFalse(ItemSerializer.hasAnyItems(null));
        assertFalse(ItemSerializer.hasAnyItems(new ItemStack[0]));
        assertFalse(ItemSerializer.hasAnyItems(new ItemStack[]{null, null}));
        assertFalse(ItemSerializer.hasAnyItems(new ItemStack[]{airItem, emptyAmountItem, null}));

        assertTrue(ItemSerializer.hasAnyItems(new ItemStack[]{null, stoneItem, null}));
        assertTrue(ItemSerializer.hasAnyItems(new ItemStack[]{stoneItem}));
    }

    @Test
    void testItemSerializerEdgeCases() throws IOException {
        ItemSerializer serializer = new ItemSerializer(new ItemSerializer.ItemCodec() {
            @Override
            public byte[] serialize(ItemStack item) {
                return new byte[0];
            }

            @Override
            public ItemStack deserialize(byte[] bytes) {
                return null;
            }
        });

        byte[] emptySerialized = serializer.serialize(null);
        assertNotNull(emptySerialized);
        assertEquals(0, emptySerialized.length);

        ItemStack[] emptyDeserialized = serializer.deserialize(null, 9);
        assertEquals(9, emptyDeserialized.length);
        assertNull(emptyDeserialized[0]);

        ItemStack[] zeroLengthDeserialized = serializer.deserialize(new byte[0], 9);
        assertEquals(9, zeroLengthDeserialized.length);
        assertNull(zeroLengthDeserialized[0]);

        assertFalse(serializer.hasItemsBeyondSlot(null, 0));
        assertFalse(serializer.hasItemsBeyondSlot(new byte[0], 0));

        ItemSerializer failingSerializer = new ItemSerializer(new ItemSerializer.ItemCodec() {
            @Override
            public byte[] serialize(ItemStack item) throws Exception {
                throw new Exception("Serialization error");
            }

            @Override
            public ItemStack deserialize(byte[] bytes) {
                return null;
            }
        });

        ItemStack realItem = Mockito.mock(ItemStack.class);
        when(realItem.getAmount()).thenReturn(1);
        when(realItem.getType()).thenReturn(Material.DIAMOND);

        assertThrows(IOException.class, () -> failingSerializer.serialize(new ItemStack[]{realItem}));

        byte[] invalidVersionData = new byte[]{99, 0, 0, 0, 1, 0};
        assertThrows(IOException.class, () -> serializer.deserialize(invalidVersionData, 9));
        assertThrows(IOException.class, () -> serializer.hasItemsBeyondSlot(invalidVersionData, 0));
    }
}

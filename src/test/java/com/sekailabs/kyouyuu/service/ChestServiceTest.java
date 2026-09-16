package com.sekailabs.kyouyuu.service;

import com.sekailabs.kyouyuu.model.ChestLocation;
import com.sekailabs.kyouyuu.model.LinkedChest;
import com.sekailabs.kyouyuu.storage.ChestRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChestServiceTest {

    private ChestRepository chestRepository;
    private Logger logger;
    private ChestService chestService;

    @BeforeEach
    void setUp() {
        chestRepository = mock(ChestRepository.class);
        logger = Logger.getLogger("ChestServiceTest");
        chestService = new ChestService(chestRepository, logger);
    }

    @Test
    void testConstructorWithNullLogger() {
        ChestService service = new ChestService(chestRepository, null);
        assertNotNull(service);
    }

    @Test
    void testIsLinkableContainer() {
        assertFalse(chestService.isLinkableContainer(null));

        Block chestBlock = mock(Block.class);
        when(chestBlock.getType()).thenReturn(Material.CHEST);
        assertTrue(chestService.isLinkableContainer(chestBlock));

        Block trappedChestBlock = mock(Block.class);
        when(trappedChestBlock.getType()).thenReturn(Material.TRAPPED_CHEST);
        assertTrue(chestService.isLinkableContainer(trappedChestBlock));

        Block barrelBlock = mock(Block.class);
        when(barrelBlock.getType()).thenReturn(Material.BARREL);
        assertTrue(chestService.isLinkableContainer(barrelBlock));

        Block stoneBlock = mock(Block.class);
        when(stoneBlock.getType()).thenReturn(Material.STONE);
        assertFalse(chestService.isLinkableContainer(stoneBlock));
    }

    @Test
    void testResolveContainerLocationsNull() {
        List<ChestLocation> locations = chestService.resolveContainerLocations(null);
        assertNotNull(locations);
        assertTrue(locations.isEmpty());
    }

    @Test
    void testResolveContainerLocationsSingleChest() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest chestState = mock(Chest.class);
        Inventory inventory = mock(Inventory.class);
        InventoryHolder singleHolder = mock(InventoryHolder.class);
        when(inventory.getHolder()).thenReturn(singleHolder);
        when(chestState.getInventory()).thenReturn(inventory);
        when(block.getState()).thenReturn(chestState);

        List<ChestLocation> locations = chestService.resolveContainerLocations(block);
        assertEquals(1, locations.size());
        assertEquals(new ChestLocation("world", 10, 64, 20), locations.get(0));
    }

    @Test
    void testResolveContainerLocationsSingleChestNullWorld() {
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(null);
        when(block.getLocation()).thenReturn(new Location(null, 15, 70, -30));

        Chest chestState = mock(Chest.class);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(null);
        when(chestState.getInventory()).thenReturn(inventory);
        when(block.getState()).thenReturn(chestState);

        List<ChestLocation> locations = chestService.resolveContainerLocations(block);
        assertEquals(1, locations.size());
        assertEquals(new ChestLocation("world", 15, 70, -30), locations.get(0));
    }

    @Test
    void testResolveContainerLocationsBarrel() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("nether");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 5, 100, 15));

        BlockState state = mock(BlockState.class);
        when(block.getState()).thenReturn(state);

        List<ChestLocation> locations = chestService.resolveContainerLocations(block);
        assertEquals(1, locations.size());
        assertEquals(new ChestLocation("nether", 5, 100, 15), locations.get(0));
    }

    @Test
    void testResolveContainerLocationsDoubleChest() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest chestState = mock(Chest.class);
        Inventory inventory = mock(Inventory.class);
        DoubleChest doubleChest = mock(DoubleChest.class);

        Chest leftChest = mock(Chest.class);
        when(leftChest.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest rightChest = mock(Chest.class);
        when(rightChest.getLocation()).thenReturn(new Location(world, 11, 64, 20));

        when(doubleChest.getLeftSide()).thenReturn(leftChest);
        when(doubleChest.getRightSide()).thenReturn(rightChest);

        when(inventory.getHolder()).thenReturn(doubleChest);
        when(chestState.getInventory()).thenReturn(inventory);
        when(block.getState()).thenReturn(chestState);

        List<ChestLocation> locations = chestService.resolveContainerLocations(block);
        assertEquals(2, locations.size());
        assertEquals(new ChestLocation("world", 10, 64, 20), locations.get(0));
        assertEquals(new ChestLocation("world", 11, 64, 20), locations.get(1));
    }

    @Test
    void testResolveContainerLocationsDoubleChestOneSideNull() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest chestState = mock(Chest.class);
        Inventory inventory = mock(Inventory.class);
        DoubleChest doubleChest = mock(DoubleChest.class);

        Chest leftChest = mock(Chest.class);
        when(leftChest.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        when(doubleChest.getLeftSide()).thenReturn(leftChest);
        when(doubleChest.getRightSide()).thenReturn(null);

        when(inventory.getHolder()).thenReturn(doubleChest);
        when(chestState.getInventory()).thenReturn(inventory);
        when(block.getState()).thenReturn(chestState);

        List<ChestLocation> locations = chestService.resolveContainerLocations(block);
        assertEquals(1, locations.size());
        assertEquals(new ChestLocation("world", 10, 64, 20), locations.get(0));
    }

    @Test
    void testLinkChestNonLinkableBlock() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.DIRT);

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("vault", block);
        assertFalse(result.success());
        assertTrue(result.message().contains("not a valid container"));
    }

    @Test
    void testLinkChestAlreadyLinkedToSameChannel() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        UUID worldUid = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldUid);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        LinkedChest existing = new LinkedChest("vault", "world", worldUid, 10, 64, 20);
        when(chestRepository.findByLocation(new ChestLocation("world", 10, 64, 20)))
                .thenReturn(Optional.of(existing));

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("vault", block);
        assertFalse(result.success());
        assertTrue(result.message().contains("already linked to channel 'vault'"));
        verify(chestRepository, never()).link(any());
    }

    @Test
    void testLinkChestAlreadyLinkedToDifferentChannel() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        UUID worldUid = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldUid);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        LinkedChest existing = new LinkedChest("other", "world", worldUid, 10, 64, 20);
        when(chestRepository.findByLocation(new ChestLocation("world", 10, 64, 20)))
                .thenReturn(Optional.of(existing));

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("vault", block);
        assertFalse(result.success());
        assertTrue(result.message().contains("already linked to another channel ('other')"));
        verify(chestRepository, never()).link(any());
    }

    @Test
    void testLinkChestDatabaseErrorOnCheck() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        when(chestRepository.findByLocation(any(ChestLocation.class)))
                .thenThrow(new SQLException("DB timeout"));

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("vault", block);
        assertFalse(result.success());
        assertTrue(result.message().contains("Database error checking chest link"));
        verify(chestRepository, never()).link(any());
    }

    @Test
    void testLinkChestSuccessfulSingleChest() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        UUID worldUid = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldUid);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        when(chestRepository.findByLocation(new ChestLocation("world", 10, 64, 20)))
                .thenReturn(Optional.empty());

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("Vault", block);
        assertTrue(result.success());
        assertNotNull(result.data());
        assertEquals(1, result.data().size());
        assertEquals("vault", result.data().get(0).channelId());
        assertEquals("world", result.data().get(0).worldName());
        assertEquals(worldUid, result.data().get(0).worldUid());
        assertEquals(10, result.data().get(0).x());
        assertEquals(64, result.data().get(0).y());
        assertEquals(20, result.data().get(0).z());
        assertTrue(result.message().contains("Successfully linked chest to channel 'Vault'"));
        assertFalse(result.message().contains("double chest"));
        verify(chestRepository, times(1)).link(result.data().get(0));
    }

    @Test
    void testLinkChestSuccessfulDoubleChest() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        UUID worldUid = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldUid);

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest chestState = mock(Chest.class);
        Inventory inventory = mock(Inventory.class);
        DoubleChest doubleChest = mock(DoubleChest.class);

        Chest leftChest = mock(Chest.class);
        when(leftChest.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest rightChest = mock(Chest.class);
        when(rightChest.getLocation()).thenReturn(new Location(world, 11, 64, 20));

        when(doubleChest.getLeftSide()).thenReturn(leftChest);
        when(doubleChest.getRightSide()).thenReturn(rightChest);
        when(inventory.getHolder()).thenReturn(doubleChest);
        when(chestState.getInventory()).thenReturn(inventory);
        when(block.getState()).thenReturn(chestState);

        when(chestRepository.findByLocation(any(ChestLocation.class))).thenReturn(Optional.empty());

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("vault", block);
        assertTrue(result.success());
        assertNotNull(result.data());
        assertEquals(2, result.data().size());
        assertTrue(result.message().contains("(double chest: 2 blocks linked)"));
        verify(chestRepository, times(2)).link(any(LinkedChest.class));
    }

    @Test
    void testLinkChestDatabaseErrorOnLink() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        when(chestRepository.findByLocation(any(ChestLocation.class))).thenReturn(Optional.empty());
        doThrow(new SQLException("Insert failed")).when(chestRepository).link(any(LinkedChest.class));

        ChannelService.ServiceResult<List<LinkedChest>> result = chestService.linkChest("vault", block);
        assertFalse(result.success());
        assertTrue(result.message().contains("Database error while linking chest"));
    }

    @Test
    void testUnlinkChestLinkedSingle() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        when(chestRepository.unlink("world", 10, 64, 20)).thenReturn(true);

        ChannelService.ServiceResult<Integer> result = chestService.unlinkChest(block);
        assertTrue(result.success());
        assertEquals(1, result.data());
        assertTrue(result.message().contains("Successfully unlinked chest (1 block)."));
    }

    @Test
    void testUnlinkChestLinkedDouble() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest chestState = mock(Chest.class);
        Inventory inventory = mock(Inventory.class);
        DoubleChest doubleChest = mock(DoubleChest.class);

        Chest leftChest = mock(Chest.class);
        when(leftChest.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        Chest rightChest = mock(Chest.class);
        when(rightChest.getLocation()).thenReturn(new Location(world, 11, 64, 20));

        when(doubleChest.getLeftSide()).thenReturn(leftChest);
        when(doubleChest.getRightSide()).thenReturn(rightChest);
        when(inventory.getHolder()).thenReturn(doubleChest);
        when(chestState.getInventory()).thenReturn(inventory);
        when(block.getState()).thenReturn(chestState);

        when(chestRepository.unlink("world", 10, 64, 20)).thenReturn(true);
        when(chestRepository.unlink("world", 11, 64, 20)).thenReturn(true);

        ChannelService.ServiceResult<Integer> result = chestService.unlinkChest(block);
        assertTrue(result.success());
        assertEquals(2, result.data());
        assertTrue(result.message().contains("Successfully unlinked chest (2 blocks)."));
    }

    @Test
    void testUnlinkChestUnlinked() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        when(chestRepository.unlink("world", 10, 64, 20)).thenReturn(false);

        ChannelService.ServiceResult<Integer> result = chestService.unlinkChest(block);
        assertFalse(result.success());
        assertTrue(result.message().contains("This chest is not linked to any channel"));
    }

    @Test
    void testUnlinkChestSQLException() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));
        when(block.getState()).thenReturn(mock(BlockState.class));

        when(chestRepository.unlink("world", 10, 64, 20)).thenThrow(new SQLException("DB error"));

        ChannelService.ServiceResult<Integer> result = chestService.unlinkChest(block);
        assertFalse(result.success());
        assertTrue(result.message().contains("This chest is not linked to any channel"));
    }

    @Test
    void testUnlinkLocationExisting() throws SQLException {
        when(chestRepository.unlink("world", 10, 64, 20)).thenReturn(true);
        assertTrue(chestService.unlinkLocation("world", 10, 64, 20));
    }

    @Test
    void testUnlinkLocationNonExisting() throws SQLException {
        when(chestRepository.unlink("world", 10, 64, 20)).thenReturn(false);
        assertFalse(chestService.unlinkLocation("world", 10, 64, 20));
    }

    @Test
    void testUnlinkLocationSQLException() throws SQLException {
        when(chestRepository.unlink("world", 10, 64, 20)).thenThrow(new SQLException("error"));
        assertFalse(chestService.unlinkLocation("world", 10, 64, 20));
    }

    @Test
    void testGetLinkedChestNullBlock() {
        Optional<LinkedChest> chest = chestService.getLinkedChest((Block) null);
        assertTrue(chest.isEmpty());
    }

    @Test
    void testGetLinkedChestBlock() throws SQLException {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");

        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(new Location(world, 10, 64, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestRepository.findByLocation("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        Optional<LinkedChest> result = chestService.getLinkedChest(block);
        assertTrue(result.isPresent());
        assertEquals("vault", result.get().channelId());
    }

    @Test
    void testGetLinkedChestBlockNullWorld() throws SQLException {
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(null);
        when(block.getLocation()).thenReturn(new Location(null, 10, 64, 20));

        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestRepository.findByLocation("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        Optional<LinkedChest> result = chestService.getLinkedChest(block);
        assertTrue(result.isPresent());
        assertEquals("vault", result.get().channelId());
    }

    @Test
    void testGetLinkedChestCoordinates() throws SQLException {
        LinkedChest linked = new LinkedChest("vault", "world", null, 10, 64, 20);
        when(chestRepository.findByLocation("world", 10, 64, 20)).thenReturn(Optional.of(linked));

        Optional<LinkedChest> result = chestService.getLinkedChest("world", 10, 64, 20);
        assertTrue(result.isPresent());
        assertEquals("vault", result.get().channelId());

        when(chestRepository.findByLocation("world", 99, 99, 99)).thenReturn(Optional.empty());
        Optional<LinkedChest> emptyResult = chestService.getLinkedChest("world", 99, 99, 99);
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void testGetLinkedChestCoordinatesSQLException() throws SQLException {
        when(chestRepository.findByLocation("world", 10, 64, 20)).thenThrow(new SQLException("error"));
        Optional<LinkedChest> result = chestService.getLinkedChest("world", 10, 64, 20);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetChestsForChannelSuccess() throws SQLException {
        LinkedChest c1 = new LinkedChest("vault", "world", null, 10, 64, 20);
        LinkedChest c2 = new LinkedChest("vault", "world", null, 11, 64, 20);
        when(chestRepository.findByChannel("vault")).thenReturn(List.of(c1, c2));

        List<LinkedChest> result = chestService.getChestsForChannel("vault");
        assertEquals(2, result.size());
    }

    @Test
    void testGetChestsForChannelSQLException() throws SQLException {
        when(chestRepository.findByChannel("vault")).thenThrow(new SQLException("error"));

        List<LinkedChest> result = chestService.getChestsForChannel("vault");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}

package engine;

import entity.DropItem;
import entity.ShopItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import screen.Screen;

import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController; // 추가됨
import java.security.PrivilegedAction; // 추가됨
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S3011")
class ItemHUDManagerTest {

    private ItemHUDManager itemHUDManager;
    private Screen screen;
    private Graphics graphics;

    @BeforeEach
    void setUp() {
        itemHUDManager = ItemHUDManager.getInstance();
        itemHUDManager.clear(); // Clear any state from previous tests
        screen = mock(Screen.class);
        graphics = mock(Graphics.class);
    }

    @Test
    void testGetInstance() {
        assertNotNull(itemHUDManager);
        assertSame(itemHUDManager, ItemHUDManager.getInstance());
    }

    @Test
    void testInitialize() {
        when(screen.getWidth()).thenReturn(800);
        itemHUDManager.initialize(screen);

        try {
            Field startXField = ItemHUDManager.class.getDeclaredField("startX");


            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                startXField.setAccessible(true);
                return null;
            });

            assertEquals(668, startXField.get(itemHUDManager));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access startX field: " + e.getMessage());
        }
    }

    @Test
    void testAddDroppedItem_withSpace() {
        itemHUDManager.addDroppedItem(DropItem.ItemType.Explode);
        List<?> activeDroppedItems = getActiveDroppedItems(itemHUDManager);
        assertEquals(1, activeDroppedItems.size());
        assertEquals(DropItem.ItemType.Explode, getDroppedItemType(activeDroppedItems.get(0)));
    }

    @Test
    void testAddDroppedItem_maxCapacityReached() {
        // Fill up to max capacity
        for (int i = 0; i < 6; i++) {
            itemHUDManager.addDroppedItem(DropItem.ItemType.Explode);
        }
        List<?> activeDroppedItems = getActiveDroppedItems(itemHUDManager);
        assertEquals(6, activeDroppedItems.size());

        // Add one more, oldest should be replaced
        itemHUDManager.addDroppedItem(DropItem.ItemType.Heal);
        assertEquals(6, activeDroppedItems.size());
        assertEquals(DropItem.ItemType.Explode, getDroppedItemType(activeDroppedItems.get(0))); // Oldest Explode still there
        assertEquals(DropItem.ItemType.Heal, getDroppedItemType(activeDroppedItems.get(5))); // Newest Heal added
    }

    @Test
    void testCleanupExpiredItems() throws Exception {
        itemHUDManager.addDroppedItem(DropItem.ItemType.Explode);
        List<?> activeDroppedItems = getActiveDroppedItems(itemHUDManager);
        assertEquals(1, activeDroppedItems.size());

        // Get the DroppedItemInfo object
        Object droppedItemInfo = activeDroppedItems.get(0);

        // Get DROPPED_ITEM_DISPLAY_DURATION using reflection
        Field durationField = ItemHUDManager.class.getDeclaredField("DROPPED_ITEM_DISPLAY_DURATION");


        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            durationField.setAccessible(true);
            return null;
        });

        long duration = (long) durationField.get(null);

        // Manipulate displayStartTime to make the item expired
        Field startTimeField = droppedItemInfo.getClass().getDeclaredField("displayStartTime");


        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            startTimeField.setAccessible(true);
            return null;
        });

        startTimeField.set(droppedItemInfo, System.currentTimeMillis() - duration - 1);

        // Use reflection to call the private cleanupExpiredItems method
        Method cleanupMethod = ItemHUDManager.class.getDeclaredMethod("cleanupExpiredItems");


        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            cleanupMethod.setAccessible(true);
            return null;
        });

        cleanupMethod.invoke(itemHUDManager);

        assertEquals(0, activeDroppedItems.size()); // Explode should be removed

        itemHUDManager.addDroppedItem(DropItem.ItemType.Slow); // Now add the new item
        assertEquals(1, activeDroppedItems.size());
        assertEquals(DropItem.ItemType.Slow, getDroppedItemType(activeDroppedItems.get(0)));
    }

    @Test
    void testDrawItems() {
        when(screen.getWidth()).thenReturn(800);
        itemHUDManager.initialize(screen);

        try (MockedStatic<ShopItem> mockedShopItem = mockStatic(ShopItem.class)) {
            // Setup mock for ShopItem static methods
            mockedShopItem.when(ShopItem::isMultiShotActive).thenReturn(true);
            mockedShopItem.when(ShopItem::getMultiShotLevel).thenReturn(1);
            mockedShopItem.when(ShopItem::getRapidFireLevel).thenReturn(2);
            mockedShopItem.when(ShopItem::isPenetrationActive).thenReturn(false);
            mockedShopItem.when(ShopItem::getPenetrationLevel).thenReturn(0);
            mockedShopItem.when(ShopItem::getBulletSpeedLevel).thenReturn(3);
            mockedShopItem.when(ShopItem::getSHIPSpeedCOUNT).thenReturn(4);

            itemHUDManager.drawItems(screen, graphics);

            verify(graphics, times(4)).setColor(Color.GREEN);
            verify(graphics, times(1)).setColor(Color.DARK_GRAY);
        }
    }

    @Test
    void testPrivateHelperMethods() throws Exception {
        // Test isShopItemActive
        try (MockedStatic<ShopItem> mockedShopItem = mockStatic(ShopItem.class)) {
            Method isShopItemActiveMethod = ItemHUDManager.class.getDeclaredMethod("isShopItemActive", ItemHUDManager.class.getDeclaredClasses()[1]);


            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                isShopItemActiveMethod.setAccessible(true);
                return null;
            });

            mockedShopItem.when(ShopItem::isMultiShotActive).thenReturn(true);
            assertTrue((Boolean) isShopItemActiveMethod.invoke(itemHUDManager, getShopItemTypeEnum("MULTI_SHOT")));

            mockedShopItem.when(ShopItem::getRapidFireLevel).thenReturn(1);
            assertTrue((Boolean) isShopItemActiveMethod.invoke(itemHUDManager, getShopItemTypeEnum("RAPID_FIRE")));

            mockedShopItem.when(ShopItem::isPenetrationActive).thenReturn(true);
            assertTrue((Boolean) isShopItemActiveMethod.invoke(itemHUDManager, getShopItemTypeEnum("PENETRATION")));

            mockedShopItem.when(ShopItem::getBulletSpeedLevel).thenReturn(1);
            assertTrue((Boolean) isShopItemActiveMethod.invoke(itemHUDManager, getShopItemTypeEnum("BULLET_SPEED")));

            mockedShopItem.when(ShopItem::getSHIPSpeedCOUNT).thenReturn(1);
            assertTrue((Boolean) isShopItemActiveMethod.invoke(itemHUDManager, getShopItemTypeEnum("SHIP_SPEED")));
        }

        // Test getShopItemLevel
        try (MockedStatic<ShopItem> mockedShopItem = mockStatic(ShopItem.class)) {
            Method getShopItemLevelMethod = ItemHUDManager.class.getDeclaredMethod("getShopItemLevel", ItemHUDManager.class.getDeclaredClasses()[1]);


            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                getShopItemLevelMethod.setAccessible(true);
                return null;
            });

            mockedShopItem.when(ShopItem::getMultiShotLevel).thenReturn(1);
            assertEquals(1, (int) getShopItemLevelMethod.invoke(itemHUDManager, getShopItemTypeEnum("MULTI_SHOT")));

            mockedShopItem.when(ShopItem::getRapidFireLevel).thenReturn(2);
            assertEquals(2, (int) getShopItemLevelMethod.invoke(itemHUDManager, getShopItemTypeEnum("RAPID_FIRE")));
        }

        // Test getShopItemLetter
        Method getShopItemLetterMethod = ItemHUDManager.class.getDeclaredMethod("getShopItemLetter", ItemHUDManager.class.getDeclaredClasses()[1]);


        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            getShopItemLetterMethod.setAccessible(true);
            return null;
        });

        assertEquals("M", (String) getShopItemLetterMethod.invoke(itemHUDManager, getShopItemTypeEnum("MULTI_SHOT")));
        assertEquals("R", (String) getShopItemLetterMethod.invoke(itemHUDManager, getShopItemTypeEnum("RAPID_FIRE")));
        assertEquals("P", (String) getShopItemLetterMethod.invoke(itemHUDManager, getShopItemTypeEnum("PENETRATION")));
        assertEquals("B", (String) getShopItemLetterMethod.invoke(itemHUDManager, getShopItemTypeEnum("BULLET_SPEED")));
        assertEquals("S", (String) getShopItemLetterMethod.invoke(itemHUDManager, getShopItemTypeEnum("SHIP_SPEED")));

        // Test getDroppedItemLetter
        Method getDroppedItemLetterMethod = ItemHUDManager.class.getDeclaredMethod("getDroppedItemLetter", DropItem.ItemType.class);


        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            getDroppedItemLetterMethod.setAccessible(true);
            return null;
        });

        assertEquals("E", (String) getDroppedItemLetterMethod.invoke(itemHUDManager, DropItem.ItemType.Explode));
        assertEquals("L", (String) getDroppedItemLetterMethod.invoke(itemHUDManager, DropItem.ItemType.Slow));
        assertEquals("T", (String) getDroppedItemLetterMethod.invoke(itemHUDManager, DropItem.ItemType.Stop));
        assertEquals("U", (String) getDroppedItemLetterMethod.invoke(itemHUDManager, DropItem.ItemType.Push));
        assertEquals("H", (String) getDroppedItemLetterMethod.invoke(itemHUDManager, DropItem.ItemType.Shield));
        assertEquals("A", (String) getDroppedItemLetterMethod.invoke(itemHUDManager, DropItem.ItemType.Heal));

        // Test getDroppedItemColor
        Method getDroppedItemColorMethod = ItemHUDManager.class.getDeclaredMethod("getDroppedItemColor", DropItem.ItemType.class);


        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            getDroppedItemColorMethod.setAccessible(true);
            return null;
        });

        assertEquals(Color.RED, (Color) getDroppedItemColorMethod.invoke(itemHUDManager, DropItem.ItemType.Explode));
        assertEquals(Color.BLUE, (Color) getDroppedItemColorMethod.invoke(itemHUDManager, DropItem.ItemType.Slow));
        assertEquals(Color.CYAN, (Color) getDroppedItemColorMethod.invoke(itemHUDManager, DropItem.ItemType.Shield));
        assertEquals(Color.GREEN, (Color) getDroppedItemColorMethod.invoke(itemHUDManager, DropItem.ItemType.Heal));
    }

    private Enum<?> getShopItemTypeEnum(String enumName) throws Exception {
        Class<?> shopItemTypeClass = ItemHUDManager.class.getDeclaredClasses()[1];
        return Enum.valueOf((Class<Enum>)shopItemTypeClass, enumName);
    }

    // Helper to get activeDroppedItems using reflection
    private List<?> getActiveDroppedItems(ItemHUDManager manager) {
        try {
            Field field = ItemHUDManager.class.getDeclaredField("activeDroppedItems");

            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                field.setAccessible(true);
                return null;
            });

            return (List<?>) field.get(manager);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access activeDroppedItems field: " + e.getMessage());
            return null;
        }
    }

    // Helper to get itemType from DroppedItemInfo using reflection
    private DropItem.ItemType getDroppedItemType(Object droppedItemInfo) {
        try {
            Field field = droppedItemInfo.getClass().getDeclaredField("itemType");


            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                field.setAccessible(true);
                return null;
            });

            return (DropItem.ItemType) field.get(droppedItemInfo);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access itemType field: " + e.getMessage());
            return null;
        }
    }
}
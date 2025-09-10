package org.example.service;

import org.example.model.Product;
import org.example.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for InventoryManager
 * Demonstrates testing best practices for inventory management operations
 */
class InventoryManagerTest {

    private InventoryManager inventoryManager;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        inventoryManager = new InventoryManager();
        testProduct = inventoryManager.addProduct(
                "Test Laptop",
                "Electronics",
                50,
                10,
                999.99,
                "TestCorp"
        );
    }

    @Nested
    @DisplayName("Product Management Tests")
    class ProductManagementTests {

        @Test
        @DisplayName("Should add product successfully")
        void shouldAddProductSuccessfully() {
            Product newProduct = inventoryManager.addProduct(
                    "Test Mouse", "Electronics", 100, 20, 29.99, "MouseCorp"
            );

            assertNotNull(newProduct.getId());
            assertEquals("Test Mouse", newProduct.getName());
            assertEquals("Electronics", newProduct.getCategory());
            assertEquals(100, newProduct.getCurrentStock());
            assertEquals(20, newProduct.getMinThreshold());
            assertEquals(29.99, newProduct.getUnitCost());
            assertEquals("MouseCorp", newProduct.getSupplier());
        }

        @Test
        @DisplayName("Should reject invalid product data")
        void shouldRejectInvalidProductData() {
            assertThrows(IllegalArgumentException.class, () ->
                    inventoryManager.addProduct("", "Electronics", 10, 5, 99.99, "TestCorp")
            );

            assertThrows(IllegalArgumentException.class, () ->
                    inventoryManager.addProduct("Valid Name", "", 10, 5, 99.99, "TestCorp")
            );

            assertThrows(IllegalArgumentException.class, () ->
                    inventoryManager.addProduct("Valid Name", "Electronics", -1, 5, 99.99, "TestCorp")
            );

            assertThrows(IllegalArgumentException.class, () ->
                    inventoryManager.addProduct("Valid Name", "Electronics", 10, -1, 99.99, "TestCorp")
            );
        }

        @Test
        @DisplayName("Should retrieve product by ID")
        void shouldRetrieveProductById() {
            Optional<Product> found = inventoryManager.getProduct(testProduct.getId());

            assertTrue(found.isPresent());
            assertEquals(testProduct.getId(), found.get().getId());
            assertEquals("Test Laptop", found.get().getName());
        }

        @Test
        @DisplayName("Should return empty for non-existent product ID")
        void shouldReturnEmptyForNonExistentProductId() {
            Optional<Product> found = inventoryManager.getProduct(999L);
            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should search products by name")
        void shouldSearchProductsByName() {
            inventoryManager.addProduct("Gaming Laptop", "Electronics", 25, 5, 1499.99, "GameCorp");
            inventoryManager.addProduct("Office Chair", "Furniture", 30, 8, 299.99, "ChairCorp");

            List<Product> laptops = inventoryManager.searchProductsByName("laptop");
            assertEquals(2, laptops.size());

            List<Product> gaming = inventoryManager.searchProductsByName("Gaming");
            assertEquals(1, gaming.size());
            assertEquals("Gaming Laptop", gaming.get(0).getName());
        }

        @Test
        @DisplayName("Should get products by category")
        void shouldGetProductsByCategory() {
            inventoryManager.addProduct("Test Monitor", "Electronics", 20, 5, 299.99, "MonitorCorp");
            inventoryManager.addProduct("Test Desk", "Furniture", 15, 3, 199.99, "DeskCorp");

            List<Product> electronics = inventoryManager.getProductsByCategory("Electronics");
            assertEquals(2, electronics.size());

            List<Product> furniture = inventoryManager.getProductsByCategory("Furniture");
            assertEquals(1, furniture.size());
        }
    }

    @Nested
    @DisplayName("Stock Operations Tests")
    class StockOperationsTests {

        @Test
        @DisplayName("Should use stock successfully when sufficient quantity available")
        void shouldUseStockSuccessfully() {
            boolean result = inventoryManager.useStock(
                    testProduct.getId(), 10, "Testing usage", "Test User"
            );

            assertTrue(result);
            assertEquals(40, testProduct.getCurrentStock());
        }

        @Test
        @DisplayName("Should fail to use stock when insufficient quantity")
        void shouldFailToUseStockWhenInsufficientQuantity() {
            boolean result = inventoryManager.useStock(
                    testProduct.getId(), 100, "Testing usage", "Test User"
            );

            assertFalse(result);
            assertEquals(50, testProduct.getCurrentStock()); // Should remain unchanged
        }

        @Test
        @DisplayName("Should sell stock successfully")
        void shouldSellStockSuccessfully() {
            boolean result = inventoryManager.sellStock(
                    testProduct.getId(), 5, 1199.99, "Customer sale", "Sales Rep"
            );

            assertTrue(result);
            assertEquals(45, testProduct.getCurrentStock());
        }

        @Test
        @DisplayName("Should restock product successfully")
        void shouldRestockProductSuccessfully() {
            boolean result = inventoryManager.restockProduct(
                    testProduct.getId(), 20, 18000.0, "Supplier delivery", "Warehouse"
            );

            assertTrue(result);
            assertEquals(70, testProduct.getCurrentStock());
        }

        @Test
        @DisplayName("Should adjust stock manually")
        void shouldAdjustStockManually() {
            boolean result = inventoryManager.adjustStock(
                    testProduct.getId(), 35, "Inventory count correction", "Manager"
            );

            assertTrue(result);
            assertEquals(35, testProduct.getCurrentStock());
        }

        @Test
        @DisplayName("Should reject negative stock adjustment")
        void shouldRejectNegativeStockAdjustment() {
            assertThrows(IllegalArgumentException.class, () ->
                    inventoryManager.adjustStock(testProduct.getId(), -5, "Invalid", "User")
            );
        }
    }

    @Nested
    @DisplayName("Transaction History Tests")
    class TransactionHistoryTests {

        @Test
        @DisplayName("Should record transactions correctly")
        void shouldRecordTransactionsCorrectly() {
            // Perform some operations
            inventoryManager.useStock(testProduct.getId(), 5, "Test usage", "User1");
            inventoryManager.sellStock(testProduct.getId(), 3, 2999.97, "Test sale", "User2");
            inventoryManager.restockProduct(testProduct.getId(), 10, 9999.9, "Test restock", "User3");

            List<Transaction> history = inventoryManager.getTransactionHistory(testProduct.getId());

            // Should have 4 transactions: initial stock + 3 operations
            assertEquals(4, history.size());

            // Check most recent transaction (restock)
            Transaction latest = history.get(0);
            assertEquals(Transaction.TransactionType.RESTOCK, latest.getType());
            assertEquals(10, latest.getQuantity());
            assertEquals(9999.9, latest.getTotalCost());
        }

        @Test
        @DisplayName("Should get recent transactions")
        void shouldGetRecentTransactions() {
            inventoryManager.useStock(testProduct.getId(), 5, "Test usage", "User");

            List<Transaction> recent = inventoryManager.getRecentTransactions(1);
            assertFalse(recent.isEmpty());
            assertTrue(recent.get(0).isRecent());
        }
    }

    @Nested
    @DisplayName("Low Stock Detection Tests")
    class LowStockDetectionTests {

        @Test
        @DisplayName("Should detect low stock products")
        void shouldDetectLowStockProducts() {
            // Reduce stock to below threshold
            inventoryManager.useStock(testProduct.getId(), 45, "Reduce to low stock", "Test");

            List<Product> lowStock = inventoryManager.getLowStockProducts();
            assertEquals(1, lowStock.size());
            assertEquals(testProduct.getId(), lowStock.get(0).getId());
        }

        @Test
        @DisplayName("Should detect out of stock products")
        void shouldDetectOutOfStockProducts() {
            // Reduce stock to zero
            inventoryManager.useStock(testProduct.getId(), 50, "Use all stock", "Test");

            List<Product> outOfStock = inventoryManager.getOutOfStockProducts();
            assertEquals(1, outOfStock.size());
            assertEquals(testProduct.getId(), outOfStock.get(0).getId());
        }
    }

    @Nested
    @DisplayName("Analytics Tests")
    class AnalyticsTests {

        @Test
        @DisplayName("Should generate inventory statistics")
        void shouldGenerateInventoryStatistics() {
            // Add another product
            inventoryManager.addProduct("Test Chair", "Furniture", 20, 5, 199.99, "ChairCorp");

            InventoryManager.InventoryStatistics stats = inventoryManager.getInventoryStatistics();

            assertEquals(2, stats.totalProducts);
            assertTrue(stats.totalValue > 0);
            assertTrue(stats.totalStockUnits > 0);
            assertEquals(2, stats.categoriesCount);
        }

        @Test
        @DisplayName("Should predict reorder timing")
        void shouldPredictReorderTiming() {
            // Create usage history
            inventoryManager.useStock(testProduct.getId(), 5, "Usage 1", "User");
            inventoryManager.useStock(testProduct.getId(), 3, "Usage 2", "User");
            inventoryManager.useStock(testProduct.getId(), 4, "Usage 3", "User");

            int daysUntilReorder = inventoryManager.predictDaysUntilReorder(testProduct.getId());

            // Should return a valid prediction (not MAX_VALUE)
            assertTrue(daysUntilReorder >= 0);
        }

        @Test
        @DisplayName("Should suggest reorder quantities")
        void shouldSuggestReorderQuantities() {
            // Create usage history
            inventoryManager.useStock(testProduct.getId(), 10, "Create history", "User");

            int suggestedQuantity = inventoryManager.suggestReorderQuantity(testProduct.getId());

            // Should suggest a reasonable quantity
            assertTrue(suggestedQuantity > 0);
            assertTrue(suggestedQuantity >= testProduct.getMinThreshold());
        }

        @Test
        @DisplayName("Should generate comprehensive report")
        void shouldGenerateComprehensiveReport() {
            InventoryAnalytics.InventoryReport report = inventoryManager.generateReport();

            assertNotNull(report.reportDate);
            assertTrue(report.totalProducts > 0);
            assertTrue(report.totalValue > 0);
            assertNotNull(report.topCategories);
            assertNotNull(report.recentActivity);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Product should calculate total value correctly")
        void productShouldCalculateTotalValueCorrectly() {
            double expectedValue = testProduct.getCurrentStock() * testProduct.getUnitCost();
            assertEquals(expectedValue, testProduct.getTotalValue(), 0.01);
        }

        @Test
        @DisplayName("Product should detect low stock correctly")
        void productShouldDetectLowStockCorrectly() {
            assertFalse(testProduct.isLowStock()); // 50 > 10

            inventoryManager.useStock(testProduct.getId(), 45, "Reduce stock", "Test");
            assertTrue(testProduct.isLowStock()); // 5 <= 10
        }

        @Test
        @DisplayName("Should get correct stock status")
        void shouldGetCorrectStockStatus() {
            assertEquals("IN STOCK", testProduct.getStockStatus());

            inventoryManager.useStock(testProduct.getId(), 45, "Reduce to low", "Test");
            assertEquals("LOW STOCK", testProduct.getStockStatus());

            inventoryManager.useStock(testProduct.getId(), 5, "Reduce to zero", "Test");
            assertEquals("OUT OF STOCK", testProduct.getStockStatus());
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Should update configuration correctly")
        void shouldUpdateConfigurationCorrectly() {
            inventoryManager.setDefaultLeadTimeDays(14);
            inventoryManager.setDefaultServiceLevel(0.98);

            assertEquals(14, inventoryManager.getDefaultLeadTimeDays());
            assertEquals(0.98, inventoryManager.getDefaultServiceLevel(), 0.001);
        }

        @Test
        @DisplayName("Should enforce configuration bounds")
        void shouldEnforceConfigurationBounds() {
            inventoryManager.setDefaultLeadTimeDays(0); // Should be set to minimum 1
            assertEquals(1, inventoryManager.getDefaultLeadTimeDays());

            inventoryManager.setDefaultServiceLevel(1.5); // Should be capped at 0.99
            assertEquals(0.99, inventoryManager.getDefaultServiceLevel(), 0.001);

            inventoryManager.setDefaultServiceLevel(0.1); // Should be raised to minimum 0.5
            assertEquals(0.5, inventoryManager.getDefaultServiceLevel(), 0.001);
        }
    }
}
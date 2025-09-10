// REST API endpoints

package org.example.controller;

import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.InventoryManager;
import org.example.service.InventoryAnalytics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST API Controller for SmartStock inventory management system
 * Provides endpoints for all inventory operations, analytics, and reporting
 */
@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryManager inventoryManager;

    public InventoryController() {
        this.inventoryManager = new InventoryManager();

        // Initialize with sample data for demonstration
        initializeSampleData();
    }

    // ============================
    // PRODUCT MANAGEMENT ENDPOINTS
    // ============================

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(inventoryManager.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        Optional<Product> product = inventoryManager.getProduct(id);
        return product.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    public ResponseEntity<Product> addProduct(@RequestBody CreateProductRequest request) {
        try {
            Product product = inventoryManager.addProduct(
                    request.name,
                    request.category,
                    request.initialStock,
                    request.minThreshold,
                    request.unitCost,
                    request.supplier
            );
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        try {
            product.setId(id);
            boolean updated = inventoryManager.updateProduct(product);
            if (updated) {
                return ResponseEntity.ok(product);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            boolean deleted = inventoryManager.deleteProduct(id);
            if (deleted) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/products/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(inventoryManager.getProductsByCategory(category));
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        return ResponseEntity.ok(inventoryManager.searchProductsByName(query));
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        return ResponseEntity.ok(inventoryManager.getLowStockProducts());
    }

    @GetMapping("/products/out-of-stock")
    public ResponseEntity<List<Product>> getOutOfStockProducts() {
        return ResponseEntity.ok(inventoryManager.getOutOfStockProducts());
    }

    @GetMapping("/products/reorder-needed")
    public ResponseEntity<List<Product>> getProductsNeedingReorder() {
        return ResponseEntity.ok(inventoryManager.getProductsNeedingReorder());
    }

    // ============================
    // STOCK OPERATION ENDPOINTS
    // ============================

    @PostMapping("/products/{id}/use")
    public ResponseEntity<String> useStock(@PathVariable Long id, @RequestBody StockOperationRequest request) {
        try {
            boolean success = inventoryManager.useStock(id, request.quantity, request.notes, request.performedBy);
            if (success) {
                return ResponseEntity.ok("Stock usage recorded successfully");
            }
            return ResponseEntity.badRequest().body("Insufficient stock available");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/products/{id}/sell")
    public ResponseEntity<String> sellStock(@PathVariable Long id, @RequestBody SaleRequest request) {
        try {
            boolean success = inventoryManager.sellStock(id, request.quantity, request.salePrice, request.notes, request.performedBy);
            if (success) {
                return ResponseEntity.ok("Sale recorded successfully");
            }
            return ResponseEntity.badRequest().body("Insufficient stock available");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/products/{id}/restock")
    public ResponseEntity<String> restockProduct(@PathVariable Long id, @RequestBody RestockRequest request) {
        try {
            boolean success = inventoryManager.restockProduct(id, request.quantity, request.totalCost, request.notes, request.performedBy);
            if (success) {
                return ResponseEntity.ok("Restock completed successfully");
            }
            return ResponseEntity.badRequest().body("Failed to restock product");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/products/{id}/adjust")
    public ResponseEntity<String> adjustStock(@PathVariable Long id, @RequestBody AdjustmentRequest request) {
        try {
            boolean success = inventoryManager.adjustStock(id, request.newQuantity, request.reason, request.performedBy);
            if (success) {
                return ResponseEntity.ok("Stock adjustment completed successfully");
            }
            return ResponseEntity.badRequest().body("Failed to adjust stock");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ============================
    // TRANSACTION ENDPOINTS
    // ============================

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getAllTransactions() {
        return ResponseEntity.ok(inventoryManager.getAllTransactions());
    }

    @GetMapping("/transactions/recent")
    public ResponseEntity<List<Transaction>> getRecentTransactions(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(inventoryManager.getRecentTransactions(days));
    }

    @GetMapping("/products/{id}/transactions")
    public ResponseEntity<List<Transaction>> getProductTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryManager.getTransactionHistory(id));
    }

    @GetMapping("/products/{id}/transactions/range")
    public ResponseEntity<List<Transaction>> getProductTransactionsInRange(
            @PathVariable Long id,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
            return ResponseEntity.ok(inventoryManager.getTransactionHistory(id, start, end));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================
    // ANALYTICS ENDPOINTS
    // ============================

    @GetMapping("/analytics/report")
    public ResponseEntity<InventoryAnalytics.InventoryReport> getInventoryReport() {
        return ResponseEntity.ok(inventoryManager.generateReport());
    }

    @GetMapping("/analytics/statistics")
    public ResponseEntity<InventoryManager.InventoryStatistics> getInventoryStatistics() {
        return ResponseEntity.ok(inventoryManager.getInventoryStatistics());
    }

    @GetMapping("/products/{id}/predict-reorder")
    public ResponseEntity<Map<String, Object>> predictReorderInfo(@PathVariable Long id) {
        try {
            int daysUntilReorder = inventoryManager.predictDaysUntilReorder(id);
            int suggestedQuantity = inventoryManager.suggestReorderQuantity(id);

            return ResponseEntity.ok(Map.of(
                    "daysUntilReorder", daysUntilReorder,
                    "suggestedQuantity", suggestedQuantity,
                    "urgent", daysUntilReorder <= 7
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<DashboardData> getDashboardData() {
        InventoryManager.InventoryStatistics stats = inventoryManager.getInventoryStatistics();
        List<Product> lowStock = inventoryManager.getLowStockProducts();
        List<Product> needReorder = inventoryManager.getProductsNeedingReorder();
        List<Transaction> recentTransactions = inventoryManager.getRecentTransactions(7);

        DashboardData dashboard = new DashboardData(stats, lowStock, needReorder, recentTransactions);
        return ResponseEntity.ok(dashboard);
    }

    // ============================
    // METADATA ENDPOINTS
    // ============================

    @GetMapping("/categories")
    public ResponseEntity<Set<String>> getAllCategories() {
        return ResponseEntity.ok(inventoryManager.getAllCategories());
    }

    @GetMapping("/suppliers")
    public ResponseEntity<Set<String>> getAllSuppliers() {
        return ResponseEntity.ok(inventoryManager.getAllSuppliers());
    }

    // ============================
    // CONFIGURATION ENDPOINTS
    // ============================

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        return ResponseEntity.ok(Map.of(
                "defaultLeadTimeDays", inventoryManager.getDefaultLeadTimeDays(),
                "defaultServiceLevel", inventoryManager.getDefaultServiceLevel()
        ));
    }

    @PostMapping("/config")
    public ResponseEntity<String> updateConfiguration(@RequestBody ConfigurationRequest request) {
        inventoryManager.setDefaultLeadTimeDays(request.leadTimeDays);
        inventoryManager.setDefaultServiceLevel(request.serviceLevel);
        return ResponseEntity.ok("Configuration updated successfully");
    }

    // ============================
    // BULK OPERATIONS
    // ============================

    @PostMapping("/bulk/restock")
    public ResponseEntity<Map<String, Object>> bulkRestock(@RequestBody List<BulkRestockItem> items) {
        int successful = 0;
        int failed = 0;

        for (BulkRestockItem item : items) {
            try {
                boolean success = inventoryManager.restockProduct(
                        item.productId, item.quantity, item.totalCost,
                        "Bulk restock operation", "Bulk API"
                );
                if (success) successful++;
                else failed++;
            } catch (Exception e) {
                failed++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "successful", successful,
                "failed", failed,
                "total", items.size()
        ));
    }

    // ============================
    // HELPER METHOD
    // ============================

    private void initializeSampleData() {
        // Add sample products for demonstration
        inventoryManager.addProduct("Laptop", "Electronics", 50, 10, 899.99, "TechCorp");
        inventoryManager.addProduct("Office Chair", "Furniture", 25, 5, 149.99, "FurniturePlus");
        inventoryManager.addProduct("Printer Paper", "Office Supplies", 100, 20, 4.99, "OfficeMax");
        inventoryManager.addProduct("USB Cable", "Electronics", 200, 50, 9.99, "TechCorp");
        inventoryManager.addProduct("Desk Lamp", "Furniture", 15, 3, 39.99, "LightingInc");

        // Simulate some transactions for analytics
        inventoryManager.useStock(1L, 5, "Department allocation", "John Doe");
        inventoryManager.sellStock(2L, 3, 179.99, "Customer purchase", "Jane Smith");
        inventoryManager.useStock(3L, 25, "Office restocking", "Admin");
        inventoryManager.sellStock(4L, 15, 12.99, "Bulk customer order", "Sales Team");
        inventoryManager.useStock(5L, 2, "Meeting room setup", "Facilities");
    }

    // ============================
    // REQUEST/RESPONSE CLASSES
    // ============================

    public static class CreateProductRequest {
        public String name;
        public String category;
        public int initialStock;
        public int minThreshold;
        public double unitCost;
        public String supplier;
    }

    public static class StockOperationRequest {
        public int quantity;
        public String notes;
        public String performedBy;
    }

    public static class SaleRequest {
        public int quantity;
        public double salePrice;
        public String notes;
        public String performedBy;
    }

    public static class RestockRequest {
        public int quantity;
        public double totalCost;
        public String notes;
        public String performedBy;
    }

    public static class AdjustmentRequest {
        public int newQuantity;
        public String reason;
        public String performedBy;
    }

    public static class ConfigurationRequest {
        public int leadTimeDays;
        public double serviceLevel;
    }

    public static class BulkRestockItem {
        public Long productId;
        public int quantity;
        public double totalCost;
    }

    public static class DashboardData {
        public InventoryManager.InventoryStatistics statistics;
        public List<Product> lowStockProducts;
        public List<Product> needReorderProducts;
        public List<Transaction> recentTransactions;

        public DashboardData(InventoryManager.InventoryStatistics statistics,
                             List<Product> lowStockProducts,
                             List<Product> needReorderProducts,
                             List<Transaction> recentTransactions) {
            this.statistics = statistics;
            this.lowStockProducts = lowStockProducts;
            this.needReorderProducts = needReorderProducts;
            this.recentTransactions = recentTransactions;
        }
    }
}
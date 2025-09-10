// Main business logic

package org.example.service;

import org.example.model.Product;
import org.example.model.Transaction;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Main service class for SmartStock inventory management operations
 * Provides thread-safe business logic for all inventory operations
 */
public class InventoryManager {

    // Thread-safe data storage
    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong productIdCounter = new AtomicLong(1);
    private final AtomicLong transactionIdCounter = new AtomicLong(1);

    // Analytics engine
    private final InventoryAnalytics analytics = new InventoryAnalytics();

    // Configuration
    private int defaultLeadTimeDays = 7;
    private double defaultServiceLevel = 0.95; // 95% service level

    // ============================
    // PRODUCT MANAGEMENT OPERATIONS
    // ============================

    /**
     * Adds a new product to the inventory system
     */
    public Product addProduct(String name, String category, int initialStock,
                              int minThreshold, double unitCost, String supplier) {
        validateProductData(name, category, initialStock, minThreshold, unitCost, supplier);

        Product product = new Product(name, category, initialStock, minThreshold, unitCost, supplier);
        product.setId(productIdCounter.getAndIncrement());
        products.put(product.getId(), product);

        // Record initial stock as a restock transaction
        if (initialStock > 0) {
            recordTransaction(product.getId(), Transaction.TransactionType.RESTOCK,
                    initialStock, initialStock * unitCost, "Initial stock entry", "System");
        }

        return product;
    }

    /**
     * Gets a product by ID
     */
    public Optional<Product> getProduct(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    /**
     * Gets all products in the system
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    /**
     * Gets products by category
     */
    public List<Product> getProductsByCategory(String category) {
        return products.values().stream()
                .filter(p -> p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    /**
     * Searches products by name (case-insensitive, partial match)
     */
    public List<Product> searchProductsByName(String searchTerm) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        return products.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerSearchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Gets products with low stock
     */
    public List<Product> getLowStockProducts() {
        return products.values().stream()
                .filter(Product::isLowStock)
                .sorted(Comparator.comparingInt(Product::getCurrentStock))
                .collect(Collectors.toList());
    }

    /**
     * Gets products that are out of stock
     */
    public List<Product> getOutOfStockProducts() {
        return products.values().stream()
                .filter(p -> p.getCurrentStock() == 0)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing product
     */
    public boolean updateProduct(Product product) {
        if (product.getId() == null || !products.containsKey(product.getId())) {
            return false;
        }

        validateProductData(product.getName(), product.getCategory(),
                product.getCurrentStock(), product.getMinThreshold(),
                product.getUnitCost(), product.getSupplier());

        products.put(product.getId(), product);
        recordTransaction(product.getId(), Transaction.TransactionType.ADJUSTMENT,
                0, 0, "Product information updated", "System");
        return true;
    }

    /**
     * Deletes a product (with validation)
     */
    public boolean deleteProduct(Long id) {
        Product product = products.get(id);
        if (product == null) {
            return false;
        }

        // Check if product has recent transactions
        boolean hasRecentActivity = transactions.stream()
                .anyMatch(t -> t.getProductId().equals(id) &&
                        t.getTimestamp().isAfter(LocalDateTime.now().minusDays(30)));

        if (hasRecentActivity) {
            throw new IllegalStateException("Cannot delete product with recent transaction history");
        }

        return products.remove(id) != null;
    }

    // ============================
    // STOCK OPERATION METHODS
    // ============================

    /**
     * Records usage/consumption of stock
     */
    public boolean useStock(Long productId, int quantity, String notes, String performedBy) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (!product.removeStock(quantity)) {
            return false; // Insufficient stock
        }

        recordTransaction(productId, Transaction.TransactionType.USAGE,
                quantity, quantity * product.getUnitCost(), notes, performedBy);

        // Check if this triggers a reorder alert
        checkAndGenerateReorderAlert(product);

        return true;
    }

    /**
     * Records sale of stock
     */
    public boolean sellStock(Long productId, int quantity, double salePrice, String notes, String performedBy) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        if (!product.removeStock(quantity)) {
            return false; // Insufficient stock
        }

        recordTransaction(productId, Transaction.TransactionType.SALE,
                quantity, salePrice, notes, performedBy);

        checkAndGenerateReorderAlert(product);
        return true;
    }

    /**
     * Restocks a product
     */
    public boolean restockProduct(Long productId, int quantity, double totalCost, String notes, String performedBy) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        int oldStock = product.getCurrentStock();
        product.addStock(quantity);

        recordTransaction(productId, Transaction.TransactionType.RESTOCK,
                quantity, totalCost, notes, performedBy);

        // Update unit cost if provided
        if (totalCost > 0) {
            double newUnitCost = totalCost / quantity;
            // Weighted average of old and new unit cost
            double weightedUnitCost = ((oldStock * product.getUnitCost()) + totalCost) /
                    (oldStock + quantity);
            product.setUnitCost(weightedUnitCost);
        }

        return true;
    }

    /**
     * Makes a manual stock adjustment
     */
    public boolean adjustStock(Long productId, int newQuantity, String reason, String performedBy) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        int oldQuantity = product.getCurrentStock();
        int difference = newQuantity - oldQuantity;

        product.setCurrentStock(newQuantity);

        recordTransaction(productId, Transaction.TransactionType.ADJUSTMENT,
                Math.abs(difference), 0,
                "Stock adjusted from " + oldQuantity + " to " + newQuantity + ". Reason: " + reason,
                performedBy);

        if (newQuantity <= product.getMinThreshold()) {
            checkAndGenerateReorderAlert(product);
        }

        return true;
    }

    // ============================
    // TRANSACTION MANAGEMENT
    // ============================

    /**
     * Records a transaction in the system
     */
    private void recordTransaction(Long productId, Transaction.TransactionType type,
                                   int quantity, double totalCost, String notes, String performedBy) {
        Transaction transaction = new Transaction(productId, type, quantity, totalCost, notes, performedBy);
        transaction.setId(transactionIdCounter.getAndIncrement());
        transactions.add(transaction);
    }

    /**
     * Gets transaction history for a specific product
     */
    public List<Transaction> getTransactionHistory(Long productId) {
        return transactions.stream()
                .filter(t -> t.getProductId().equals(productId))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Gets transaction history for a specific product within date range
     */
    public List<Transaction> getTransactionHistory(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactions.stream()
                .filter(t -> t.getProductId().equals(productId))
                .filter(t -> !t.getTimestamp().isBefore(startDate) && !t.getTimestamp().isAfter(endDate))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Gets all transactions in the system
     */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    /**
     * Gets recent transactions (last N days)
     */
    public List<Transaction> getRecentTransactions(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return transactions.stream()
                .filter(t -> t.getTimestamp().isAfter(cutoffDate))
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    // ============================
    // ANALYTICS AND REPORTING
    // ============================

    /**
     * Generates comprehensive inventory report
     */
    public InventoryAnalytics.InventoryReport generateReport() {
        return analytics.generateReport(getAllProducts(), getAllTransactions());
    }

    /**
     * Gets products that need reordering
     */
    public List<Product> getProductsNeedingReorder() {
        return analytics.getProductsNeedingReorder(getAllProducts(), getAllTransactions());
    }

    /**
     * Predicts days until reorder needed for a specific product
     */
    public int predictDaysUntilReorder(Long productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        return analytics.predictDaysUntilReorder(product, getAllTransactions());
    }

    /**
     * Suggests optimal reorder quantity for a specific product
     */
    public int suggestReorderQuantity(Long productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        return analytics.suggestReorderQuantity(product, getAllTransactions());
    }

    /**
     * Gets inventory statistics
     */
    public InventoryStatistics getInventoryStatistics() {
        List<Product> allProducts = getAllProducts();

        InventoryStatistics stats = new InventoryStatistics();
        stats.totalProducts = allProducts.size();
        stats.totalValue = allProducts.stream().mapToDouble(Product::getTotalValue).sum();
        stats.totalStockUnits = allProducts.stream().mapToInt(Product::getCurrentStock).sum();
        stats.lowStockCount = (int) allProducts.stream().filter(Product::isLowStock).count();
        stats.outOfStockCount = (int) allProducts.stream().filter(p -> p.getCurrentStock() == 0).count();
        stats.categoriesCount = (int) allProducts.stream().map(Product::getCategory).distinct().count();

        // Recent activity
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        stats.recentTransactionsCount = (int) transactions.stream()
                .filter(t -> t.getTimestamp().isAfter(weekAgo))
                .count();

        return stats;
    }

    // ============================
    // UTILITY METHODS
    // ============================

    /**
     * Checks if a product needs reordering and generates alert
     */
    private void checkAndGenerateReorderAlert(Product product) {
        if (product.isLowStock()) {
            int daysUntilReorder = analytics.predictDaysUntilReorder(product, getAllTransactions());
            int suggestedQuantity = analytics.suggestReorderQuantity(product, getAllTransactions());

            // In a real system, this would send notifications/emails
            System.out.printf("🚨 REORDER ALERT: %s (ID: %d) - Current: %d, Threshold: %d, Days left: %d, Suggested order: %d%n",
                    product.getName(), product.getId(), product.getCurrentStock(),
                    product.getMinThreshold(), daysUntilReorder, suggestedQuantity);
        }
    }

    /**
     * Validates product data
     */
    private void validateProductData(String name, String category, int stock,
                                     int threshold, double cost, String supplier) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Minimum threshold cannot be negative");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        if (supplier == null || supplier.trim().isEmpty()) {
            throw new IllegalArgumentException("Supplier cannot be empty");
        }
    }

    /**
     * Gets all unique categories
     */
    public Set<String> getAllCategories() {
        return products.values().stream()
                .map(Product::getCategory)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all unique suppliers
     */
    public Set<String> getAllSuppliers() {
        return products.values().stream()
                .map(Product::getSupplier)
                .collect(Collectors.toSet());
    }

    // ============================
    // CONFIGURATION METHODS
    // ============================

    public void setDefaultLeadTimeDays(int days) {
        this.defaultLeadTimeDays = Math.max(1, days);
    }

    public void setDefaultServiceLevel(double serviceLevel) {
        this.defaultServiceLevel = Math.max(0.5, Math.min(0.99, serviceLevel));
    }

    public int getDefaultLeadTimeDays() {
        return defaultLeadTimeDays;
    }

    public double getDefaultServiceLevel() {
        return defaultServiceLevel;
    }

    // ============================
    // DATA CLASSES
    // ============================

    /**
     * Statistics summary for the inventory system
     */
    public static class InventoryStatistics {
        public int totalProducts;
        public double totalValue;
        public int totalStockUnits;
        public int lowStockCount;
        public int outOfStockCount;
        public int categoriesCount;
        public int recentTransactionsCount;

        @Override
        public String toString() {
            return String.format(
                    "InventoryStatistics{products=%d, value=$%.2f, units=%d, lowStock=%d, outOfStock=%d, categories=%d, recentTxns=%d}",
                    totalProducts, totalValue, totalStockUnits, lowStockCount,
                    outOfStockCount, categoriesCount, recentTransactionsCount
            );
        }
    }
}
package org.example;

import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.InventoryAnalytics;
import org.example.service.InventoryManager;

import java.util.List;
import java.util.Scanner;

/**
 * SmartStock Inventory Management System - Simplified Demo
 */
public class SmartStockApplication {

    private static InventoryManager inventoryManager;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("🏭 SmartStock Inventory Management System");
        System.out.println("==========================================");

        // Initialize the inventory manager
        inventoryManager = new InventoryManager();

        // Add demo data
        initializeDemoData();

        // Run demo
        runDemo();
    }

    private static void initializeDemoData() {
        System.out.println("Initializing demo inventory...");

        // Add sample products
        inventoryManager.addProduct("MacBook Pro", "Electronics", 25, 5, 2499.99, "Apple Inc");
        inventoryManager.addProduct("Office Chair", "Furniture", 40, 8, 299.99, "ErgoMax");
        inventoryManager.addProduct("A4 Paper", "Office Supplies", 200, 30, 8.99, "PaperCorp");
        inventoryManager.addProduct("USB-C Hub", "Electronics", 75, 15, 89.99, "TechGear");
        inventoryManager.addProduct("Standing Desk", "Furniture", 12, 3, 599.99, "WorkSpace Pro");

        // Simulate some transactions
        inventoryManager.useStock(1L, 3, "IT department", "John Smith");
        inventoryManager.sellStock(2L, 5, 349.99, "Customer order", "Sales Team");
        inventoryManager.useStock(3L, 45, "Office use", "Admin");
        inventoryManager.sellStock(4L, 20, 109.99, "Corporate order", "B2B Sales");

        System.out.println("✅ Demo data initialized\n");
    }

    private static void runDemo() {
        System.out.println("=== INVENTORY OVERVIEW ===");
        showInventoryOverview();

        System.out.println("\n=== PREDICTIVE ANALYTICS ===");
        showPredictiveAnalytics();

        System.out.println("\n=== LOW STOCK ALERTS ===");
        showLowStockAlerts();

        System.out.println("\n=== COMPREHENSIVE REPORT ===");
        showReport();

        System.out.println("\n=== TRANSACTION DEMO ===");
        performTransactionDemo();

        System.out.println("\n🎯 SmartStock Demo Complete!");
        System.out.println("This system demonstrates:");
        System.out.println("✅ Real-time inventory tracking");
        System.out.println("✅ Predictive reorder analytics");
        System.out.println("✅ Automated stock alerts");
        System.out.println("✅ Comprehensive reporting");
        System.out.println("✅ Professional backend architecture");
    }

    private static void showInventoryOverview() {
        InventoryManager.InventoryStatistics stats = inventoryManager.getInventoryStatistics();
        System.out.println("Total Products: " + stats.totalProducts);
        System.out.println("Total Value: $" + String.format("%.2f", stats.totalValue));
        System.out.println("Low Stock Count: " + stats.lowStockCount);
        System.out.println("Categories: " + stats.categoriesCount);

        System.out.println("\nProduct List:");
        System.out.printf("%-5s %-15s %-12s %-8s %-8s %-12s%n",
                "ID", "Name", "Category", "Stock", "Min", "Status");
        System.out.println("-".repeat(65));

        List<Product> products = inventoryManager.getAllProducts();
        for (Product p : products) {
            System.out.printf("%-5d %-15s %-12s %-8d %-8d %-12s%n",
                    p.getId(),
                    truncate(p.getName(), 15),
                    truncate(p.getCategory(), 12),
                    p.getCurrentStock(),
                    p.getMinThreshold(),
                    p.getStockStatus());
        }
    }

    private static void showPredictiveAnalytics() {
        List<Product> products = inventoryManager.getAllProducts();

        System.out.printf("%-5s %-15s %-8s %-15s %-12s%n",
                "ID", "Product", "Stock", "Days to Reorder", "Suggested");
        System.out.println("-".repeat(60));

        for (Product p : products) {
            int daysUntilReorder = inventoryManager.predictDaysUntilReorder(p.getId());
            int suggestedQty = inventoryManager.suggestReorderQuantity(p.getId());

            String daysStr = daysUntilReorder == Integer.MAX_VALUE ? "No data" : daysUntilReorder + "d";

            System.out.printf("%-5d %-15s %-8d %-15s %-12d%n",
                    p.getId(),
                    truncate(p.getName(), 15),
                    p.getCurrentStock(),
                    daysStr,
                    suggestedQty);
        }
    }

    private static void showLowStockAlerts() {
        List<Product> lowStock = inventoryManager.getLowStockProducts();
        List<Product> needReorder = inventoryManager.getProductsNeedingReorder();

        if (lowStock.isEmpty()) {
            System.out.println("✅ No low stock products");
        } else {
            System.out.println("⚠️ Low Stock Products:");
            for (Product p : lowStock) {
                System.out.println("  " + p.getName() + " - Stock: " + p.getCurrentStock());
            }
        }

        if (!needReorder.isEmpty()) {
            System.out.println("\n🚨 Products Needing Reorder:");
            for (Product p : needReorder) {
                int suggested = inventoryManager.suggestReorderQuantity(p.getId());
                System.out.println("  " + p.getName() + " - Suggest ordering: " + suggested + " units");
            }
        }
    }

    private static void showReport() {
        InventoryAnalytics.InventoryReport report = inventoryManager.generateReport();

        System.out.println("Report Date: " + report.reportDate.toLocalDate());
        System.out.println("Total Products: " + report.totalProducts);
        System.out.println("Total Value: $" + String.format("%.2f", report.totalValue));
        System.out.println("Average Turnover: " + String.format("%.1f", report.averageTurnoverDays) + " days");

        System.out.println("\nTop Categories by Value:");
        report.topCategories.forEach((category, value) ->
                System.out.println("  " + category + ": $" + String.format("%.2f", value)));

        System.out.println("\nRecent Activity (Last 7 Days):");
        report.recentActivity.forEach((type, quantity) ->
                System.out.println("  " + type + ": " + quantity + " units"));
    }

    private static void performTransactionDemo() {
        System.out.println("Demonstrating stock operations...");

        // Use some stock
        System.out.println("\n1. Using 10 units of A4 Paper:");
        boolean used = inventoryManager.useStock(3L, 10, "Office consumption", "Demo System");
        System.out.println("Result: " + (used ? "Success" : "Failed"));

        // Try to sell something
        System.out.println("\n2. Selling 2 MacBook Pros:");
        boolean sold = inventoryManager.sellStock(1L, 2, 2699.99, "Customer sale", "Demo System");
        System.out.println("Result: " + (sold ? "Success" : "Failed"));

        // Restock something
        System.out.println("\n3. Restocking 20 USB-C Hubs:");
        boolean restocked = inventoryManager.restockProduct(4L, 20, 1600.0, "Supplier delivery", "Demo System");
        System.out.println("Result: " + (restocked ? "Success" : "Failed"));

        // Show updated stats
        System.out.println("\n4. Updated inventory overview:");
        List<Product> products = inventoryManager.getAllProducts();
        for (Product p : products) {
            if (p.getId() <= 4) { // Show first 4 products that we modified
                System.out.printf("  %s: %d units (%s)%n",
                        p.getName(), p.getCurrentStock(), p.getStockStatus());
            }
        }

        // Show recent transactions
        System.out.println("\n5. Recent Transactions:");
        List<Transaction> recent = inventoryManager.getRecentTransactions(1);
        recent.stream().limit(5).forEach(t ->
                System.out.println("  " + t.getType() + " - " + t.getQuantity() + " units - " + t.getFormattedTimestamp()));
    }

    private static String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
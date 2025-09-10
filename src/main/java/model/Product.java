// Represents inventory items

package org.example.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a product in the SmartStock inventory system
 * Contains product information, stock levels, and business logic
 */
public class Product {
    private Long id;
    private String name;
    private String category;
    private int currentStock;
    private int minThreshold;
    private double unitCost;
    private String supplier;
    private LocalDateTime lastRestocked;
    private LocalDateTime createdAt;

    // Constructors
    public Product() {
        this.createdAt = LocalDateTime.now();
    }

    public Product(String name, String category, int currentStock, int minThreshold,
                   double unitCost, String supplier) {
        this();
        this.name = name;
        this.category = category;
        this.currentStock = currentStock;
        this.minThreshold = minThreshold;
        this.unitCost = unitCost;
        this.supplier = supplier;
        this.lastRestocked = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(int minThreshold) {
        this.minThreshold = minThreshold;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public LocalDateTime getLastRestocked() {
        return lastRestocked;
    }

    public void setLastRestocked(LocalDateTime lastRestocked) {
        this.lastRestocked = lastRestocked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Business Logic Methods
    /**
     * Checks if product stock is at or below minimum threshold
     */
    public boolean isLowStock() {
        return currentStock <= minThreshold;
    }

    /**
     * Calculates total value of current stock
     */
    public double getTotalValue() {
        return currentStock * unitCost;
    }

    /**
     * Adds stock to inventory and updates restock timestamp
     */
    public void addStock(int quantity) {
        if (quantity > 0) {
            this.currentStock += quantity;
            this.lastRestocked = LocalDateTime.now();
        }
    }

    /**
     * Removes stock from inventory if sufficient quantity available
     */
    public boolean removeStock(int quantity) {
        if (quantity > 0 && currentStock >= quantity) {
            this.currentStock -= quantity;
            return true;
        }
        return false;
    }

    /**
     * Gets stock status description
     */
    public String getStockStatus() {
        if (currentStock == 0) {
            return "OUT OF STOCK";
        } else if (isLowStock()) {
            return "LOW STOCK";
        } else {
            return "IN STOCK";
        }
    }

    /**
     * Calculates days since last restock
     */
    public long getDaysSinceRestock() {
        if (lastRestocked == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(lastRestocked, LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", currentStock=" + currentStock +
                ", minThreshold=" + minThreshold +
                ", status='" + getStockStatus() + '\'' +
                '}';
    }
}
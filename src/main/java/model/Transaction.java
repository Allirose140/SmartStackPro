// Tracks stock movements

package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents a stock transaction in the SmartStock inventory system
 * Tracks all inventory movements (usage, sales, restocking, adjustments)
 */
public class Transaction {
    private Long id;
    private Long productId;
    private TransactionType type;
    private int quantity;
    private double totalCost;
    private LocalDateTime timestamp;
    private String notes;
    private String performedBy;

    /**
     * Enum for different types of inventory transactions
     */
    public enum TransactionType {
        USAGE("Usage/Consumption"),
        SALE("Sale"),
        RESTOCK("Restock/Purchase"),
        ADJUSTMENT("Manual Adjustment"),
        RETURN("Return/Refund"),
        DAMAGE("Damaged Goods"),
        TRANSFER("Transfer");

        private final String description;

        TransactionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Returns true if this transaction type reduces stock
         */
        public boolean reducesStock() {
            return this == USAGE || this == SALE || this == DAMAGE || this == TRANSFER;
        }

        /**
         * Returns true if this transaction type increases stock
         */
        public boolean increasesStock() {
            return this == RESTOCK || this == RETURN;
        }
    }

    // Constructors
    public Transaction() {
        this.timestamp = LocalDateTime.now();
        this.performedBy = "System";
    }

    public Transaction(Long productId, TransactionType type, int quantity,
                       double totalCost, String notes) {
        this();
        this.productId = productId;
        this.type = type;
        this.quantity = quantity;
        this.totalCost = totalCost;
        this.notes = notes;
    }

    public Transaction(Long productId, TransactionType type, int quantity,
                       double totalCost, String notes, String performedBy) {
        this(productId, type, quantity, totalCost, notes);
        this.performedBy = performedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    // Business Logic Methods
    /**
     * Gets the unit cost per item in this transaction
     */
    public double getUnitCost() {
        return quantity != 0 ? totalCost / quantity : 0;
    }

    /**
     * Returns formatted timestamp for display
     */
    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Returns formatted timestamp for date only
     */
    public String getFormattedDate() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * Gets transaction impact description
     */
    public String getImpactDescription() {
        if (type.reducesStock()) {
            return "Reduced stock by " + quantity + " units";
        } else if (type.increasesStock()) {
            return "Increased stock by " + quantity + " units";
        } else {
            return "Adjusted stock by " + quantity + " units";
        }
    }

    /**
     * Returns true if this is a recent transaction (within last 24 hours)
     */
    public boolean isRecent() {
        return timestamp.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Returns true if this transaction involves a cost
     */
    public boolean hasCost() {
        return totalCost != 0;
    }

    /**
     * Gets the effective quantity change (positive for increases, negative for decreases)
     */
    public int getEffectiveQuantityChange() {
        if (type.increasesStock()) {
            return quantity;
        } else if (type.reducesStock()) {
            return -quantity;
        } else {
            // For adjustments, we assume positive quantity means increase
            return quantity;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", productId=" + productId +
                ", type=" + type +
                ", quantity=" + quantity +
                ", totalCost=$" + String.format("%.2f", totalCost) +
                ", timestamp=" + getFormattedTimestamp() +
                ", performedBy='" + performedBy + '\'' +
                '}';
    }

    /**
     * Returns a detailed description of the transaction
     */
    public String getDetailedDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getDescription())
                .append(" - ")
                .append(quantity)
                .append(" units");

        if (hasCost()) {
            sb.append(" ($").append(String.format("%.2f", totalCost)).append(")");
        }

        if (notes != null && !notes.trim().isEmpty()) {
            sb.append(" - ").append(notes);
        }

        return sb.toString();
    }
}
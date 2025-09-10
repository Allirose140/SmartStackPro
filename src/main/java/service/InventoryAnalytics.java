// Predictive analytics

package org.example.service;

import org.example.model.Product;
import org.example.model.Transaction;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced analytics engine for SmartStock inventory predictions and insights
 * Provides predictive algorithms, trend analysis, and automated decision support
 */
public class InventoryAnalytics {

    /**
     * Predicts days until reorder needed based on historical usage patterns
     * Uses weighted moving average with trend analysis
     */
    public int predictDaysUntilReorder(Product product, List<Transaction> transactions) {
        List<Transaction> relevantTransactions = getRelevantUsageTransactions(product.getId(), transactions, 60);

        if (relevantTransactions.isEmpty()) {
            return Integer.MAX_VALUE; // No usage data available
        }

        double dailyUsageRate = calculateWeightedDailyUsage(relevantTransactions);
        if (dailyUsageRate <= 0) {
            return Integer.MAX_VALUE;
        }

        // Apply trend factor to account for increasing/decreasing usage
        double trendFactor = calculateUsageTrend(relevantTransactions);
        double adjustedUsageRate = dailyUsageRate * (1 + trendFactor);

        int stockAboveThreshold = Math.max(0, product.getCurrentStock() - product.getMinThreshold());
        return (int) Math.ceil(stockAboveThreshold / Math.max(adjustedUsageRate, 0.1));
    }

    /**
     * Calculates weighted daily usage rate (recent transactions weighted more heavily)
     */
    private double calculateWeightedDailyUsage(List<Transaction> transactions) {
        if (transactions.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        double totalWeightedUsage = 0;
        double totalWeight = 0;

        for (Transaction t : transactions) {
            long daysAgo = ChronoUnit.DAYS.between(t.getTimestamp(), now);
            // Exponential decay: more recent = higher weight
            double weight = Math.exp(-daysAgo / 30.0); // 30-day half-life

            totalWeightedUsage += t.getQuantity() * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0) return 0;

        // Convert to daily rate
        LocalDateTime earliest = transactions.stream()
                .map(Transaction::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(now);

        long daysPeriod = Math.max(1, ChronoUnit.DAYS.between(earliest, now));
        return (totalWeightedUsage / totalWeight) * (transactions.size() / (double) daysPeriod);
    }

    /**
     * Calculates usage trend (positive = increasing, negative = decreasing)
     */
    private double calculateUsageTrend(List<Transaction> transactions) {
        if (transactions.size() < 4) return 0; // Need minimum data for trend

        // Sort by timestamp
        transactions.sort(Comparator.comparing(Transaction::getTimestamp));

        // Split into two halves and compare
        int midPoint = transactions.size() / 2;
        List<Transaction> firstHalf = transactions.subList(0, midPoint);
        List<Transaction> secondHalf = transactions.subList(midPoint, transactions.size());

        double firstHalfAvg = firstHalf.stream().mapToInt(Transaction::getQuantity).average().orElse(0);
        double secondHalfAvg = secondHalf.stream().mapToInt(Transaction::getQuantity).average().orElse(0);

        if (firstHalfAvg == 0) return 0;

        // Return percentage change (capped at ±50%)
        return Math.max(-0.5, Math.min(0.5, (secondHalfAvg - firstHalfAvg) / firstHalfAvg));
    }

    /**
     * Suggests optimal reorder quantity using advanced algorithms
     */
    public int suggestReorderQuantity(Product product, List<Transaction> transactions) {
        // Economic Order Quantity (EOQ) inspired calculation
        double monthlyUsage = calculateMonthlyUsage(product.getId(), transactions);
        double leadTimeUsage = calculateLeadTimeUsage(product, transactions);

        // Safety stock: covers variability + lead time
        double safetyStock = calculateSafetyStock(product, transactions);

        // Target: 2-3 months supply + safety stock, but consider storage costs
        double targetStock = (monthlyUsage * 2.5) + safetyStock;

        // Minimum order constraints
        int minimumOrder = Math.max(product.getMinThreshold(), 10);
        int suggestedQuantity = (int) Math.ceil(targetStock - product.getCurrentStock());

        return Math.max(suggestedQuantity, minimumOrder);
    }

    /**
     * Calculates safety stock based on demand variability
     */
    private double calculateSafetyStock(Product product, List<Transaction> transactions) {
        List<Transaction> usageData = getRelevantUsageTransactions(product.getId(), transactions, 90);

        if (usageData.size() < 3) {
            return product.getMinThreshold() * 0.5; // Default to 50% of min threshold
        }

        // Calculate demand variability (standard deviation)
        double[] weeklyUsage = calculateWeeklyUsage(usageData);
        double meanUsage = Arrays.stream(weeklyUsage).average().orElse(0);
        double variance = Arrays.stream(weeklyUsage)
                .map(usage -> Math.pow(usage - meanUsage, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);

        // Safety stock = 1.65 * std dev (95% service level) * sqrt(lead time in weeks)
        double leadTimeWeeks = 1.0; // Assume 1 week lead time
        return 1.65 * stdDev * Math.sqrt(leadTimeWeeks);
    }

    /**
     * Calculates weekly usage patterns
     */
    private double[] calculateWeeklyUsage(List<Transaction> transactions) {
        if (transactions.isEmpty()) return new double[0];

        LocalDateTime start = transactions.stream()
                .map(Transaction::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime end = LocalDateTime.now();
        long totalWeeks = ChronoUnit.WEEKS.between(start, end) + 1;
        double[] weeklyUsage = new double[(int) totalWeeks];

        for (Transaction t : transactions) {
            long weekIndex = ChronoUnit.WEEKS.between(start, t.getTimestamp());
            if (weekIndex >= 0 && weekIndex < weeklyUsage.length) {
                weeklyUsage[(int) weekIndex] += t.getQuantity();
            }
        }

        return weeklyUsage;
    }

    /**
     * Estimates lead time usage for reorder calculations
     */
    private double calculateLeadTimeUsage(Product product, List<Transaction> transactions) {
        double dailyUsage = calculateWeightedDailyUsage(
                getRelevantUsageTransactions(product.getId(), transactions, 30));
        return dailyUsage * 7; // Assume 1 week lead time
    }

    /**
     * Calculates monthly usage rate
     */
    private double calculateMonthlyUsage(Long productId, List<Transaction> transactions) {
        List<Transaction> monthlyData = getRelevantUsageTransactions(productId, transactions, 30);
        return monthlyData.stream().mapToInt(Transaction::getQuantity).sum();
    }

    /**
     * Identifies products that need immediate attention
     */
    public List<Product> getProductsNeedingReorder(List<Product> products, List<Transaction> transactions) {
        return products.stream()
                .filter(p -> {
                    int daysUntilReorder = predictDaysUntilReorder(p, transactions);
                    return p.isLowStock() || daysUntilReorder <= 14; // 2 week threshold
                })
                .sorted((p1, p2) -> {
                    // Sort by urgency: low stock first, then by days until reorder
                    if (p1.isLowStock() && !p2.isLowStock()) return -1;
                    if (!p1.isLowStock() && p2.isLowStock()) return 1;

                    int days1 = predictDaysUntilReorder(p1, transactions);
                    int days2 = predictDaysUntilReorder(p2, transactions);
                    return Integer.compare(days1, days2);
                })
                .collect(Collectors.toList());
    }

    /**
     * Generates comprehensive inventory health report
     */
    public InventoryReport generateReport(List<Product> products, List<Transaction> transactions) {
        InventoryReport report = new InventoryReport();

        report.reportDate = LocalDateTime.now();
        report.totalProducts = products.size();
        report.totalValue = products.stream().mapToDouble(Product::getTotalValue).sum();

        report.lowStockCount = (int) products.stream().filter(Product::isLowStock).count();
        report.outOfStockCount = (int) products.stream()
                .filter(p -> p.getCurrentStock() == 0).count();

        report.productsNeedingReorder = getProductsNeedingReorder(products, transactions);
        report.topCategories = getTopCategoriesByValue(products);
        report.recentActivity = getRecentTransactionSummary(transactions);

        // Calculate turnover metrics
        report.averageTurnoverDays = calculateAverageTurnover(products, transactions);
        report.slowMovingProducts = identifySlowMovingProducts(products, transactions);

        return report;
    }

    /**
     * Gets relevant usage/sale transactions within specified days
     */
    private List<Transaction> getRelevantUsageTransactions(Long productId, List<Transaction> transactions, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        return transactions.stream()
                .filter(t -> t.getProductId().equals(productId))
                .filter(t -> t.getType().reducesStock())
                .filter(t -> t.getTimestamp().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }

    /**
     * Gets top categories by total inventory value
     */
    private Map<String, Double> getTopCategoriesByValue(List<Product> products) {
        return products.stream()
                .collect(Collectors.groupingBy(
                        Product::getCategory,
                        Collectors.summingDouble(Product::getTotalValue)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Gets summary of recent transaction activity
     */
    private Map<String, Integer> getRecentTransactionSummary(List<Transaction> transactions) {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        return transactions.stream()
                .filter(t -> t.getTimestamp().isAfter(weekAgo))
                .collect(Collectors.groupingBy(
                        t -> t.getType().getDescription(),
                        Collectors.summingInt(Transaction::getQuantity)
                ));
    }

    /**
     * Calculates average inventory turnover in days
     */
    private double calculateAverageTurnover(List<Product> products, List<Transaction> transactions) {
        return products.stream()
                .mapToDouble(p -> {
                    List<Transaction> usage = getRelevantUsageTransactions(p.getId(), transactions, 90);
                    if (usage.isEmpty() || p.getCurrentStock() == 0) return 365; // Default to 1 year

                    double dailyUsage = calculateWeightedDailyUsage(usage);
                    return dailyUsage > 0 ? p.getCurrentStock() / dailyUsage : 365;
                })
                .average().orElse(365);
    }

    /**
     * Identifies slow-moving products (low turnover)
     */
    private List<Product> identifySlowMovingProducts(List<Product> products, List<Transaction> transactions) {
        return products.stream()
                .filter(p -> {
                    List<Transaction> usage = getRelevantUsageTransactions(p.getId(), transactions, 90);
                    double monthlyUsage = usage.stream().mapToInt(Transaction::getQuantity).sum() / 3.0;
                    return monthlyUsage < (p.getCurrentStock() * 0.1); // Less than 10% monthly turnover
                })
                .collect(Collectors.toList());
    }

    /**
     * Comprehensive inventory report data class
     */
    public static class InventoryReport {
        public LocalDateTime reportDate;
        public int totalProducts;
        public double totalValue;
        public int lowStockCount;
        public int outOfStockCount;
        public List<Product> productsNeedingReorder;
        public Map<String, Double> topCategories;
        public Map<String, Integer> recentActivity;
        public double averageTurnoverDays;
        public List<Product> slowMovingProducts;

        @Override
        public String toString() {
            return String.format(
                    "InventoryReport{date=%s, products=%d, value=$%.2f, lowStock=%d, outOfStock=%d, needReorder=%d, avgTurnover=%.1f days}",
                    reportDate.toLocalDate(),
                    totalProducts,
                    totalValue,
                    lowStockCount,
                    outOfStockCount,
                    productsNeedingReorder.size(),
                    averageTurnoverDays
            );
        }
    }
}
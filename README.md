# SmartStock Pro

An intelligent inventory management system with predictive analytics, built with Java and Spring Boot.

## Features

### Core Inventory Management
- **Product Management**: Add, update, delete, and search products
- **Stock Operations**: Track usage, sales, restocking, and manual adjustments
- **Real-time Monitoring**: Live inventory tracking with automatic alerts
- **Transaction History**: Complete audit trail of all stock movements

### Advanced Analytics
- **Predictive Reordering**: AI-powered predictions for when to reorder stock
- **Smart Quantity Suggestions**: Optimal reorder quantities using EOQ-inspired algorithms
- **Usage Trend Analysis**: Weighted moving averages with trend detection
- **Low Stock Alerts**: Automated notifications for products needing attention

### Reporting & Insights
- **Comprehensive Dashboard**: Real-time inventory statistics and KPIs
- **Category Analysis**: Performance breakdown by product categories
- **Turnover Metrics**: Inventory velocity and slow-moving product identification
- **Safety Stock Calculations**: Demand variability analysis for buffer stock

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.2.0
- **API**: RESTful web services with OpenAPI documentation
- **Testing**: JUnit 5, Mockito with comprehensive test coverage
- **Build**: Maven with multi-profile configuration
- **Documentation**: UI for API exploration

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/smartstock-pro.git
cd smartstock-pro
```

2. Build the project:
```bash
mvn clean compile
```

3. Run tests:
```bash
mvn test
```

4. Start the application:
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### API Documentation

Once the application is running, access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

## Usage Examples

### Adding a Product
```bash
curl -X POST http://localhost:8080/api/inventory/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "category": "Electronics",
    "initialStock": 50,
    "minThreshold": 10,
    "unitCost": 999.99,
    "supplier": "TechCorp"
  }'
```

### Recording Stock Usage
```bash
curl -X POST http://localhost:8080/api/inventory/products/1/use \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 5,
    "notes": "Department allocation",
    "performedBy": "John Doe"
  }'
```

### Getting Predictive Analytics
```bash
curl http://localhost:8080/api/inventory/products/1/predict-reorder
```

## API Endpoints

### Product Management
- `GET /api/inventory/products` - List all products
- `POST /api/inventory/products` - Add new product
- `GET /api/inventory/products/{id}` - Get product details
- `PUT /api/inventory/products/{id}` - Update product
- `DELETE /api/inventory/products/{id}` - Delete product

### Stock Operations
- `POST /api/inventory/products/{id}/use` - Record stock usage
- `POST /api/inventory/products/{id}/sell` - Record sale
- `POST /api/inventory/products/{id}/restock` - Add stock
- `POST /api/inventory/products/{id}/adjust` - Manual adjustment

### Analytics & Reporting
- `GET /api/inventory/analytics/dashboard` - Dashboard data
- `GET /api/inventory/analytics/report` - Comprehensive report
- `GET /api/inventory/products/{id}/predict-reorder` - Reorder predictions
- `GET /api/inventory/products/low-stock` - Low stock alerts

### Transactions
- `GET /api/inventory/transactions` - All transactions
- `GET /api/inventory/transactions/recent` - Recent activity
- `GET /api/inventory/products/{id}/transactions` - Product history

## Project Structure

```
src/
├── main/java/org/example/
│   ├── controller/          # REST API controllers
│   │   └── InventoryController.java
│   ├── model/              # Domain models
│   │   ├── Product.java
│   │   └── Transaction.java
│   ├── service/            # Business logic
│   │   ├── InventoryManager.java
│   │   └── InventoryAnalytics.java
│   └── SmartStockApplication.java
└── test/java/              # Unit tests
    └── org/example/service/
        └── InventoryManagerTest.java
```

## Key Algorithms

### Predictive Reordering
- Uses weighted moving averages with exponential decay
- Applies trend analysis to adjust for usage pattern changes
- Factors in minimum thresholds and safety stock requirements

### Reorder Quantity Optimization
- Economic Order Quantity (EOQ) inspired calculations
- Demand variability analysis using standard deviation
- Safety stock computation with configurable service levels

### Usage Trend Detection
- Historical data segmentation and comparison
- Percentage change calculation with bounds limiting
- Integration with reorder timing predictions

## Building & Deployment

### Development Profile
```bash
mvn spring-boot:run -Pdev
```

### Production Build
```bash
mvn clean package -Pprod
java -jar target/smartstock-pro-1.0.0.jar
```

### Docker Support
```bash
mvn clean package -Pdocker
# Docker build configuration included in pom.xml
```

## Testing

Run the complete test suite:
```bash
mvn clean test
```

Generate test coverage report:
```bash
mvn clean test jacoco:report
```

Coverage report will be available at `target/site/jacoco/index.html`

## Configuration

Key configuration options in `application.properties`:

```properties
# Server configuration
server.port=8080

# Application settings
spring.application.name=SmartStock Pro

# Logging levels
logging.level.org.example=INFO

# API documentation
springdoc.swagger-ui.path=/swagger-ui.html
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Demo

Run the built-in demo to see the system in action:
```bash
mvn exec:java -Dexec.mainClass="org.example.SmartStockApplication"
```

The demo initializes sample data and demonstrates:
- Inventory overview and statistics
- Predictive analytics capabilities
- Low stock detection
- Transaction processing
- Comprehensive reporting

## Future Enhancements

- Database persistence (JPA/Hibernate)
- User authentication and authorization
- Web-based frontend interface
- Email/SMS notifications for alerts
- Integration with external suppliers
- Advanced reporting with charts
- Multi-location inventory support

- Barcode scanning capabilities

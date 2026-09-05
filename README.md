# Healthcare Patient Data Protection System - Backend

Java 26 / Spring Boot 4 backend providing zero-trust security, AES-256-GCM medical record encryption, ECC key protection, role-based and permission-based access control, immutable auditing, and AI anomaly detection.

## Prerequisites
- Java 26 (Oracle JDK)
- Apache Maven 3.9+
- MySQL 8.0+ running on port 3306 with database `patient_data_protection`

## Quick Start
```powershell
# Build and run tests
& "C:\Users\indur\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd" clean test

# Run application
& "C:\Users\indur\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd" spring-boot:run
```

## Health Check
- `GET /api/health`

Build Instructions
==================

Your instructions go here.

1. Make sure you have Java JDK installed (Java 8 or higher)
2. Compile all Java files with:
   javac *.java
3. Run the test program:
   java LocalTest
   java AzureLabTest

Working Functionality
=====================

Describe what you think should work.

The following functionality has been implemented:
- Basic node configuration (setNodeName and openPort)
- Key/value storage for both address and data pairs
- Proper distance calculation using SHA-256 hashes
- Message handling framework for all CRN protocol messages
- Name Request/Response messages
- Nearest Request/Response messages
- Key Existence Request/Response messages
- Read Request/Response with timeout handling and retry logic
- Write Request/Response with proper handling
- Compare-and-Swap (CAS) implementation with retry logic
- Relay message handling for request forwarding
- Relay stack management (pushRelay/popRelay)
- UDP reliability handling (timeout management, retries)
- Bootstrap message parsing for network discovery
- Closest node selection based on hash distance
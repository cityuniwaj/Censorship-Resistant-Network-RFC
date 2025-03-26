Build Instructions
==================

Your instructions go here.

To build the project:
1. Make sure you have Java JDK installed (Java 8 or higher recommended)
2. Compile all Java files with:
   javac *.java
3. Run the SimpleTest program to test basic functionality:
   java SimpleTest
4. Once more functionality is implemented, you can run the full test:
   java LocalTest

Working Functionality
=====================

Describe what you think should work.

The following functionality has been implemented:
- Basic node configuration (setNodeName and openPort)
- Data structures for storing address and data key/value pairs
- Helper methods for hash distance calculations
- Message formatting and parsing utilities
- Functions to create various CRN protocol messages

Not yet implemented:
- Message handling (handleIncomingMessages)
- Key/value operations (read, write, exists, CAS)
- Relay functionality
- UDP message sending and receiving
Build Instructions
==================

Your instructions go here.

To build the project:
1. Make sure you have Java JDK installed (Java 8 or higher recommended)
2. Compile all Java files with:
   javac *.java
3. You can choose to compile and run the programs stored in the Test Programs
   folder after moving them to the main source folder and uncommenting the
   correct helper functions in Node.java. 
   Those are programs I created for my own use to test basic functionality 
   (SimpleTest.java) and to make sure every CRN message implementation is 
   creating nodes and working as intended.
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
- Handling of Name Request/Response messages
- Basic message processing framework for incoming packets
- Implementation of isActive() to check if a node is responsive
- Relay stack management (pushRelay/popRelay)

Not yet implemented:
- Handling of Nearest, Key Existence, Read, Write, CAS, and Relay messages
- Complete key/value operations (exists, read, write, CAS)
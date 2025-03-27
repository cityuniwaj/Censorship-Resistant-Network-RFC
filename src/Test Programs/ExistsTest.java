public class ExistsTest {
    public static void main(String[] args) {
        try {
            // Create two nodes for testing
            System.out.println("Creating test nodes...");
            Node node1 = new Node();
            Node node2 = new Node();
            
            // Configure the nodes
            node1.setNodeName("N:testnode1");
            node1.openPort(20110);
            
            node2.setNodeName("N:testnode2");
            node2.openPort(20111);
            
            System.out.println("Nodes created successfully!");
            
            // Add some data to node2
            System.out.println("Adding test data to node2...");
            node2.addData("D:testkey1", "Test Value 1");
            node2.addData("D:testkey2", "Test Value 2");

            // Debug
            System.out.println("DEBUG: Node2 data entries: " + node2.getDataEntriesDebug());
            
            // Make node1 aware of node2
            System.out.println("Adding node2's address to node1's knowledge...");
            node1.addAddressEntry("N:testnode2", "127.0.0.1:20111");
            
            // Start node2 in a separate thread to handle incoming messages
            Thread node2Thread = new Thread(() -> {
                try {
                    System.out.println("Node2 listening for messages...");
                    node2.handleIncomingMessages(20000); // Listen for 20 seconds
                    System.out.println("Node2 finished listening");
                } catch (Exception e) {
                    System.err.println("Error in node2 thread: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            node2Thread.start();
            
            // Wait a moment for node2 to start listening
            Thread.sleep(1000);
            
            // Test exists functionality
            System.out.println("\nTesting exists method:");
            
            // Check for existing key
            System.out.println("Checking if 'D:testkey1' exists...");
            boolean exists1 = node1.exists("D:testkey1");
            System.out.println("Key 'D:testkey1' exists: " + exists1);
            
            // Check for another existing key
            System.out.println("Checking if 'D:testkey2' exists...");
            boolean exists2 = node1.exists("D:testkey2");
            System.out.println("Key 'D:testkey2' exists: " + exists2);
            
            // Check for non-existing key
            System.out.println("Checking if 'D:nonexistent' exists...");
            boolean exists3 = node1.exists("D:nonexistent");
            System.out.println("Key 'D:nonexistent' exists: " + exists3);
            
            // Test with relay
            System.out.println("\nTesting exists method with relay:");
            
            // Create a third node as relay
            Node node3 = new Node();
            node3.setNodeName("N:relay");
            node3.openPort(20112);
            
            // Make node1 aware of node3
            node1.addAddressEntry("N:relay", "127.0.0.1:20112");
            
            // Make node3 aware of node2
            node3.addAddressEntry("N:testnode2", "127.0.0.1:20111");
            
            // Start node3 as relay
            Thread node3Thread = new Thread(() -> {
                try {
                    System.out.println("Node3 (relay) listening for messages...");
                    node3.handleIncomingMessages(20000); // Listen for 20 seconds
                    System.out.println("Node3 finished listening");
                } catch (Exception e) {
                    System.err.println("Error in node3 thread: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            node3Thread.start();
            
            // Wait a moment for node3 to start listening
            Thread.sleep(1000);
            
            // Check for existing key through relay
            System.out.println("Checking if 'D:testkey1' exists through relay...");
            // Push relay before this specific test
            node1.pushRelay("N:relay");
            boolean existsRelay = node1.exists("D:testkey1");
            System.out.println("Key 'D:testkey1' exists through relay: " + existsRelay);
            // Pop relay after the test
            node1.popRelay();
            
            // Wait for threads to finish
            node2Thread.join();
            node3Thread.join();
            
            System.out.println("\nTest completed!");
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
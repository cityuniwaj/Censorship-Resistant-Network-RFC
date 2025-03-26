public class NameRequestTest {
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
            
            // Start node2 in a separate thread to handle incoming messages
            Thread node2Thread = new Thread(() -> {
                try {
                    System.out.println("Node2 listening for messages...");
                    node2.handleIncomingMessages(10000); // Listen for 10 seconds
                    System.out.println("Node2 finished listening");
                } catch (Exception e) {
                    System.err.println("Error in node2 thread: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            node2Thread.start();
            
            // Wait a moment for node2 to start listening
            Thread.sleep(1000);
            
            // Test isActive functionality with node1 checking if node2 is active
            System.out.println("Testing isActive - node1 checking node2...");
            
            // First we need to add node2's address to node1's knowledge
            // For a real implementation, this would be handled by the bootstrapping,
            // but for this test we'll do it manually
            
            // We'll need to add a method to Node.java to manually add an address entry
            // Let's call it addAddressEntry(String nodeName, String address)
            
            // For now, we're assuming you've added this method to Node.java
            node1.addAddressEntry("N:testnode2", "127.0.0.1:20111");
            
            // Now check if node2 is active
            boolean active = node1.isActive("N:testnode2");
            System.out.println("Is node2 active? " + active);
            
            // Wait for node2 thread to finish
            node2Thread.join();
            
            System.out.println("Test completed!");
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
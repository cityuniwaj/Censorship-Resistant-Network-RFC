public class SimpleTest {
    public static void main(String[] args) {
        try {
            // Create a node
            Node node = new Node();
            
            // Set up the node
            node.setNodeName("N:testnode");
            node.openPort(20110);
            
            System.out.println("Node created successfully!");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
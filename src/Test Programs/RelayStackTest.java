public class RelayStackTest {
    public static void main(String[] args) {
        try {
            // Create a test node
            System.out.println("Creating test node...");
            Node node = new Node();
            
            // Configure the node
            node.setNodeName("N:testnode");
            node.openPort(20110);
            
            System.out.println("Node created successfully!");
            
            // Test pushing and popping from the relay stack
            System.out.println("\nTesting relay stack operations:");
            
            // Push some nodes onto the stack
            System.out.println("Pushing 'N:relay1' onto the stack");
            node.pushRelay("N:relay1");
            
            System.out.println("Pushing 'N:relay2' onto the stack");
            node.pushRelay("N:relay2");
            
            System.out.println("Pushing 'N:relay3' onto the stack");
            node.pushRelay("N:relay3");
            
            // We need to add a method to check the current relay stack
            // Since it's a private field, we'll add a temporary method to Node.java
            // public String[] getRelayStack() {
            //     return relayStack.toArray(new String[0]);
            // }
            
            // Print the current stack
            System.out.println("\nCurrent relay stack:");
            String[] stack = node.getRelayStack();
            for (int i = 0; i < stack.length; i++) {
                System.out.println((i+1) + ". " + stack[i]);
            }
            
            // Test popping from the stack
            System.out.println("\nPopping from the stack");
            node.popRelay();
            
            // Print the updated stack
            System.out.println("\nUpdated relay stack after one pop:");
            stack = node.getRelayStack();
            for (int i = 0; i < stack.length; i++) {
                System.out.println((i+1) + ". " + stack[i]);
            }
            
            // Pop again
            System.out.println("\nPopping from the stack again");
            node.popRelay();
            
            // Print the updated stack
            System.out.println("\nUpdated relay stack after second pop:");
            stack = node.getRelayStack();
            for (int i = 0; i < stack.length; i++) {
                System.out.println((i+1) + ". " + stack[i]);
            }
            
            // Test popping from an almost empty stack
            System.out.println("\nPopping from the stack a third time");
            node.popRelay();
            
            // Print the empty stack
            System.out.println("\nUpdated relay stack after third pop (should be empty):");
            stack = node.getRelayStack();
            if (stack.length == 0) {
                System.out.println("Stack is empty");
            } else {
                for (int i = 0; i < stack.length; i++) {
                    System.out.println((i+1) + ". " + stack[i]);
                }
            }
            
            // Test popping from an empty stack (should not throw an exception)
            System.out.println("\nPopping from an empty stack");
            node.popRelay();
            System.out.println("No exception thrown (good)");
            
            System.out.println("\nTest completed successfully!");
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
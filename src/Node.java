// IN2011 Computer Networks
// Coursework 2024/2025
//
// Submission by
//  YOUR_NAME_GOES_HERE
//  YOUR_STUDENT_ID_NUMBER_GOES_HERE
//  YOUR_EMAIL_GOES_HERE

//  Imports required for the implementation
import java.net.*;
import java.util.*;
import java.io.IOException;

// DO NOT EDIT starts
// This gives the interface that your code must implement.
// These descriptions are intended to help you understand how the interface
// will be used. See the RFC for how the protocol works.

interface NodeInterface {

    /* These methods configure your node.
     * They must both be called once after the node has been created but
     * before it is used. */
    
    // Set the name of the node.
    public void setNodeName(String nodeName) throws Exception;

    // Open a UDP port for sending and receiving messages.
    public void openPort(int portNumber) throws Exception;


    /*
     * These methods query and change how the network is used.
     */

    // Handle all incoming messages.
    // If you wait for more than delay miliseconds and
    // there are no new incoming messages return.
    // If delay is zero then wait for an unlimited amount of time.
    public void handleIncomingMessages(int delay) throws Exception;
    
    // Determines if a node can be contacted and is responding correctly.
    // Handles any messages that have arrived.
    public boolean isActive(String nodeName) throws Exception;

    // You need to keep a stack of nodes that are used to relay messages.
    // The base of the stack is the first node to be used as a relay.
    // The first node must relay to the second node and so on.
    
    // Adds a node name to a stack of nodes used to relay all future messages.
    public void pushRelay(String nodeName) throws Exception;

    // Pops the top entry from the stack of nodes used for relaying.
    // No effect if the stack is empty
    public void popRelay() throws Exception;
    

    /*
     * These methods provide access to the basic functionality of
     * CRN-25 network.
     */

    // Checks if there is an entry in the network with the given key.
    // Handles any messages that have arrived.
    public boolean exists(String key) throws Exception;
    
    // Reads the entry stored in the network for key.
    // If there is a value, return it.
    // If there isn't a value, return null.
    // Handles any messages that have arrived.
    public String read(String key) throws Exception;

    // Sets key to be value.
    // Returns true if it worked, false if it didn't.
    // Handles any messages that have arrived.
    public boolean write(String key, String value) throws Exception;

    // If key is set to currentValue change it to newValue.
    // Returns true if it worked, false if it didn't.
    // Handles any messages that have arrived.
    public boolean CAS(String key, String currentValue, String newValue) throws Exception;

}
// DO NOT EDIT ends

// Complete this!
public class Node implements NodeInterface {
    // Node information
    private String nodeName;
    private byte[] nodeHashID;  // SHA-256 hash of the node name
    
    // Network communication
    private DatagramSocket socket;
    private InetAddress localAddress;
    private int portNumber;
    
    // Storage for key/value pairs
    // Map of distances to sets of address key/value pairs (max 3 per distance)
    private Map<Integer, List<AddressEntry>> addressEntries;
    // Storage for data key/value pairs
    private Map<String, String> dataEntries;
    
    // Relay stack for message routing
    private Stack<String> relayStack;
    
    // Transaction tracking (for request/response matching)
    private Map<String, ResponseHandler> pendingRequests;
    private Random random;  // For generating transaction IDs
    
    // Inner class to represent an address entry with distance information
    private class AddressEntry {
        String nodeName;
        String address;    // in format "ip:port"
        byte[] hashID;
        
        public AddressEntry(String nodeName, String address) throws Exception {
            this.nodeName = nodeName;
            this.address = address;
            this.hashID = HashID.computeHashID(nodeName);
        }
        
        // Parse the IP address and port from the address string
        public InetAddress getInetAddress() throws Exception {
            String ip = address.substring(0, address.lastIndexOf(':'));
            return InetAddress.getByName(ip);
        }
        
        public int getPort() {
            String port = address.substring(address.lastIndexOf(':') + 1);
            return Integer.parseInt(port);
        }
    }
    
    // Interface for handling responses to requests
    private interface ResponseHandler {
        void handleResponse(String response);
        void handleTimeout();
    }
    
    // Constructor
    public Node() {
        // Initialize our data structures
        this.addressEntries = new HashMap<>();
        this.dataEntries = new HashMap<>();
        this.relayStack = new Stack<>();
        this.pendingRequests = new HashMap<>();
        this.random = new Random();
    }
    
    // Helper method to calculate distance between two hashIDs
    private int calculateDistance(byte[] hashID1, byte[] hashID2) {
        // The distance is 256 minus the number of leading bits that match
        int matchingBits = 0;
        
        for (int i = 0; i < hashID1.length; i++) {
            byte xor = (byte) (hashID1[i] ^ hashID2[i]);
            
            if (xor == 0) {
                matchingBits += 8;
            } else {
                // Count the number of leading zeros in this byte
                int mask = 0x80;
                while ((xor & mask) == 0 && mask != 0) {
                    matchingBits++;
                    mask >>>= 1;
                }
                break;
            }
        }
        
        return 256 - matchingBits;
    }
    
    // Helper method to find the three closest nodes to a given hashID
    private List<AddressEntry> findClosestNodes(byte[] targetHashID) {
        // Create a list of all address entries with their distances
        List<AddressEntry> allEntries = new ArrayList<>();
        
        for (List<AddressEntry> entries : addressEntries.values()) {
            allEntries.addAll(entries);
        }
        
        // Sort by distance to target
        allEntries.sort((a, b) -> {
            int distA = calculateDistance(a.hashID, targetHashID);
            int distB = calculateDistance(b.hashID, targetHashID);
            return Integer.compare(distA, distB);
        });
        
        // Return the three closest (or fewer if we don't have three)
        int count = Math.min(3, allEntries.size());
        return allEntries.subList(0, count);
    }
    
    // Helper methods for message formatting and parsing

    // Generate a random transaction ID (two non-space bytes)
    private String generateTransactionID() {
        byte[] id = new byte[2];
        do {
            random.nextBytes(id);
        } while (id[0] == 0x20 || id[1] == 0x20); // Ensure no spaces
        
        return new String(id);
    }

    // Format a string according to CRN protocol (count + space + string + space)
    private String formatString(String str) {
        int spaceCount = 0;
        for (char c : str.toCharArray()) {
            if (c == ' ') spaceCount++;
        }
        
        return spaceCount + " " + str + " ";
    }

    // Format a key/value pair according to CRN protocol
    private String formatKeyValue(String key, String value) {
        return formatString(key) + formatString(value);
    }

    // Parse a formatted string from a CRN message
    private String parseString(String message, int[] position) {
        // Find the space count
        int endPos = message.indexOf(' ', position[0]);
        if (endPos == -1) return null;
        
        int spaceCount = Integer.parseInt(message.substring(position[0], endPos));
        position[0] = endPos + 1; // Move past the first space
        
        // Find the end of the string (after the correct number of spaces)
        int spacesFound = 0;
        int startPos = position[0];
        
        while (position[0] < message.length() && spacesFound <= spaceCount) {
            if (message.charAt(position[0]) == ' ') {
                spacesFound++;
            }
            position[0]++;
        }
        
        // If we didn't find enough spaces, the format is incorrect
        if (spacesFound != spaceCount + 1) return null;
        
        // Return the string without the final space
        return message.substring(startPos, position[0] - 1);
    }

    // Send a UDP packet with the given message to the specified address and port
    private void sendMessage(String message, InetAddress address, int port) throws Exception {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }

    // Methods for creating different message types

    // Create a name request message
    private String createNameRequest() {
        String transactionID = generateTransactionID();
        return transactionID + " G";
    }

    // Create a nearest request message
    private String createNearestRequest(String hashIDHex) {
        String transactionID = generateTransactionID();
        return transactionID + " N " + hashIDHex;
    }

    // Create a key existence request message
    private String createExistenceRequest(String key) {
        String transactionID = generateTransactionID();
        return transactionID + " E " + formatString(key);
    }

    // Create a read request message
    private String createReadRequest(String key) {
        String transactionID = generateTransactionID();
        return transactionID + " R " + formatString(key);
    }

    // Create a write request message
    private String createWriteRequest(String key, String value) {
        String transactionID = generateTransactionID();
        return transactionID + " W " + formatKeyValue(key, value);
    }

    // Create a compare-and-swap request message
    private String createCASRequest(String key, String currentValue, String newValue) {
        String transactionID = generateTransactionID();
        return transactionID + " C " + formatString(key) + formatString(currentValue) + formatString(newValue);
    }

    // Create a relay message
    private String createRelayMessage(String nodeName, String message) {
        String transactionID = generateTransactionID();
        return transactionID + " V " + formatString(nodeName) + message;
    }

    // setNodeName method
    public void setNodeName(String nodeName) throws Exception {
        this.nodeName = nodeName;
        this.nodeHashID = HashID.computeHashID(nodeName);
    }

    // openPort method
    public void openPort(int portNumber) throws Exception {
        this.portNumber = portNumber;
        this.socket = new DatagramSocket(portNumber);
        this.localAddress = InetAddress.getLocalHost();
    }
    
    // 1st method implementation
    public void handleIncomingMessages(int delay) throws Exception {
        byte[] buffer = new byte[1024]; // Buffer for incoming packets
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        // Set socket timeout based on delay parameter
        if (delay > 0) {
            socket.setSoTimeout(delay);
        } else {
            socket.setSoTimeout(0); // Infinite timeout
        }
        
        try {
            while (true) {
                try {
                    // Wait for an incoming packet
                    socket.receive(packet);
                    
                    // Convert packet data to string
                    String message = new String(packet.getData(), 0, packet.getLength());
                    
                    // Process the message
                    processMessage(message, packet.getAddress(), packet.getPort());
                    
                    // If delay is 0, continue indefinitely
                    // Otherwise, process one message and return
                    if (delay > 0) {
                        return;
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout occurred, return as specified
                    return;
                }
            }
        } catch (IOException e) {
            throw new Exception("Error handling incoming messages: " + e.getMessage());
        }
    }
    
    // Process an incoming message and send appropriate response
    private void processMessage(String message, InetAddress sourceAddress, int sourcePort) throws Exception {
        // Message must be at least 4 characters: 2 for transaction ID, 1 for space, 1 for message type
        if (message.length() < 4) {
            return; // Invalid message format
        }
        
        // Extract the transaction ID (first two characters)
        String transactionID = message.substring(0, 2);
        
        // Check if this is a response to a pending request
        if (pendingRequests.containsKey(transactionID)) {
            // This is a response to our request
            ResponseHandler handler = pendingRequests.get(transactionID);
            handler.handleResponse(message);
            pendingRequests.remove(transactionID);
            return;
        }
        
        // This is a new request, process based on message type
        char messageType = message.charAt(3);
        
        // For version 1.3, we only implement the Name Request ('G')
        if (messageType == 'G') { // Name Request
            handleNameRequest(transactionID, sourceAddress, sourcePort);
        }
        // Other message types will be implemented in future versions
    }
    
    // Handler method for Name Request
    private void handleNameRequest(String transactionID, InetAddress sourceAddress, int sourcePort) throws Exception {
        // Create response message: transactionID + " H " + nodeName
        String response = transactionID + " H " + formatString(nodeName);
        sendMessage(response, sourceAddress, sourcePort);
        
        // Passive mapping: store the address of this node if we don't have it
        String address = sourceAddress.getHostAddress() + ":" + sourcePort;
        
        // We'll handle storing the address in a future version
    }
    
    public boolean isActive(String nodeName) throws Exception {
        // Look up the node in our address entries
        AddressEntry entry = findAddressEntryByName(nodeName);
        
        if (entry == null) {
            return false; // We don't know this node
        }
        
        // Create a new transaction ID for this request
        final String transactionID = generateTransactionID();
        
        // Create a name request message
        String request = transactionID + " G";
        
        // Prepare for the response
        final boolean[] isActive = {false};
        final boolean[] responseReceived = {false};
        
        // Create a response handler
        ResponseHandler handler = new ResponseHandler() {
            @Override
            public void handleResponse(String response) {
                // Check if the response is a valid name response
                if (response.length() >= 4 && response.startsWith(transactionID) && response.charAt(3) == 'H') {
                    // Parse the node name from the response
                    int[] position = {5}; // Start after "XX H "
                    String respondingNodeName = parseString(response, position);
                    
                    // Check if the node name matches what we expect
                    if (respondingNodeName != null && respondingNodeName.equals(nodeName)) {
                        isActive[0] = true;
                    }
                }
                responseReceived[0] = true;
            }
            
            @Override
            public void handleTimeout() {
                responseReceived[0] = true;
            }
        };
        
        // Register the handler
        pendingRequests.put(transactionID, handler);
        
        try {
            // Send the request
            sendMessage(request, entry.getInetAddress(), entry.getPort());
            
            // Set a timeout
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    ResponseHandler h = pendingRequests.remove(transactionID);
                    if (h != null && !responseReceived[0]) {
                        h.handleTimeout();
                    }
                }
            }, 5000); // 5 second timeout
            
            // Wait for the response by processing incoming messages
            while (!responseReceived[0]) {
                handleIncomingMessages(100); // Wait with 100ms timeout
            }
            
            return isActive[0];
        } catch (Exception e) {
            // Clean up in case of failure
            pendingRequests.remove(transactionID);
            throw e;
        }
    }
    
    // Helper method to find an address entry by node name
    private AddressEntry findAddressEntryByName(String nodeName) {
        for (List<AddressEntry> entries : addressEntries.values()) {
            for (AddressEntry entry : entries) {
                if (entry.nodeName.equals(nodeName)) {
                    return entry;
                }
            }
        }
        return null;
    }

    // TESTER FILE HELPER METHODS TO TEST CRN MESSAGES
    // Files stored in the Test Programs folder must be COMPILED and RUN in the main source
    // folder or wherever the main compiled java source code classes are (HashID.class, 
    // LocalTest.class, Node.class, NodeInterface.class etc)
    // These files are NOT part of the LocalTest.java program, they are just for me to make
    // sure that the CRN message implementations i have created are creating nodes
    // and are working as intended.
    // ---------------------------------------------------------------------------------------



    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // UNCOMMENT AND MOVE THE NameRequestTest.java FILE TO THE MAIN SRC FOLDER, THEN COMPILE AND RUN IT
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    
    // // Helper method for testing 1st method implementation - add an address entry manually
    // public void addAddressEntry(String nodeName, String address) throws Exception {
    //     AddressEntry entry = new AddressEntry(nodeName, address);
        
    //     // Calculate the distance between this node and the entry
    //     int distance = calculateDistance(nodeHashID, entry.hashID);
        
    //     // Get or create the list for this distance
    //     List<AddressEntry> entries = addressEntries.getOrDefault(distance, new ArrayList<>());
        
    //     // Add the entry if we have fewer than 3 at this distance
    //     if (entries.size() < 3) {
    //         entries.add(entry);
    //         addressEntries.put(distance, entries);
    //     }
    // }



    // ---------------------------------------------------------------------------------------
    
    // 2nd method implementation
    public void pushRelay(String nodeName) throws Exception {
        // Add the node name to the top of the relay stack
        relayStack.push(nodeName);
    }

    // 3rd method implementation
    public void popRelay() throws Exception {
        // Remove the top entry from the relay stack if it's not empty
        if (!relayStack.isEmpty()) {
            relayStack.pop();
        }
    }

    // ---------------------------------------------------------------------------------------

    // 4th method implementation
    public boolean exists(String key) throws Exception {
        throw new Exception("Not implemented");
    }
    
    // 5th method implementation
    public String read(String key) throws Exception {
        throw new Exception("Not implemented");
    }

    // 6th method implementation
    public boolean write(String key, String value) throws Exception {
        throw new Exception("Not implemented");
    }

    // 7th method implementation
    public boolean CAS(String key, String currentValue, String newValue) throws Exception {
        throw new Exception("Not implemented");
    }
}
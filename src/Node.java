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

    public void handleIncomingMessages(int delay) throws Exception {
        throw new Exception("Not implemented");
    }
    
    public boolean isActive(String nodeName) throws Exception {
        throw new Exception("Not implemented");
    }
    
    public void pushRelay(String nodeName) throws Exception {
        throw new Exception("Not implemented");
    }

    public void popRelay() throws Exception {
        throw new Exception("Not implemented");
    }

    public boolean exists(String key) throws Exception {
        throw new Exception("Not implemented");
    }
    
    public String read(String key) throws Exception {
        throw new Exception("Not implemented");
    }

    public boolean write(String key, String value) throws Exception {
        throw new Exception("Not implemented");
    }

    public boolean CAS(String key, String currentValue, String newValue) throws Exception {
        throw new Exception("Not implemented");
    }
}
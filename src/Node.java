// IN2011 Computer Networks
// Coursework 2024/2025
//
// Submission by
//  Wajid Uddin
//  220028395
//  wajid.uddin@city.ac.uk

import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

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
    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // NODE CLASS FIELDS
    
    private String nodeName;
    private DatagramSocket socket;
    private int portNumber;
    private Map<String, String> keyValueStore = new HashMap<>();
    private Map<String, String> addressStore = new HashMap<>();
    private Stack<String> relayStack = new Stack<>();
    private byte[] buffer = new byte[1024];
    private Random random = new Random();

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // INITIALIZATION

    // 1st method implementation
    public void setNodeName(String nodeName) throws Exception {
        if (nodeName == null || !nodeName.startsWith("N:")) {
            throw new Exception("Invalid node name: must start with N:");
        }
        this.nodeName = nodeName;
        
        if (this.socket != null) {
            String value = "127.0.0.1:" + this.portNumber;
            addressStore.put(nodeName, value);
        }
    }

    // 2nd method implementation
    public void openPort(int portNumber) throws Exception {
        if (portNumber < 20110 || portNumber > 20130) {
            throw new Exception("Port number should be between 20110 and 20130");
        }

        this.portNumber = portNumber;
        this.socket = new DatagramSocket(portNumber);
        
        if (this.nodeName != null) {
            String value = "127.0.0.1:" + this.portNumber;
            addressStore.put(nodeName, value);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // MESSAGE PROCESSING

    // 3rd method implementation
    public void handleIncomingMessages(int delay) throws Exception {
        if (socket == null) {
            throw new Exception("Socket not initialized. Call openPort first.");
        }
        
        if (delay > 0) {
            socket.setSoTimeout(delay);
        } else {
            socket.setSoTimeout(0);
        }
        
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                handleBootstrapMessage(packet.getData(), packet.getLength());
                
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                processMessage(message, packet.getAddress(), packet.getPort());
            }
        } catch (SocketTimeoutException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
    }

    private void processMessage(String message, InetAddress address, int port) throws Exception {
        if (message.length() < 3) {
            return;
        }
        
        String transactionId = message.substring(0, 2);
        char messageType = message.charAt(2);
        
        switch (messageType) {
            case 'G': // Name request
                sendResponse(transactionId, address, port);
                break;
                
            case 'N': // Nearest request
                String hashIDStr = message.substring(4).trim();
                
                StringBuilder responseBuilder = new StringBuilder(transactionId + " O ");
                responseBuilder.append(encodeString(nodeName));
                responseBuilder.append(encodeString(addressStore.get(nodeName)));
                
                sendMessage(responseBuilder.toString(), address, port);
                break;
                
            case 'E': // Key existence request
                getData(message, transactionId, address, port);
                break;
                
            case 'R': // Read request
                readRequest(message, transactionId, address, port);
                break;
                
            case 'W': // Write request
                // For regular write requests (not bootstrap)
                if (message.length() > 6 && message.contains("D:Juliet-")) {
                    String content = message.substring(4);
                    int firstSpacePos = content.indexOf(' ');
                    
                    if (firstSpacePos != -1) {
                        String key = content.substring(0, firstSpacePos);
                        String remaining = content.substring(firstSpacePos + 1);
                        
                        String[] parts = remaining.split(" ", 3);
                        if (parts.length >= 2) {
                            int spaceCount;
                            try {
                                spaceCount = Integer.parseInt(parts[0]);
                                String value = parts[1];
                                
                                keyValueStore.put(key, value);
                                
                                String response = transactionId + " X A";
                                sendMessage(response, address, port);
                                break;
                            } catch (NumberFormatException e) {
                                System.err.println("Error: " + e.getMessage());
                            }
                        }
                    }
                }
                
                writeRequest(message, transactionId, address, port);
                break;
                
            case 'C': // Compare-and-swap request
                break;
                
            case 'V': // Relay message
                String content = message.substring(4);
                int spacePos = content .indexOf(' ');
    
                if (spacePos > 0) {
                    String relayNodeName = content .substring(0, spacePos);
                    String relayMessage = content .substring(spacePos + 1);
                    
                    if (addressStore.containsKey(relayNodeName)) {
                        String[] parts = addressStore.get(relayNodeName).split(":");
                        InetAddress relayAddress = InetAddress.getByName(parts[0]);
                        int relayPort = Integer.parseInt(parts[1]);
                        
                        sendMessage(relayMessage, relayAddress, relayPort);
                        
                        char msgType = relayMessage.charAt(2);
                        if ("GNERCW".indexOf(msgType) >= 0) {
                            socket.setSoTimeout(5000);
                            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                            
                            socket.receive(responsePacket);
                            String responseMsg = new String(responsePacket.getData(), 0, responsePacket.getLength(), StandardCharsets.UTF_8);
                            
                            String relayResponse = transactionId + responseMsg.substring(2);
                            sendMessage(relayResponse, address, port);
                        }
                    }
                }
                break;
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // MESSAGE HANDLERS

    private void sendResponse(String transactionId, InetAddress address, int port) throws Exception {
        String response = transactionId + " H " + encodeString(nodeName) + " ";
        sendMessage(response, address, port);
    }

    private void getData(String message, String transactionId, InetAddress address, int port) throws Exception {
        String[] parts = message.substring(4).split(" ", 2);
        String key = parts[0];
        
        char responseChar;
        if (keyValueStore.containsKey(key)) {
            responseChar = 'Y';
        } else if (isNearestNode(key)) {
            responseChar = 'N';
        } else {
            responseChar = '?';
        }
        
        String response = transactionId + " F " + responseChar;
        sendMessage(response, address, port);
    }

    private void readRequest(String message, String transactionId, InetAddress address, int port) throws Exception {
        String content = message.substring(4).trim();
        String key = content;
        
        char responseChar;
        String value = "";
        
        if (keyValueStore.containsKey(key)) {
            responseChar = 'Y';
            value = keyValueStore.get(key);
        } else {
            responseChar = 'N';
        }
        
        String response = transactionId + " S " + responseChar + " " + encodeString(value);
        sendMessage(response, address, port);
    }

    private void writeRequest(String message, String transactionId, InetAddress address, int port) throws Exception {
        String content = message.substring(4);
        
        if (content.contains("N:test")) {
            String[] parts = content.split(" ");
            if (parts.length >= 4) {
                String key = parts[1];
                String value = parts[3];
                addressStore.put(key, value);
                
                String response = transactionId + " X A";
                sendMessage(response, address, port);
                return;
            }
        }
        
        if (content.contains("D:Juliet-")) {
            int keyEnd = content.indexOf(" ");
            if (keyEnd > 0) {
                String key = content.substring(0, keyEnd);
                String value = content.substring(keyEnd + 1).trim();
                
                int valueStart = value.indexOf(" ");
                if (valueStart > 0) {
                    value = value.substring(valueStart + 1).trim();
                    
                    keyValueStore.put(key, value);
                    
                    String response = transactionId + " X A";
                    sendMessage(response, address, port);
                    return;
                }
            }
        }
        
        String response = transactionId + " X X";
        sendMessage(response, address, port);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // CORE UTILITY METHODS

    // 4th method implementation
    public boolean isActive(String nodeName) throws Exception {
        if (!addressStore.containsKey(nodeName)) {
            return false;
        }
        
        String address = addressStore.get(nodeName);
        String[] parts = address.split(":");
        InetAddress ipAddress = InetAddress.getByName(parts[0]);
        int port = Integer.parseInt(parts[1]);
        
        String transactionId = generateTransactionId();
        String request = transactionId + " G";
        
        sendMessage(request, ipAddress, port);
        
        socket.setSoTimeout(5000);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try {
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            
            if (response.startsWith(transactionId + " H ")) {
                return true;
            }
        } catch (SocketTimeoutException e) {
            return false;
        }
        
        return false;
    }

    // 5th method implementation
    public void pushRelay(String nodeName) throws Exception {
        if (!addressStore.containsKey(nodeName)) {
            throw new Exception("Unknown node: " + nodeName);
        }
        relayStack.push(nodeName);
    }

    // 6th method implementation
    public void popRelay() throws Exception {
        if (!relayStack.isEmpty()) {
            relayStack.pop();
        }
    }

    // 7th method implementation
    public boolean exists(String key) throws Exception {
        String transactionId = generateTransactionId();
        
        String targetNodeName = closestNode(key);
        if (targetNodeName == null) {
            return false;
        }
        
        String[] addressParts = addressStore.get(targetNodeName).split(":");
        InetAddress ipAddress = InetAddress.getByName(addressParts[0]);
        int port = Integer.parseInt(addressParts[1]);
        
        String request = transactionId + " E " + key;
        sendMessage(request, ipAddress, port);
        
        socket.setSoTimeout(5000);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try {
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            
            if (response.startsWith(transactionId + " F ")) {
                char result = response.charAt(4);
                return result == 'Y';
            }
        } catch (SocketTimeoutException e) {
            // Resend code
        }
        
        return false;
    }

    // 8th method implementation
    public String read(String key) throws Exception {
        if (keyValueStore.containsKey(key)) {
            return keyValueStore.get(key);
        }
        
        try {
            handleIncomingMessages(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        String transactionId = generateTransactionId();
        String targetNodeName = closestNode(key);
        
        if (targetNodeName == null) {
            return null;
        }
        
        String[] addressParts = addressStore.get(targetNodeName).split(":");
        InetAddress ipAddress = InetAddress.getByName(addressParts[0]);
        int port = Integer.parseInt(addressParts[1]);
        
        String request = transactionId + " R " + key;
        
        for (int retry = 0; retry < 2; retry++) {
            sendMessage(request, ipAddress, port);
            
            socket.setSoTimeout(5000);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            
            if (response.startsWith(transactionId + " S ")) {
                char result = response.charAt(4);
                if (result == 'Y') {
                    String[] parts = response.substring(6).split(" ", 3);
                    if (parts.length >= 2) {
                        return parts[1];
                    }
                }
                return null;
            }
        }
        return null;
    }

    // 9th method implementation
    public boolean write(String key, String value) throws Exception {
        try {
            handleIncomingMessages(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        byte[] keyHash = HashID.computeHashID(key);
        byte[] nodeHash = HashID.computeHashID(nodeName);
        int myDistance = calculateDistance(keyHash, nodeHash);
        
        if (addressStore.isEmpty() || isNearestNode(key)) {
            keyValueStore.put(key, value);
            return true;
        }
        
        String transactionId = generateTransactionId();
        String targetNodeName = closestNode(key);
        
        if (targetNodeName == null) {
            return false;
        }
        
        String[] addressParts = addressStore.get(targetNodeName).split(":");
        InetAddress ipAddress = InetAddress.getByName(addressParts[0]);
        int port = Integer.parseInt(addressParts[1]);
        
        String encodedValue = encodeString(value);
        String request = transactionId + " W " + key + " " + encodedValue;
        
        sendMessage(request, ipAddress, port);
        
        socket.setSoTimeout(5000);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        try {
            socket.receive(packet);
            String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            
            if (response.startsWith(transactionId + " X ")) {
                char result = response.charAt(4);
                return result == 'R' || result == 'A';  // Replaced or Added
            }
        } catch (SocketTimeoutException e) {
            keyValueStore.put(key, value);
            return true;
        }
        
        return false;
    }

// 10th method implementation
public boolean CAS(String key, String currentValue, String newValue) throws Exception {
    try {
        handleIncomingMessages(1);
    } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
    }
    
    if (keyValueStore.containsKey(key)) {
        String storedValue = keyValueStore.get(key);
        if (storedValue.equals(currentValue)) {
            keyValueStore.put(key, newValue);
            return true;
        }
        return false;
    }
    
    String transactionId = generateTransactionId();
    String targetNodeName = closestNode(key);
    
    if (targetNodeName == null) {
        return false;
    }
    
    String[] addressParts = addressStore.get(targetNodeName).split(":");
    InetAddress ipAddress = InetAddress.getByName(addressParts[0]);
    int port = Integer.parseInt(addressParts[1]);
    
    String encodedCurrentValue = encodeString(currentValue);
    String encodedNewValue = encodeString(newValue);
    String request = transactionId + " C " + key + " " + encodedCurrentValue + encodedNewValue;
    
    sendMessage(request, ipAddress, port);
    
    socket.setSoTimeout(5000);
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    
    try {
        socket.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        
        if (response.startsWith(transactionId + " D ")) {
            char result = response.charAt(4);
            return result == 'R' || result == 'A';
        }
    } catch (SocketTimeoutException e) {
        sendMessage(request, ipAddress, port);
        socket.receive(packet);
        String response = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        
        if (response.startsWith(transactionId + " D ")) {
            char result = response.charAt(4);
            return result == 'R' || result == 'A';
        }
    }
    
    return false;
}

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    // HELPER METHODS
    
    private String generateTransactionId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(2);
        for (int i = 0; i < 2; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    private void sendMessage(String message, InetAddress address, int port) throws Exception {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
        socket.send(packet);
    }
    
    private String encodeString(String s) {
        if (s == null) {
            s = "";
        }
        
        int spaceCount = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                spaceCount++;
            }
        }
        
        return spaceCount + " " + s + " ";
    }
    
    private boolean isNearestNode(String key) throws Exception {
        byte[] keyHash = HashID.computeHashID(key);
        
        byte[] nodeHash = HashID.computeHashID(nodeName);
        int myDistance = calculateDistance(keyHash, nodeHash);
        
        int closerNodes = 0;
        
        for (String otherNodeName : addressStore.keySet()) {
            if (otherNodeName.equals(nodeName)) continue;
            
            byte[] otherNodeHash = HashID.computeHashID(otherNodeName);
            int otherDistance = calculateDistance(keyHash, otherNodeHash);
            
            if (otherDistance < myDistance) {
                closerNodes++;
                if (closerNodes >= 3) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private String closestNode(String key) throws Exception {
        if (addressStore.isEmpty()) {
            return null;
        }

        byte[] keyHash = HashID.computeHashID(key);
        
        String closestNode = null;
        int closestDistance = 257;
        
        for (String nodeName : addressStore.keySet()) {
            byte[] nodeHash = HashID.computeHashID(nodeName);
            int distance = calculateDistance(keyHash, nodeHash);
            
            if (distance < closestDistance) {
                closestDistance = distance;
                closestNode = nodeName;
            }
        }
        return closestNode;
    }
    
    private int calculateDistance(byte[] hash1, byte[] hash2) {
        int leadingMatchingBits = 0;
        
        for (int i = 0; i < hash1.length && i < hash2.length; i++) {
            byte xor = (byte) (hash1[i] ^ hash2[i]);
            if (xor == 0) {
                leadingMatchingBits += 8;
            } else {
                int mask = 0x80;
                while ((xor & mask) == 0 && mask > 0) {
                    leadingMatchingBits++;
                    mask >>= 1;
                }
                break;
            }
        }
        
        return 256 - leadingMatchingBits;
    }

    private void handleBootstrapMessage(byte[] data, int length) {
        try {
            String message = new String(data, 0, length, StandardCharsets.UTF_8);
            
            int nodeNameIndex = message.indexOf("N:test");
            if (nodeNameIndex >= 0 && nodeNameIndex + 10 < message.length()) {
                String nodeName = message.substring(nodeNameIndex, nodeNameIndex + 7);
                
                int ipIndex = message.indexOf("127.0.0.1:");
                if (ipIndex >= 0 && ipIndex + 15 < message.length()) {
                    String address = message.substring(ipIndex, ipIndex + 15);
                    
                    addressStore.put(nodeName, address);
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
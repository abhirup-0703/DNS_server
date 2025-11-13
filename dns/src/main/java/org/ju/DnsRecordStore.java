package org.ju;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ju.model.DnsClass;
import org.ju.model.DnsResourceRecord;

/**
 * Stores DNS records organized by domain name.
 * Simulates a distributed database by holding Root, TLD, and Authoritative records.
 */
public class DnsRecordStore {

    // private static final Logger LOGGER = LoggerFactory.getLogger(DnsRecordStore.class);
    
    // Map: Domain Name -> List of Records associated with that name
    private final Map<String, List<DnsResourceRecord>> zoneData = new HashMap<>();

    // static {
    //     try {
    //         // --- 1. ROOT ZONE (.) ---
    //         // The Root knows who handles "com." and "org."
    //         // We point these NS records to "localhost" (127.0.0.1) because we are simulating
    //         // the whole internet on one machine. In real life, these would be different IPs.
    //         addRecord("com.", DnsType.NS, "127.0.0.1:5001"); 
    //         addRecord("org.", DnsType.NS, "127.0.0.1:5002");

    //         // --- 2. TLD ZONE (com.) ---
    //         // The .com server knows who handles "example.com" and "google.com"
    //         addRecord("example.com.", DnsType.NS, "127.0.0.1:5004");
    //         addRecord("google.com.", DnsType.NS, "127.0.0.1:5003");
    //         addRecord("test.org.", DnsType.NS, "127.0.0.1:5005");

    //         // --- 3. AUTHORITATIVE ZONES ---
    //         // These servers know the actual IP addresses (A Records)
    //         addRecord("www.example.com.", DnsType.A, "192.168.1.10");
    //         addRecord("mail.example.com.", DnsType.A, "192.168.1.20");
    //         addRecord("www.google.com.", DnsType.A, "8.8.8.8");
            
    //         // Add a record for org just to test
    //         addRecord("www.test.org.", DnsType.A, "10.0.0.50");

    //     } catch (Exception e) {
    //         LOGGER.error("Failed to init store", e);
    //     }
    // }

    public void addRecord(String name, int type, String dataIp, int port) throws Exception {
        byte[] rDataIP = InetAddress.getByName(dataIp).getAddress();
        byte[] rDataPort = new byte[]{(byte)(port/256), (byte)(port%256)};
        byte[] rData = new byte[rDataIP.length + rDataPort.length];
        System.arraycopy(rDataIP, 0, rData, 0, rDataIP.length);
        System.arraycopy(rDataIP, 0, rData, rDataIP.length, rDataPort.length);
        DnsResourceRecord record = new DnsResourceRecord(
            name, type, DnsClass.IN, 3600, rData.length, rData
        );
        
        zoneData.computeIfAbsent(name, k -> new ArrayList<>()).add(record);
    }

    public void addRecord(String name, int type, String dataIp) throws Exception {
        byte[] rData = InetAddress.getByName(dataIp).getAddress();
        DnsResourceRecord record = new DnsResourceRecord(
            name, type, DnsClass.IN, 3600, rData.length, rData
        );
        
        zoneData.computeIfAbsent(name, k -> new ArrayList<>()).add(record);
    }

    /**
     * Iterative Lookup Logic:
     * Attempts to find the exact match. If not found, strips the prefix
     * (e.g., www.google.com -> google.com) to find the closest "Parent"
     * that has an NS record.
     */
    public List<DnsResourceRecord> findClosestMatch(String domain) {
        String current = domain;
        
        while (!current.isEmpty()) {
            if (zoneData.containsKey(current)) {
                return zoneData.get(current);
            }

            // Logic to strip the first label: "www.example.com." -> "example.com."
            int dotIndex = current.indexOf('.');
            if (dotIndex == -1 || dotIndex == current.length() - 1) {
                // We reached the end or just a dot. Check root "."
                return zoneData.get("."); // Might return null if root isn't defined or needed
            }
            current = current.substring(dotIndex + 1);
        }
        return null;
    }
}
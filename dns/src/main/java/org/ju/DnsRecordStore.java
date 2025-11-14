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
    
    // Map: Domain Name -> List of Records associated with that name
    private final Map<String, List<DnsResourceRecord>> zoneData = new HashMap<>();
    
    public void addRecord(String name, int type, String dataIp, int port) throws Exception {
        byte[] rDataIP = InetAddress.getByName(dataIp).getAddress();
        byte[] rDataPort = new byte[]{(byte)(port/256), (byte)(port%256)};
        byte[] rData = new byte[rDataIP.length + rDataPort.length];
        System.arraycopy(rDataIP, 0, rData, 0, rDataIP.length);
        System.arraycopy(rDataPort, 0, rData, rDataIP.length, rDataPort.length);
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
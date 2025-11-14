package org.ju.model;

import java.net.UnknownHostException;
import java.util.Arrays;

public class DnsResourceRecord {

    // Variable length fields from the Question section
    private final String name;      // e.g., "www.example.com"
    private final int type;         // e.g., DnsType.A
    private final int rClass;       // e.g., DnsClass.IN

    // Fixed length fields
    private final long ttl;         // 4 bytes: Time-to-live in seconds
    private final int rdLength;     // 2 bytes: Length of the RDATA field
    private final byte[] rData;     // The actual data (e.g., 4 bytes for an IPv4)

    public DnsResourceRecord(String name, int type, int rClass, long ttl, int rdLength, byte[] rData) {
        this.name = name;
        this.type = type;
        this.rClass = rClass;
        this.ttl = ttl;
        this.rdLength = rdLength;
        this.rData = rData;
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getRClass() {
        return rClass;
    }

    public long getTtl() {
        return ttl;
    }

    public int getRdLength() {
        return rdLength;
    }

    public byte[] getRData() {
        return rData;
    }

    @Override
    public String toString() {
        // A helper to make rData readable for logging (e.g., "[C0, A8, 01, 0A]")
        String rDataString = Arrays.toString(rData);
        if (type == DnsType.A) {
            // Try to format IPv4 for better logging
            try {
                rDataString = java.net.InetAddress.getByAddress(rData).getHostAddress();
            } catch (UnknownHostException e) {
                // ignore
            }
        }

        return "DnsResourceRecord{" +
                "name='" + name + '\'' +
                ", type=" + DnsType.toString(type) +
                ", rClass=" + DnsClass.toString(rClass) +
                ", ttl=" + ttl +
                ", rdLength=" + rdLength +
                ", rData=" + rDataString +
                '}';
    }
}
package org.ju.model;


public class DnsClass {
    public static final int IN = 1; // Internet
    
    public static String toString(int dnsClass) {
        if (dnsClass == IN) {
            return "IN";
        }
        return "UNKNOWN (" + dnsClass + ")";
    }
}
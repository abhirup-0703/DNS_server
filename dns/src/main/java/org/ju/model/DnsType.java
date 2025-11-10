package org.ju.model;

public class DnsType {
    public static final int A = 1;      // Host Address (IPv4)
    public static final int NS = 2;     // Authoritative Name Server
    public static final int CNAME = 5;  // Canonical Name
    public static final int SOA = 6;    // Start of Authority
    public static final int PTR = 12;   // Pointer Record
    public static final int MX = 15;    // Mail Exchange
    public static final int TXT = 16;   // Text Record
    public static final int AAAA = 28;  // Host Address (IPv6)

    public static String toString(int type) {
        switch (type) {
            case A: return "A";
            case NS: return "NS";
            case CNAME: return "CNAME";
            case SOA: return "SOA";
            case PTR: return "PTR";
            case MX: return "MX";
            case TXT: return "TXT";
            case AAAA: return "AAAA";
            default: return "UNKNOWN (" + type + ")";
        }
    }
}
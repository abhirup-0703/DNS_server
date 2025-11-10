package org.ju;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple in-memory "database" or "zone file" for our DNS server.
 * It maps domain names (String) to their 4-byte IPv4 addresses (byte[]).
 */
public class DnsRecordStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsRecordStore.class);

    // We store the IP as a byte[] to avoid repeated conversion.
    private static final Map<String, byte[]> store = new HashMap<>();

    static {
        // Populate our fake records
        // Note: The keys MUST end with a "." to be a valid FQDN.
        try {
            store.put("www.example.com.", InetAddress.getByName("192.168.1.10").getAddress());
            store.put("app.example.com.", InetAddress.getByName("192.168.1.11").getAddress());
            store.put("api.test.net.", InetAddress.getByName("10.0.0.5").getAddress());
            store.put("google.com.", InetAddress.getByName("8.8.8.8").getAddress());
            store.put("localhost.", InetAddress.getByName("127.0.0.1").getAddress());
        } catch (UnknownHostException e) {
            // This should not happen with hardcoded valid IPs
            LOGGER.error("Failed to initialize DNS record store", e);
        }
    }

    /**
     * Looks up the 4-byte IPv4 address for a given domain name.
     *
     * @param domainName The fully qualified domain name (e.g., "www.example.com.").
     * @return The 4-byte IP address, or null if not found.
     */
    public static byte[] getIpAddress(String domainName) {
        LOGGER.info("Store: Looking up record for: {}", domainName);
        return store.get(domainName);
    }
}
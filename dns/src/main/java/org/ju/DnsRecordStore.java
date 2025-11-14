package org.ju;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ju.model.DnsClass;
import org.ju.model.DnsResourceRecord;
import org.ju.model.DnsType; // Added import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores DNS records organized by domain name, persistent to a file.
 * Each server port gets its own file in the 'dns_storage' directory.
 */
public class DnsRecordStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DnsRecordStore.class);
    private static final Path STORAGE_DIR = Paths.get("dns_storage");
    private final Path zoneFile;

    /**
     * Creates a record store linked to a specific port's file.
     * @param port The port number of the server, used to name the file.
     */
    public DnsRecordStore(int port) {
        try {
            Files.createDirectories(STORAGE_DIR);
            this.zoneFile = STORAGE_DIR.resolve(port + ".dns");
            if (!Files.exists(zoneFile)) {
                Files.createFile(zoneFile);
            }
            LOGGER.info("DnsRecordStore bound to file: {}", zoneFile.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Failed to initialize storage file {}: {}", port + ".dns", e.getMessage());
            throw new RuntimeException("Failed to initialize record store", e);
        }
    }

    /**
     * Reads all records from the persistent file and loads them into a map.
     * @return A map of Domain Name -> List of Records
     */
    private Map<String, List<DnsResourceRecord>> loadRecordsFromFile() {
        Map<String, List<DnsResourceRecord>> zoneData = new HashMap<>();
        List<String> lines;
        try {
            // Ensure file exists before reading
            if (!Files.exists(zoneFile)) {
                LOGGER.warn("Zone file {} does not exist, returning empty map.", zoneFile);
                return zoneData;
            }
            lines = Files.readAllLines(zoneFile);
        } catch (IOException e) {
            LOGGER.error("Failed to read zone file {}: {}", zoneFile, e.getMessage());
            return zoneData; // Return empty map
        }

        for (String line : lines) {
            if (line.trim().isEmpty()) continue; // Skip empty lines
            try {
                // File format: name,type,ip,port
                String[] parts = line.split(",");
                if (parts.length < 3) {
                     LOGGER.warn("Skipping malformed line in {}: {}", zoneFile.getFileName(), line);
                     continue;
                }

                String name = parts[0];
                int type = Integer.parseInt(parts[1]);
                String dataIp = parts[2];
                int port = (parts.length == 4) ? Integer.parseInt(parts[3]) : 0;

                DnsResourceRecord record;
                if (type == DnsType.NS) {
                    byte[] rDataIP = InetAddress.getByName(dataIp).getAddress();
                    byte[] rDataPort = new byte[]{(byte)(port/256), (byte)(port%256)};
                    byte[] rData = new byte[rDataIP.length + rDataPort.length];
                    System.arraycopy(rDataIP, 0, rData, 0, rDataIP.length);
                    System.arraycopy(rDataPort, 0, rData, rDataIP.length, rDataPort.length);
                    record = new DnsResourceRecord(name, type, DnsClass.IN, 3600, rData.length, rData);
                } else { // Assume A record or other type without port
                    byte[] rData = InetAddress.getByName(dataIp).getAddress();
                    record = new DnsResourceRecord(name, type, DnsClass.IN, 3600, rData.length, rData);
                }
                
                zoneData.computeIfAbsent(name, k -> new ArrayList<>()).add(record);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse record line: '{}'. Error: {}", line, e.getMessage());
            }
        }
        return zoneData;
    }

    /**
     * Appends a new NS record to the persistent file.
     */
    public void addRecord(String name, int type, String dataIp, int port) throws Exception {
        // We just write to the file. The original conversion logic is now in loadRecordsFromFile.
        String line = String.format("%s,%d,%s,%d\n", name, type, dataIp, port);
        try {
            Files.writeString(zoneFile, line, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            LOGGER.info("Appended NS record to {}: {}", zoneFile.getFileName(), line.trim());
        } catch (IOException e) {
            LOGGER.error("Failed to write record to file {}: {}", zoneFile, e.getMessage());
            throw new RuntimeException("Failed to write record", e);
        }
    }

    /**
     * Appends a new A record to the persistent file.
     */
    public void addRecord(String name, int type, String dataIp) throws Exception {
        // Use 0 as a placeholder for port in A records
        String line = String.format("%s,%d,%s,%d\n", name, type, dataIp, 0);
         try {
            Files.writeString(zoneFile, line, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            LOGGER.info("Appended A record to {}: {}", zoneFile.getFileName(), line.trim());
        } catch (IOException e) {
            LOGGER.error("Failed to write record to file {}: {}", zoneFile, e.getMessage());
            throw new RuntimeException("Failed to write record", e);
        }
    }

    /**
     * Iterative Lookup Logic:
     * Loads all records from file, then finds the exact match.
     * If not found, strips the prefix (e.g., www.google.com -> google.com)
     * to find the closest "Parent" that has an NS record.
     */
    public List<DnsResourceRecord> findClosestMatch(String domain) {
        // Load fresh data from file *every time* a query comes in.
        // This ensures new records added from the God GUI are found.
        Map<String, List<DnsResourceRecord>> zoneData = loadRecordsFromFile();
        
        if (zoneData.isEmpty()) {
            LOGGER.warn("Zone data for {} is empty.", zoneFile.getFileName());
        }
        
        String current = domain;
        
        while (!current.isEmpty()) {
            if (zoneData.containsKey(current)) {
                return zoneData.get(current);
            }

            // Logic to strip the first label: "www.example.com." -> "example.com."
            int dotIndex = current.indexOf('.');
            if (dotIndex == -1 || dotIndex == current.length() - 1) {
                // We reached the end or just a dot. Check root "."
                return zoneData.get("."); // Might return null if root isn't defined
            }
            current = current.substring(dotIndex + 1);
        }
        return null;
    }
}
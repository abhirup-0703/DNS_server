package org.ju;

// Import MongoDB classes
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.Binary; // For storing byte[] data

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.ju.model.DnsClass;
import org.ju.model.DnsResourceRecord;
// import org.ju.model.DnsType; // Good practice to import

/**
 * Stores DNS records in a MongoDB database.
 * Connects to a MongoDB collection named 'dnsRecords'.
 */
public class DnsRecordStore {
    
    // --- STEP 1: PASTE YOUR ATLAS CONNECTION STRING HERE ---
    /**
     * Replace this placeholder with your actual MongoDB Atlas connection string.
     * Get this from the Atlas UI (Connect > Drivers > Java).
     * It looks like: "mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority"
     */
    private static final String ATLAS_CONNECTION_STRING = "mongodb+srv://deeznerdz67_db_user:tAIBtmXiaDCfOfqV@cluster0.2m4q6sa.mongodb.net/?appName=Cluster0";
    
    // --- STEP 2: SET YOUR DATABASE NAME ---
    /**
     * You can change this to whatever you named your database in Atlas.
     */
    private static final String DATABASE_NAME = "dns_db";
    
    /**
     * You can change this to whatever you want your collection to be named.
     */
    private static final String COLLECTION_NAME = "dnsRecords";


    private final MongoCollection<Document> recordCollection;

    /**
     * Initializes the record store by connecting to the MongoDB collection
     * using the connection string defined in this file.
     */
    public DnsRecordStore() {
        if (ATLAS_CONNECTION_STRING.startsWith("mongodb+srv://YOUR_USERNAME")) {
            System.err.println("*******************************************************************");
            System.err.println("WARNING: You are using the default placeholder connection string.");
            System.err.println("Please edit ATLAS_CONNECTION_STRING in DnsRecordStore.java");
            System.err.println("*******************************************************************");
            throw new IllegalArgumentException("Invalid MongoDB Connection String. Please update placeholder.");
        }

        // 1. Create the MongoDB Client
        MongoClient mongoClient = MongoClients.create(ATLAS_CONNECTION_STRING);
        
        // 2. Get (or create) your database
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        
        // 3. Get (or create) your collection
        this.recordCollection = database.getCollection(COLLECTION_NAME);
        
        // Create an index on the 'name' field for faster lookups
        // This is idempotent; it won't re-create if it already exists.
        try {
            this.recordCollection.createIndex(Indexes.ascending("name"));
        } catch (Exception e) {
            System.err.println("Warning: Could not create index on 'name'. May be a permissions issue or DB is down.");
            // We don't stop the app, but log the warning.
            e.printStackTrace();
        }
    }
    
    /**
     * Converts a DnsResourceRecord object into a MongoDB Document.
     * This is a private helper method.
     */
    private Document recordToDocument(DnsResourceRecord record) {
        return new Document("name", record.getName())
                .append("type", record.getType())
                .append("class", record.getRClass())
                .append("ttl", record.getTtl()) // Stored as Long
                .append("rdlength", record.getRdLength())
                .append("rdata", new Binary(record.getRData())); // Store byte[] as BSON Binary type
    }

    /**
     * Converts a MongoDB Document back into a DnsResourceRecord object.
     * This is a private helper method.
     */
    private DnsResourceRecord documentToRecord(Document doc) {
        String name = doc.getString("name");
        int type = doc.getInteger("type");
        int rClass = doc.getInteger("class");
        
        // Handle TTL being stored as Long (preferred) or Integer (fallback)
        long ttl;
        Object ttlObj = doc.get("ttl");
        if (ttlObj instanceof Long) {
            ttl = (Long) ttlObj;
        } else if (ttlObj instanceof Integer) {
            ttl = ((Integer) ttlObj).longValue();
        } else {
            // Default or error
            System.err.println("Warning: TTL format unknown for " + name + ". Defaulting to 3600.");
            ttl = 3600;
        }
        
        int rdLength = doc.getInteger("rdlength");
        
        // Retrieve BSON Binary data and convert back to byte[]
        Binary rdataBinary = (Binary) doc.get("rdata");
        byte[] rData = rdataBinary.getData();

        return new DnsResourceRecord(name, type, rClass, ttl, rdLength, rData);
    }

    /**
     * Adds a new DNS record to the MongoDB database.
     * This version constructs the record from IP and port.
     */
    public void addRecord(String name, int type, String dataIp, int port) throws Exception {
        byte[] rDataIP = InetAddress.getByName(dataIp).getAddress();
        byte[] rDataPort = new byte[]{(byte)(port / 256), (byte)(port % 256)};
        byte[] rData = new byte[rDataIP.length + rDataPort.length];
        System.arraycopy(rDataIP, 0, rData, 0, rDataIP.length);
        System.arraycopy(rDataPort, 0, rData, rDataIP.length, rDataPort.length);
        
        DnsResourceRecord record = new DnsResourceRecord(
            name, type, DnsClass.IN, 3600, rData.length, rData
        );
        
        // Convert to Document and insert
        Document doc = recordToDocument(record);
        recordCollection.insertOne(doc);
        System.out.println("Inserted record into MongoDB: " + doc.toJson());
    }

    /**
     * Adds a new DNS record to the MongoDB database.
     * This version constructs the record from just an IP.
     */
    public void addRecord(String name, int type, String dataIp) throws Exception {
        byte[] rData = InetAddress.getByName(dataIp).getAddress();
        DnsResourceRecord record = new DnsResourceRecord(
            name, type, DnsClass.IN, 3600, rData.length, rData
        );

        // Convert to Document and insert
        Document doc = recordToDocument(record);
        recordCollection.insertOne(doc);
        System.out.println("Inserted record into MongoDB: " + doc.toJson());
    }

    /**
     * Iterative Lookup Logic (MongoDB Version):
     * Attempts to find the exact match by querying MongoDB.
     * If not found, strips the prefix (e.g., www.google.com -> google.com)
     * and queries again, simulating the iterative DNS lookup process.
     */
    public List<DnsResourceRecord> findClosestMatch(String domain) {
        String current = domain;
        
        while (current != null && !current.isEmpty()) {
            // 1. Query MongoDB for the current domain name
            List<DnsResourceRecord> records = new ArrayList<>();
            // Use a Consumer to process each document found
            Consumer<Document> processDoc = doc -> records.add(documentToRecord(doc));
            recordCollection.find(Filters.eq("name", current)).forEach(processDoc);

            if (!records.isEmpty()) {
                // 2. Found a match. Return all records for this name.
                return records;
            }

            // 3. No match found. Strip the first label.
            // "www.example.com." -> "example.com."
            int dotIndex = current.indexOf('.');
            if (dotIndex == -1 || dotIndex == current.length() - 1) {
                // We reached the end (e.g., "com.") or just a dot.
                // Try to find the root "."
                current = "."; // Check for root
                if (domain.equals(".")) {
                    // We already checked root and found nothing, so break
                    break; 
                }
            } else {
                current = current.substring(dotIndex + 1);
            }
        }
        return null; // No match found at all
    }
}
package org.ju.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * A utility class to handle DNS label encoding (compression is not supported).
 * This converts between "www.example.com" and the (3)www(7)example(3)com(0) format.
 */
public class DnsLabelEncoder {

    /**
     * Encodes a standard domain name into the DNS label format.
     *
     * @param domainName The domain name (e.g., "www.example.com").
     * @return A byte array in label-encoded format.
     */
    public static byte[] encode(String domainName) {
        // Handle the root domain "."
        if (".".equals(domainName)) {
            return new byte[]{0};
        }

        // Use ByteArrayOutputStream to build the byte array
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Remove trailing dot if present
            String name = domainName.endsWith(".") ?
                    domainName.substring(0, domainName.length() - 1) : domainName;

            // Split the domain by its labels
            String[] labels = name.split("\\.");
            for (String label : labels) {
                if (label.length() > 63) {
                    throw new IllegalArgumentException("Label is too long: " + label);
                }
                // Write the length byte
                baos.write((byte) label.length());
                // Write the label bytes
                baos.write(label.getBytes(StandardCharsets.US_ASCII));
            }
            // Write the null terminator (root label)
            baos.write((byte) 0);
            return baos.toByteArray();
            
        } catch (IOException e) {
            // This should not happen with a ByteArrayOutputStream
            throw new RuntimeException(e);
        }
    }

    /**
     * Decodes a DNS label-encoded name from a ByteBuffer.
     * NOTE: This is a simple decoder and does NOT support compression pointers (0xC0).
     *
     * @param buffer The ByteBuffer containing the DNS message.
     * @return The decoded domain name (e.g., "www.example.com.").
     */
    public static String decode(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (true) {
            // Read the length byte. & 0xFF treats it as unsigned.
            int length = buffer.get() & 0xFF;

            // Check for the null terminator (end of name)
            if (length == 0) {
                break;
            }

            // Check for compression pointer (not supported, but good to detect)
            if ((length & 0xC0) == 0xC0) {
                // We're not handling compression. We must skip the pointer.
                // A pointer is 2 bytes, so read the next byte to advance the buffer.
                buffer.get(); // Skip the second byte of the pointer
                // Ideally, you'd decode the pointer, but for this simple
                // server, we'll stop decoding this name here.
                // In a real client, you'd get the pointer offset and jump.
                // For our server's *responses*, we never use compression.
                // For our client's *requests*, we never use compression.
                // So, we only fail if a remote server sent us a compressed
                // response, which is outside our client's simple scope.
                
                // Let's just break for now.
                // A full implementation would be:
                // int offset = ((length & 0x3F) << 8) | (buffer.get() & 0xFF);
                // long oldPos = buffer.position();
                // buffer.position(offset);
                // sb.append(decode(buffer)); // Recursive call
                // buffer.position(oldPos);
                // break;
                
                // For simplicity, we just stop.
                throw new UnsupportedOperationException("DNS compression pointers (0xC0) are not supported.");

            }

            // Read the label
            byte[] labelBytes = new byte[length];
            buffer.get(labelBytes);
            sb.append(new String(labelBytes, StandardCharsets.US_ASCII));
            sb.append(".");
        }

        // If the name was empty (just a null terminator), it's the root.
        if (sb.length() == 0) {
            return ".";
        }

        return sb.toString();
    }
}
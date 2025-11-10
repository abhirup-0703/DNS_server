package org.ju.util;

import org.ju.model.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles encoding (serializing) a DnsMessage object into a byte array
 * and decoding (parsing) a byte array into a DnsMessage object.
 */
public class DnsMessageCodec {

    // Standard DNS UDP packet size
    private static final int DNS_BUFFER_SIZE = 512;

    /**
     * Decodes a raw byte array from a UDP packet into a DnsMessage object.
     *
     * @param data The raw bytes from the packet.
     * @return A parsed DnsMessage object.
     */
    public DnsMessage decode(byte[] data) {
        // DNS protocol uses Big Endian byte order
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // --- 1. Parse Header ---
        // Read 16-bit (2-byte) fields. & 0xFFFF treats them as unsigned.
        int id = buffer.getShort() & 0xFFFF;
        int flags = buffer.getShort() & 0xFFFF;
        int qdCount = buffer.getShort() & 0xFFFF;
        int anCount = buffer.getShort() & 0xFFFF;
        int nsCount = buffer.getShort() & 0xFFFF;
        int arCount = buffer.getShort() & 0xFFFF;

        DnsHeader header = new DnsHeader(id, flags, qdCount, anCount, nsCount, arCount);

        // --- 2. Parse Questions ---
        List<DnsQuestion> questions = new ArrayList<>(qdCount);
        for (int i = 0; i < qdCount; i++) {
            String qName = DnsLabelEncoder.decode(buffer);
            int qType = buffer.getShort() & 0xFFFF;
            int qClass = buffer.getShort() & 0xFFFF;
            questions.add(new DnsQuestion(qName, qType, qClass));
        }

        // --- 3. Parse Answers (Resource Records) ---
        List<DnsResourceRecord> answers = new ArrayList<>(anCount);
        for (int i = 0; i < anCount; i++) {
            String name = DnsLabelEncoder.decode(buffer);
            int type = buffer.getShort() & 0xFFFF;
            int rClass = buffer.getShort() & 0xFFFF;
            // Read 32-bit (4-byte) TTL. & 0xFFFFFFFFL treats it as unsigned.
            long ttl = buffer.getInt() & 0xFFFFFFFFL;
            int rdLength = buffer.getShort() & 0xFFFF;
            byte[] rData = new byte[rdLength];
            buffer.get(rData);
            
            answers.add(new DnsResourceRecord(name, type, rClass, ttl, rdLength, rData));
        }
        
        // We ignore Authority (nsCount) and Additional (arCount) sections for this
        // simple simulator.

        return new DnsMessage(header, questions, answers);
    }

    /**
     * Encodes a DnsMessage object into a raw byte array for sending in a UDP packet.
     *
     * @param message The DnsMessage object to encode.
     * @return A byte array ready to be sent.
     */
    public byte[] encode(DnsMessage message) {
        ByteBuffer buffer = ByteBuffer.allocate(DNS_BUFFER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);

        DnsHeader header = message.getHeader();

        // --- 1. Write Header ---
        buffer.putShort((short) header.getId());
        buffer.putShort((short) header.getFlags());
        buffer.putShort((short) header.getQdCount());
        buffer.putShort((short) header.getAnCount());
        buffer.putShort((short) header.getNsCount());
        buffer.putShort((short) header.getArCount());

        // --- 2. Write Questions ---
        for (DnsQuestion question : message.getQuestions()) {
            buffer.put(DnsLabelEncoder.encode(question.getQName()));
            buffer.putShort((short) question.getQType());
            buffer.putShort((short) question.getQClass());
        }

        // --- 3. Write Answers (Resource Records) ---
        for (DnsResourceRecord record : message.getAnswers()) {
            buffer.put(DnsLabelEncoder.encode(record.getName()));
            buffer.putShort((short) record.getType());
            buffer.putShort((short) record.getRClass());
            buffer.putInt((int) record.getTtl());
            buffer.putShort((short) record.getRdLength());
            buffer.put(record.getRData());
        }

        // --- 4. Prepare byte array for sending ---
        buffer.flip(); // Mark the end of writing
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result); // Copy bytes into the final array
        return result;
    }
}
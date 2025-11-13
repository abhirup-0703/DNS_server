package org.ju.util;

import org.ju.model.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DnsMessageCodec {

    private static final int DNS_BUFFER_SIZE = 512;

    public DnsMessage decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        // --- 1. Parse Header ---
        int id = buffer.getShort() & 0xFFFF;
        int flags = buffer.getShort() & 0xFFFF;
        int qdCount = buffer.getShort() & 0xFFFF;
        int anCount = buffer.getShort() & 0xFFFF;
        int nsCount = buffer.getShort() & 0xFFFF; // Authority Count
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

        // --- 3. Parse Answers ---
        List<DnsResourceRecord> answers = new ArrayList<>(anCount);
        for (int i = 0; i < anCount; i++) {
            answers.add(decodeRecord(buffer));
        }
        
        // --- 4. Parse Authorities (NEW) ---
        List<DnsResourceRecord> authorities = new ArrayList<>(nsCount);
        for (int i = 0; i < nsCount; i++) {
            authorities.add(decodeRecord(buffer));
        }

        // We still ignore Additional records (arCount) for now
        
        return new DnsMessage(header, questions, answers, authorities);
    }

    // Helper to decode a single Resource Record (used for both Answers and Authorities)
    private DnsResourceRecord decodeRecord(ByteBuffer buffer) {
        String name = DnsLabelEncoder.decode(buffer);
        int type = buffer.getShort() & 0xFFFF;
        int rClass = buffer.getShort() & 0xFFFF;
        long ttl = buffer.getInt() & 0xFFFFFFFFL;
        int rdLength = buffer.getShort() & 0xFFFF;
        byte[] rData = new byte[rdLength];
        buffer.get(rData);
        return new DnsResourceRecord(name, type, rClass, ttl, rdLength, rData);
    }

    public byte[] encode(DnsMessage message) {
        ByteBuffer buffer = ByteBuffer.allocate(DNS_BUFFER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);

        DnsHeader header = message.getHeader();

        // --- 1. Write Header ---
        buffer.putShort((short) header.getId());
        buffer.putShort((short) header.getFlags());
        buffer.putShort((short) header.getQdCount());
        // Use the actual list sizes for counts
        buffer.putShort((short) message.getAnswers().size());      // anCount
        buffer.putShort((short) message.getAuthorities().size());  // nsCount (Updated)
        buffer.putShort((short) header.getArCount());

        // --- 2. Write Questions ---
        for (DnsQuestion question : message.getQuestions()) {
            buffer.put(DnsLabelEncoder.encode(question.getQName()));
            buffer.putShort((short) question.getQType());
            buffer.putShort((short) question.getQClass());
        }

        // --- 3. Write Answers ---
        for (DnsResourceRecord record : message.getAnswers()) {
            encodeRecord(buffer, record);
        }
        
        // --- 4. Write Authorities (NEW) ---
        for (DnsResourceRecord record : message.getAuthorities()) {
            encodeRecord(buffer, record);
        }

        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    // Helper to write a record
    private void encodeRecord(ByteBuffer buffer, DnsResourceRecord record) {
        buffer.put(DnsLabelEncoder.encode(record.getName()));
        buffer.putShort((short) record.getType());
        buffer.putShort((short) record.getRClass());
        buffer.putInt((int) record.getTtl());
        buffer.putShort((short) record.getRdLength());
        buffer.put(record.getRData());
    }
}
package org.ju.model;

import java.util.Collections;
import java.util.List;

/**
 * Represents a full DNS message, containing the header and all sections.
 */
public class DnsMessage {

    private final DnsHeader header;
    private final List<DnsQuestion> questions;
    private final List<DnsResourceRecord> answers;
    
    // NEW: The Authority Section (used for Referrals/NS records)
    private final List<DnsResourceRecord> authorities;

    // Updated Constructor to accept authorities
    public DnsMessage(DnsHeader header, 
                      List<DnsQuestion> questions, 
                      List<DnsResourceRecord> answers, 
                      List<DnsResourceRecord> authorities) {
        this.header = header;
        this.questions = questions;
        this.answers = answers;
        // If null is passed, use an empty list to avoid NullPointerExceptions
        this.authorities = (authorities != null) ? authorities : Collections.emptyList();
    }

    // --- Getters ---

    public DnsHeader getHeader() {
        return header;
    }

    public List<DnsQuestion> getQuestions() {
        return questions;
    }

    public List<DnsResourceRecord> getAnswers() {
        return answers;
    }
    
    // NEW: Getter for authorities
    public List<DnsResourceRecord> getAuthorities() {
        return authorities;
    }

    @Override
    public String toString() {
        return "DnsMessage{\n" +
                "  header=" + header + "\n" +
                "  questions=" + questions + "\n" +
                "  answers=" + answers + "\n" +
                "  authorities=" + authorities + "\n" +
                '}';
    }
}
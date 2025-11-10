package org.ju.model;

import java.util.List;

/**
 * Represents a full DNS message, containing the header and all sections.
 */
public class DnsMessage {

    private final DnsHeader header;
    private final List<DnsQuestion> questions;
    private final List<DnsResourceRecord> answers;
    // We will ignore Authority and Additional records for simplicity
    // private final List<DnsResourceRecord> authorities;
    // private final List<DnsResourceRecord> additionals;

    public DnsMessage(DnsHeader header, List<DnsQuestion> questions, List<DnsResourceRecord> answers) {
        this.header = header;
        this.questions = questions;
        this.answers = answers;
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

    @Override
    public String toString() {
        return "DnsMessage{\n" +
                "  header=" + header + "\n" +
                "  questions=" + questions + "\n" +
                "  answers=" + answers + "\n" +
                '}';
    }
}
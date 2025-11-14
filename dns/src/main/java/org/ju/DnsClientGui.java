package org.ju;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableModel;

public class DnsClientGui extends JFrame {
    private final JTextField domainInput;
    private final JButton resolveButton;
    private final JTable logTable;
    private final DefaultTableModel tableModel;
    private final JTextField resultDisplay;
    
    private final SimpleDnsClient queryResolver;

    public DnsClientGui() {
        setTitle("Iterative DNS Client");
        setSize(800, 600); // Made wider for the 3 columns
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        this.queryResolver = new SimpleDnsClient();

        // --- Top Panel (Input + Result) ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel("Domain:"), BorderLayout.WEST);
        domainInput = new JTextField("www.google.com.");
        inputPanel.add(domainInput, BorderLayout.CENTER);
        resolveButton = new JButton("Resolve");
        inputPanel.add(resolveButton, BorderLayout.EAST);
        
        topPanel.add(inputPanel, BorderLayout.NORTH);

        resultDisplay = new JTextField("Enter a domain and press 'Resolve'");
        resultDisplay.setEditable(false);
        resultDisplay.setFont(new Font("SansSerif", Font.BOLD, 18));
        resultDisplay.setHorizontalAlignment(JTextField.CENTER);
        resultDisplay.setBorder(BorderFactory.createTitledBorder("Final Result"));
        topPanel.add(resultDisplay, BorderLayout.CENTER);
        
        // --- Bottom Panel (Log Table) ---
        // 1. Updated column names
        String[] columnNames = {"Step", "Server Queried", "Log"};
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        logTable = new JTable(tableModel);
        logTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // 2. Updated column widths for 3 columns
        logTable.getColumnModel().getColumn(0).setMaxWidth(50); // Step
        logTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Server
        logTable.getColumnModel().getColumn(2).setPreferredWidth(500); // Log
        
        logTable.setFillsViewportHeight(true);
        
        JScrollPane scrollPane = new JScrollPane(logTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // --- Split Pane ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, scrollPane);
        splitPane.setDividerLocation(150);
        
        add(splitPane, BorderLayout.CENTER);
        
        // --- Action Listener ---
        resolveButton.addActionListener(e -> startQuery());
        getRootPane().setDefaultButton(resolveButton);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startQuery() {
        String domain = domainInput.getText().trim();
        if (domain.isEmpty()) {
            resultDisplay.setText("Please enter a domain name.");
            resultDisplay.setBackground(new Color(0xFF, 0xFF, 0xE0)); // Light yellow
            return;
        }

        // Reset GUI state
        tableModel.setRowCount(0); // Clear the table
        resultDisplay.setText("Resolving " + domain + "...");
        resultDisplay.setBackground(UIManager.getColor("TextField.background"));
        resolveButton.setEnabled(false);

        DnsQueryWorker worker = new DnsQueryWorker(domain);
        worker.execute();
    }

    private class DnsQueryWorker extends SwingWorker<String, Object[]> 
                                 implements SimpleDnsClient.LogCallback {
        private final String domain;

        public DnsQueryWorker(String domain) {
            this.domain = domain;
        }

        @Override
        public void onStepComplete(int step, String serverQueried, String logMessage) {
            publish(new Object[]{step, serverQueried, logMessage});
        }

        @Override
        protected String doInBackground() throws Exception {
            return queryResolver.resolve(domain, this);
        }

        @Override
        protected void process(List<Object[]> chunks) {
            for (Object[] row : chunks) {
                tableModel.addRow(row);
                int lastRow = logTable.getRowCount() - 1;
                logTable.scrollRectToVisible(logTable.getCellRect(lastRow, 0, true));
            }
        }

        @Override
        protected void done() {
            try {
                String finalResult = get();
                resultDisplay.setText(finalResult);
                resultDisplay.setBackground(new Color(0xE0, 0xFF, 0xE0)); // Light green
            } catch (InterruptedException | ExecutionException e) {
                resultDisplay.setText("Error: Resolution failed. See log for details.");
                resultDisplay.setBackground(new Color(0xFF, 0xE0, 0xE0)); // Light red
            }
            resolveButton.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println("Nimbus Look and Feel not available, using default.");
        }
        
        SwingUtilities.invokeLater(DnsClientGui::new);
    }
}
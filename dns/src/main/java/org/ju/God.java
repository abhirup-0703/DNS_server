package org.ju;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.ju.model.DnsType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional; // Import Optional

/**
 * A GUI dashboard to manage all SimpleDnsServer instances.
 * This class replaces the old God.java.
 */
public class God extends JFrame {

    // --- Core Server State ---
    private final List<SimpleDnsServer> servers = new ArrayList<>();
    private final List<Thread> serverThreads = new ArrayList<>();
    
    // --- GUI Components ---
    private final JTextArea logArea;
    private final JButton startPredefinedButton;
    private final JComboBox<SimpleDnsServer> serverSelectorCombo;
    private final DefaultComboBoxModel<SimpleDnsServer> serverComboModel;
    
    private final JComboBox<String> recordTypeCombo;
    private final JTextField recordNameField;
    private final JTextField recordIpField;
    private final JTextField recordPortField;
    private final JLabel recordPortLabel;
    
    private final JTextField newServerNameField;
    private final JTextField newServerPortField;

    // --- NEW ---
    // Start auto-assigning ports from a high number
    // to avoid conflicts with manually-created ones.
    private int nextAvailablePort = 5100;

    public God() {
        setTitle("God (Server Manager Dashboard)");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // 1. --- Main Controls Panel (GridBagLayout) ---
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Section 1: Start Predefined Servers ---
        startPredefinedButton = new JButton("Start Servers From Storage"); // <-- Text Changed
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span 2 columns
        controlsPanel.add(startPredefinedButton, gbc);

        gbc.gridwidth = 1; // Reset span
        gbc.gridy = 1;
        controlsPanel.add(new JSeparator(), gbc);
        controlsPanel.add(new JSeparator(), gbc);

        // --- Section 2: Add Records ---
        gbc.gridy = 2;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Add Record to Server:"), gbc);
        
        gbc.gridx = 1;
        serverComboModel = new DefaultComboBoxModel<>();
        serverSelectorCombo = new JComboBox<>(serverComboModel);
        controlsPanel.add(serverSelectorCombo, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Record Name:"), gbc);
        gbc.gridx = 1;
        recordNameField = new JTextField("www.newzone.com.");
        controlsPanel.add(recordNameField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Record Type:"), gbc);
        gbc.gridx = 1;
        recordTypeCombo = new JComboBox<>(new String[]{"A", "NS"});
        controlsPanel.add(recordTypeCombo, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Data (IP):"), gbc);
        gbc.gridx = 1;
        recordIpField = new JTextField("10.0.0.1");
        controlsPanel.add(recordIpField, gbc);

        gbc.gridy = 6;
        gbc.gridx = 0;
        recordPortLabel = new JLabel("Data (Port):");
        controlsPanel.add(recordPortLabel, gbc);
        gbc.gridx = 1;
        recordPortField = new JTextField("5010");
        controlsPanel.add(recordPortField, gbc);
        
        // Hide port field initially
        recordPortLabel.setEnabled(false);
        recordPortField.setEnabled(false);

        gbc.gridy = 7;
        gbc.gridx = 1; // Align button to the right
        JButton addRecordButton = new JButton("Add Record");
        controlsPanel.add(addRecordButton, gbc);
        
        gbc.gridy = 8;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        controlsPanel.add(new JSeparator(), gbc);

        // --- Section 3: Create New Server ---
        gbc.gridy = 9;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        controlsPanel.add(new JLabel("New Server Name (Zone):"), gbc);
        gbc.gridx = 1;
        newServerNameField = new JTextField("newzone.com.");
        controlsPanel.add(newServerNameField, gbc);

        gbc.gridy = 10;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("New Server Port:"), gbc);
        gbc.gridx = 1;
        newServerPortField = new JTextField("5010");
        controlsPanel.add(newServerPortField, gbc);

        gbc.gridy = 11;
        gbc.gridx = 1; // Align button to the right
        JButton createServerButton = new JButton("Create & Start Server");
        controlsPanel.add(createServerButton, gbc);

        // Add the main controls to the frame
        add(controlsPanel, BorderLayout.NORTH);

        // 2. --- Log Area ---
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        add(scrollPane, BorderLayout.CENTER);

        // 3. --- Attach Action Listeners ---
        startPredefinedButton.addActionListener(e -> startServersFromStorage()); // <-- Method name changed
        addRecordButton.addActionListener(e -> addNewRecord());
        createServerButton.addActionListener(e -> createNewServer());

        // Show/hide port field based on record type
        recordTypeCombo.addActionListener(e -> {
            boolean isNs = "NS".equals(recordTypeCombo.getSelectedItem());
            recordPortLabel.setEnabled(isNs);
            recordPortField.setEnabled(isNs);
        });

        setLocationByPlatform(true);
        setVisible(true);
    }

    /**
     * Helper method to log to the GUI's text area.
     */
    private void log(String message) {
        // Ensure log updates happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Core logic to create a server, add it to lists, and update the GUI.
     * Does NOT start the thread.
     */
    private SimpleDnsServer addServer(int port, String name) {
        SimpleDnsServer server = new SimpleDnsServer(port, name);
        servers.add(server);
        serverThreads.add(new Thread(server::start));
        
        // Update the JComboBox
        serverComboModel.addElement(server);
        
        log("Created server: " + server);
        return server;
    }

    /**
     * ACTION: Scans the 'dns_storage' directory and starts servers for any
     * existing .dns files.
     */
    private void startServersFromStorage() {
        log("--- Scanning 'dns_storage' for existing servers... ---");
        Path storageDir = Paths.get("dns_storage");

        if (!Files.exists(storageDir) || !Files.isDirectory(storageDir)) {
            log("INFO: 'dns_storage' directory not found. No servers to start.");
            // Button's job is done, even if it found nothing.
            startPredefinedButton.setEnabled(false);
            return;
        }

        int serversFound = 0;
        try (var stream = Files.list(storageDir)) {
            List<Path> files = stream
                                .filter(p -> p.getFileName().toString().endsWith(".dns"))
                                .toList();
            
            for (Path path : files) {
                String fileName = path.getFileName().toString();
                try {
                    String portStr = fileName.substring(0, fileName.length() - 4);
                    int port = Integer.parseInt(portStr);
                    
                    // We don't know the "name" (e.g., "google.com.") from the file,
                    // so we'll just name it based on its port.
                    // --> UPDATED: Let's try to infer name from its records
                    String serverName = "Server-" + port;
                    
                    try {
                        // A simple way to find the "highest" level zone in the file
                        // e.g., if file has "google.com." and "www.google.com.",
                        // we guess the server name is "google.com."
                        Optional<String> bestName = Files.lines(path)
                            .map(line -> line.split(","))
                            .filter(parts -> parts.length > 0)
                            .map(parts -> parts[0])
                            .min(Comparator.comparingInt(s -> s.split("\\.").length));
                        
                        if (bestName.isPresent()) {
                            serverName = bestName.get();
                        }
                    } catch(IOException e) {
                        //
                    }
                    
                    // This helper adds to 'servers', 'serverThreads', and GUI combo box
                    addServer(port, serverName); 
                    serversFound++;
                    
                } catch (NumberFormatException e) {
                    log("Skipping invalid file name: " + fileName);
                }
            }
        } catch (IOException e) {
            log("Error reading dns_storage directory: " + e.getMessage());
            return;
        }

        if (serversFound == 0) {
            log("No .dns files found in storage. No servers started.");
        } else {
            // 3. Start all threads
            log("Starting all " + serversFound + " server threads...");
            for (Thread t : serverThreads) {
                t.start();
            }
            log("--- All " + serversFound + " stored servers are running! ---");
        }
        
        startPredefinedButton.setEnabled(false); // Only run once
    }

    /**
     * ACTION: Creates and starts a single new server.
     */
    private void createNewServer() {
        try {
            String name = newServerNameField.getText().trim();
            int port = Integer.parseInt(newServerPortField.getText().trim());
            
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Server Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Use the new recursive logic.
            // This will create all necessary parent servers
            // and use the user-specified port for the final server.
            getOrCreateServer(name, port);
            
            log("--- Server " + name + " and its parents are running! ---");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Port Number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ACTION: Adds a new A or NS record.
     * If the authoritative server for this record does not exist,
     * it will be recursively created.
     */
    private void addNewRecord() {
        try {
            String name = recordNameField.getText().trim();
            String ip = recordIpField.getText().trim();
            String type = (String) recordTypeCombo.getSelectedItem();
            
            if (name.isEmpty() || ip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Record Name and IP cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Determine the authoritative zone for this record.
            // For "www.google.com.", the zone is "google.com."
            // For "google.com." (an NS record), the parent zone is "com."
            String authoritativeZone;
            if (type.equals("NS")) {
                authoritativeZone = getParentDomain(name);
            } else {
                authoritativeZone = getParentDomain(name);
                // This logic might need refinement.
                // For now, let's assume A records are added to their parent domain server.
                // A-Record for "www.google.com." -> add to "google.com." server
                // A-Record for "google.com." -> add to "com." server?
                // Let's refine: The server IS the name.
                // A-Record "www.google.com." -> zone is "google.com."
                // A-Record "google.com." -> zone is "com." (This is debatable, let's stick to getParentDomain)
                
                // Let's redefine:
                // For "www.google.com.", the zone IS "google.com."
                // For "google.com." (as an A record), the zone IS "com."
                // This is correct.
                authoritativeZone = getParentDomain(name);
            }


            // Recursively find or create the server for this zone.
            // Pass 0 to auto-assign a port if it needs to be created.
            SimpleDnsServer server = getOrCreateServer(authoritativeZone, 0);

            if (server == null) {
                log("Error: Could not find or create parent server for " + authoritativeZone);
                JOptionPane.showMessageDialog(this, "Could not find or create server. Is root '.' server running?", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Now, add the record to the correct server.
            if ("A".equals(type)) {
                server.addRecord(name, DnsType.A, ip);
                log("Added A record to " + server.getName() + ": " + name + " -> " + ip);
            } else if ("NS".equals(type)) {
                int port = Integer.parseInt(recordPortField.getText().trim());
                server.addRecord(name, DnsType.NS, ip, port);
                log("Added NS record to " + server.getName() + ": " + name + " -> " + ip + ":" + port);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Port Number for NS record.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- NEW HELPER METHODS ---

    /**
     * Finds the next available port, avoiding known servers.
     */
    private int getNextAvailablePort() {
        boolean portTaken;
        do {
            portTaken = false;
            int portToCheck = nextAvailablePort;
            for (SimpleDnsServer server : servers) {
                if (server.getPort() == portToCheck) {
                    portTaken = true;
                    nextAvailablePort++; // Increment for next time
                    break;
                }
            }
        } while (portTaken);
        
        return nextAvailablePort++; // Return and increment for next use
    }

    /**
     * Finds a running server by its exact zone name.
     */
    private SimpleDnsServer findServerByName(String zoneName) {
        for (SimpleDnsServer server : servers) {
            if (server.getName().equals(zoneName)) {
                return server;
            }
        }
        return null;
    }

    /**
     * The main recursive logic. Finds a server or creates it (and its parents).
     * @param zoneName The zone to find/create (e.g., "google.com.")
     * @param suggestedPort A specific port to use (0 to auto-assign)
     * @return The server instance, or null if root isn't running.
     */
    private SimpleDnsServer getOrCreateServer(String zoneName, int suggestedPort) {
        // 1. Check if it already exists
        SimpleDnsServer server = findServerByName(zoneName);
        if (server != null) {
            return server; // Found it!
        }

        // 2. Base Case: The Root Server
        if (zoneName.equals(".")) {
            // --- THIS IS THE FIX ---
            // Root server must be created manually or via storage.
            // We will NOT auto-create it with an auto-assigned port (suggestedPort == 0).
            // We WILL create it if a specific port is suggested (e.g., 5000 from createNewServer).
            if (suggestedPort > 0) {
                log("Creating new server for " + zoneName + " on port " + suggestedPort);
                // Check if port is taken
                for (SimpleDnsServer s : servers) {
                    if (s.getPort() == suggestedPort) {
                         log("Error: Port " + suggestedPort + " is already in use by " + s.getName());
                         JOptionPane.showMessageDialog(this, "Port " + suggestedPort + " is already in use.", "Error", JOptionPane.ERROR_MESSAGE);
                         return null;
                    }
                }
                SimpleDnsServer newServer = addServer(suggestedPort, zoneName);
                // Start the new server's thread
                serverThreads.get(serverThreads.size() - 1).start();
                return newServer;
            } else {
                // This is a recursive call trying to find the root, but it's not running.
                log("Error: Root server '.' not found. Please start it first.");
                JOptionPane.showMessageDialog(this, "Root server '.' is not running.\nPlease create it (e.g., on port 5000) or Start Servers From Storage.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            // --- END FIX ---
        }

        // 3. Server doesn't exist. We must create it.
        //    But first, ensure its parent exists.
        String parentZone = getParentDomain(zoneName);
        
        // Recursive call to create the parent
        SimpleDnsServer parentServer = getOrCreateServer(parentZone, 0); // Parents always auto-port

        if (parentServer == null) {
            // This happens if the recursion failed (e.g., root is missing)
            log("Error: Failed to find or create parent zone '" + parentZone + "'");
            return null;
        }

        // 4. Parent exists! Now create the new server.
        int newPort = (suggestedPort > 0) ? suggestedPort : getNextAvailablePort();

        // Check if user-suggested port is already taken
        if (suggestedPort > 0) {
            for (SimpleDnsServer s : servers) {
                if (s.getPort() == suggestedPort) {
                    log("Error: Port " + suggestedPort + " is already in use by " + s.getName());
                    JOptionPane.showMessageDialog(this, "Port " + suggestedPort + " is already in use.", "Error", JOptionPane.ERROR_MESSAGE);
                    return null; // Don't create
                }
            }
        }
        
        // Check for name collision (should be covered by step 1, but good for safety)
        if (findServerByName(zoneName) != null) {
            return findServerByName(zoneName);
        }


        log("Creating new server for " + zoneName + " on port " + newPort);
        SimpleDnsServer newServer = addServer(newPort, zoneName);
        
        // Start the new server's thread
        serverThreads.get(serverThreads.size() - 1).start();
        
        // 5. Register this new server with its parent
        log("Auto-registering NS record for '" + zoneName + "' on parent server '" + parentServer.getName() + "'");
        parentServer.addRecord(zoneName, DnsType.NS, "127.0.0.1", newPort);
        
        return newServer;
    }


    /**
     * ACTION: Adds a new A or NS record to the selected server.
     */
    /* THIS METHOD IS REDUNDANT AND REMOVED. The correct one is at line 270.
    private void addNewRecord() {
        SimpleDnsServer selectedServer = (SimpleDnsServer) serverSelectorCombo.getSelectedItem();
        if (selectedServer == null) {
            JOptionPane.showMessageDialog(this, "No server selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String name = recordNameField.getText().trim();
            String ip = recordIpField.getText().trim();
            String type = (String) recordTypeCombo.getSelectedItem();
            
            if (name.isEmpty() || ip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Record Name and IP cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ("A".equals(type)) {
                selectedServer.addRecord(name, DnsType.A, ip);
                log("Added A record to " + selectedServer.getName() + ": " + name + " -> " + ip);
            } else if ("NS".equals(type)) {
                int port = Integer.parseInt(recordPortField.getText().trim());
                selectedServer.addRecord(name, DnsType.NS, ip, port);
                log("Added NS record to " + selectedServer.getName() + ": " + name + " -> " + ip + ":" + port);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Port Number for NS record.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    */

    // --- NEW HELPER METHODS ---

    /**
     * Finds the parent domain for a given domain.
     * e.g., "www.google.com." -> "google.com."
     * e.g., "google.com." -> "com."
     * e.g., "com." -> "."
     */
    private String getParentDomain(String domain) {
        if (domain == null || domain.isEmpty() || domain.equals(".")) {
            return null; // Root has no parent
        }
        int dotIndex = domain.indexOf('.');
        if (dotIndex == -1 || dotIndex == domain.length() - 1) {
            // This is a TLD (like "com.") or invalid, its parent is root.
            return ".";
        }
        return domain.substring(dotIndex + 1);
    }

    /**
     * Finds the best (most specific) running server that is a parent of the given domain.
     * e.g., for "google.com.", servers "com." and "." are parents, but "com." is better.
     */
    // This method is no longer needed by the new logic, but we can keep it
    // or remove it. Let's remove it to clean up.
    /*
    private SimpleDnsServer findBestParentServer(String domain) {
        SimpleDnsServer bestMatch = null;
        int bestMatchLength = -1;

        for (SimpleDnsServer server : servers) {
            String serverName = server.getName();
            // Check if the server's name is a suffix of the domain
            if (domain.endsWith(serverName)) {
                if (serverName.length() > bestMatchLength) {
                    bestMatch = server;
                    bestMatchLength = serverName.length();
                }
            }
        }
        return bestMatch;
    }
    */

    /**
     * Registers a newly created server with its closest existing parent by
     * adding an NS record to that parent.
     */
    // This logic is now *inside* getOrCreateServer, so this is obsolete.
    /*
    private void registerWithParent(SimpleDnsServer newServer) {
        String name = newServer.getName();
        int port = newServer.getPort();

        if (name.equals(".")) {
            return; // Root server doesn't get registered with a parent.
        }

        String parentDomain = getParentDomain(name);
        if (parentDomain == null) {
            return; // Should not happen if not root, but good to check.
        }
        
        SimpleDnsServer parentServer = findBestParentServer(parentDomain);

        if (parentServer != null) {
            log("Auto-registering NS record for '" + name + "' on parent server '" + parentServer.getName() + "'");
            // Add an NS record to the parent server, pointing to the new server
            parentServer.addRecord(name, DnsType.NS, "127.0.0.1", port);
        } else {
            log("WARN: Could not find a running parent server for '" + name + "'. NS record not auto-added.");
        }
    }
    */
    
    // --- END NEW HELPER METHODS ---


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println("Nimbus Look and Feel not available, using default.");
        }
        
        SwingUtilities.invokeLater(God::new);
    }
}
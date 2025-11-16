Recursive DNS Simulator

A Java-based system that simulates the full iterative DNS resolution process, complete with:

A graphical iterative DNS client

A “God-view” server dashboard that creates/manages DNS servers

Persistent, file-based DNS records

Automatic recursive creation of parent domain servers

Dynamic loading of records on every query (no server restart needed)

This system mirrors how real DNS works—from root, to TLD, to authoritative servers.

🚀 Features
✔ Iterative DNS Client

A Swing GUI (DnsClientGui) that shows each referral step during query resolution.

✔ Server Management Dashboard ("God")

A Swing GUI (God) that can:

Create new DNS servers

Start servers from persisted storage

Add DNS records

Trigger auto-creation of parent domain servers

✔ Persistent Storage

Each server stores its records in:

dns_storage/<port>.dns


Example:

dns_storage/5000.dns
dns_storage/5100.dns

✔ Automatic Recursive Server Creation

If you add:

www.a.b.com → 1.2.3.4


The system automatically creates:

b.com. server

a.b.com. server

Links each server with proper NS records

Adds the final A record to the authoritative zone

✔ Dynamic Read-On-Query

Servers do NOT cache DNS files in memory. Every query triggers:

Reading the .dns file

Building a temporary in-memory map

Finding exact match or closest NS referral

Rejecting the temporary map

This ensures real-time updates to DNS behavior.

🧠 System Architecture
DnsClientGui → Root Server (.) → TLD Server (com.) → Domain Server (example.com.)

Example flow for www.example.com.

Client queries Root (5000)

Root refers to com. server

com. refers to example.com. server

example.com. returns authoritative A record

Each step appears in the GUI table.

📦 Project Structure
org/
 └── ju/
      ├── God.java
      ├── DnsClientGui.java
      ├── SimpleDnsServer.java
      ├── SimpleDnsClient.java
      ├── model/
      │     ├── DnsResourceRecord.java
      │     └── ...
      └── util/
            ├── DnsRecordStore.java
            └── ...
dns_storage/

🔧 Prerequisites

Java 11 or higher

slf4j (API + simple binding)

🏗 Compile

Navigate to project root and run:

javac org/ju/*.java org/ju/model/*.java org/ju/util/*.java

▶ Step-By-Step Usage Guide
Step 1 — Run the Server Dashboard
java org.ju.God

First-ever run:

Create Root server:

Field	Value
Name	.
Port	5000

Click Create & Start Server.

Subsequent runs:

Click Start Servers From Storage
→ The dashboard scans dns_storage/ and starts all servers.

Step 2 — Add a Domain

Example:

Field	Value
Record Name	www.my-cool-site.org.
Record Type	A
Data	10.20.30.40

After clicking Add Record, the system will:

Create org. server (if missing)

Create my-cool-site.org. server

Add NS referrals to parent servers

Add your final A record

All automatically.

Step 3 — Run the DNS Client
java org.ju.DnsClientGui

Step 4 — Resolve a Domain

In the GUI, type:

www.my-cool-site.org.


Click Resolve.

You will see:

Full iterative path (. → org. → my-cool-site.org.)

Final IP result: 10.20.30.40

📂 Persistence Format

Each .dns file contains:

name,type_id,ip_address,port


Examples:

NS Record
com.,2,127.0.0.1,5100

A Record
www.example.com.,1,1.2.3.4,0

🧬 Core Components
God.java

Tracks all running servers

Maintains:

List<SimpleDnsServer> servers

List<Thread> serverThreads

Implements recursive server creation:

example.com. → creates com. → creates . (root already exists)

DnsRecordStore.java

Handles persistence.

Key properties:

No memory caching

Reads .dns file on every query

Creates a temporary map

Finds exact or closest NS match

Discards map immediately

This guarantees real-time consistency.

🖥 How DNS Resolution Works in This System (Detailed)

Given query: www.example.com.

1. Query Root (.) at port 5000

Root returns:

Refer to com. at 127.0.0.1:5100

2. Query com. server at port 5100

TLD returns:

Refer to example.com. at 127.0.0.1:5101

3. Query example.com. at port 5101

Authoritative server returns final A record:

1.2.3.4

✔ Summary

This system provides:

Full DNS server hierarchy simulation

Automatic server creation and linking

Persistent storage

GUI tooling for both client and admin

Real-world iterative DNS behavior

If you want, I can also create:

✅ A diagram/image of the DNS workflow
✅ A Quickstart section
✅ A Troubleshooting section
✅ A JAR packaging README
✅ A Dockerized setup
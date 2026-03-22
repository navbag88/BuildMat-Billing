# 🏗️ BuildMat Billing System
### JavaFX Desktop App for Building Material Suppliers

A full-featured billing desktop application with Customer Management, Product Catalog,
Invoice Creation (with GST), Payment Tracking, and PDF Invoice printing.
Data is stored locally in SQLite — no server or internet required.

---

## 📋 Features

| Module        | Features                                                          |
|---------------|-------------------------------------------------------------------|
| Dashboard     | Revenue summary, outstanding balance, invoice counts             |
| Invoices      | Create/edit invoices, line items, GST auto-calc, status tracking |
| Customers     | Add/edit/search customers                                         |
| Products      | Product catalog with stock tracking and low-stock indicators     |
| Payments      | Record cash/cheque/NEFT payments, auto-updates invoice status    |
| PDF Export    | Professional PDF invoice with GST breakdown                      |

---

## 🛠️ Prerequisites

| Tool         | Version   | Download                              |
|--------------|-----------|---------------------------------------|
| Java JDK     | 17 or 21  | https://adoptium.net                  |
| Gradle       | 8.x       | https://gradle.org/install            |

**Verify installation:**
```cmd
java -version
gradle -version
```

---

## 🚀 Step-by-Step: Build & Run

### Step 1 — Clone / Copy the project
```
BillingApp/
├── build.gradle
├── settings.gradle
├── build-and-run.bat       ← Windows: build + launch
├── Run.bat                 ← Windows: launch pre-built JAR
├── package-windows.bat     ← Windows: create .exe installer
└── src/main/java/com/buildmat/
    ├── MainApp.java
    ├── model/      (Customer, Product, Invoice, InvoiceItem, Payment)
    ├── dao/        (CustomerDAO, ProductDAO, InvoiceDAO, PaymentDAO)
    ├── ui/         (MainWindow, DashboardPanel, InvoicePanel, ...)
    └── util/       (DatabaseManager, InvoicePdfGenerator)
```

### Step 2 — Build the fat JAR
```cmd
cd BillingApp
gradle fatJar
```
Output: `build/libs/BuildMat-Billing-1.0.0-all.jar`

Or just double-click `build-and-run.bat` — it builds and launches automatically.

### Step 3 — Run the app
```cmd
java -jar build/libs/BuildMat-Billing-1.0.0-all.jar
```
Or double-click `Run.bat` (copy it next to the JAR).

**On first run:**
- SQLite database `buildmat_billing.db` is created in the working directory
- 10 sample building materials are auto-loaded into the Products catalog

---

## 📦 Deploy on Windows

### Option A — Simple JAR + Batch File (requires Java on PC)
1. Copy `BuildMat-Billing-1.0.0-all.jar` to target PC
2. Copy `Run.bat` next to the JAR
3. Double-click `Run.bat` to launch

The user needs Java 17+ installed: https://adoptium.net

---

### Option B — Standalone EXE Installer (bundles Java, no install needed)

Run `package-windows.bat` on your PC. This uses `jpackage` (part of JDK 14+) to
bundle the app + JVM into a single `.exe` installer.

```cmd
package-windows.bat
```

**Output:** `dist/BuildMat Billing-1.0.0.exe`

This installer:
✅ Bundles the JVM — no Java needed on target PC
✅ Creates a Start Menu shortcut
✅ Creates a Desktop shortcut
✅ Supports uninstall from Windows Add/Remove Programs

> If you see a WiX error, install WiX Toolset: https://wixtoolset.org/releases/
> Or use `--type app-image` for a portable folder instead.

---

### Option C — Portable App Folder (no installer, no Java required)
```cmd
jpackage ^
  --type app-image ^
  --name "BuildMat Billing" ^
  --input build\libs ^
  --main-jar BuildMat-Billing-1.0.0-all.jar ^
  --dest dist
```
Output: `dist/BuildMat Billing/` — copy this entire folder to any Windows PC and run `BuildMat Billing.exe` inside.

---

## 🗄️ Database

The SQLite database is stored as `buildmat_billing.db` in the **same folder as the JAR**.
To back up your data, simply copy this file.

**Tables:**
- `customers` — customer records
- `products` — product catalog with stock
- `invoices` — invoice header with totals and GST
- `invoice_items` — line items per invoice
- `payments` — payment records linked to invoices

---

## 🖨️ PDF Invoices

When you click **PDF** on any invoice:
1. A folder chooser opens
2. The PDF is saved as `INV-2025-0001.pdf`
3. It opens automatically in your default PDF viewer

The PDF includes:
- Company header with GST number
- Customer billing details
- Itemised product table
- Subtotal, GST amount, Grand Total
- Paid / Balance Due
- Status badge (PAID / PARTIAL / UNPAID)

To customise company name/address/GST number, edit `InvoicePdfGenerator.java`:
```java
companyCell.add(new Paragraph("Your Company Name Here")...)
companyCell.add(new Paragraph("GST No: YOUR-GST-NUMBER")...)
```

---

## 🔧 Customise GST

Default GST rate is 18%. Change the default in `Invoice.java`:
```java
this.taxPercent = 18.0;  // ← change this
```
Users can also override GST % per invoice in the invoice form.

---

## 📞 Support & Enhancements

Possible next additions:
- Multi-user support with login
- Email invoice directly from the app
- Reports (monthly sales, top customers)
- Barcode scanner integration for products
- Automatic backup to Google Drive

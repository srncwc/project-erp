package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * รวมชื่อ CSV ที่ระบบใช้ และตรวจข้อมูลกับความสัมพันธ์ระหว่างไฟล์
 * อ่านข้อมูลผ่าน Repository; Main เรียกตรวจชุดข้อมูลก่อนเปิดหน้าต่างหลัก
 */
public class DataFiles {
    public static final String[] NAMES = { "users.csv", "roles.csv", "stock.csv", "sales.csv", "purchases.csv",
            "finance.csv", "customers.csv", "suppliers.csv", "payments.csv" };

    /**
     * อ่านข้อมูลจาก Repository เข้าสู่ array แล้วตรวจตามเงื่อนไขใน method
     * ตรวจบัญชีและ Role ชื่อซ้ำ คู่ค้า สินค้า สต็อก และเอกสารที่ Payment อ้างถึง
     * อ่านไฟล์ทีละไฟล์ ไม่ใช่ snapshot แบบ atomic; ไม่ได้ตรวจทุกกฎเดียวกับ Service ครบทั้งหมด
     */
    public static void verify(File folder) throws IOException {
        UserAccount[] users = new UserRepository(folder).findAll();
        Role[] roles = new RoleRepository(folder).findAll();
        StockItem[] stock = new StockRepository(folder).findAll();
        Sale[] sales = new SaleRepository(folder).findAll();
        Purchase[] purchases = new PurchaseRepository(folder).findAll();
        FinanceEntry[] finance = new FinanceRepository(folder).findAll();
        Partner[] customers = new PartnerRepository(folder, true).findAll();
        Partner[] suppliers = new PartnerRepository(folder, false).findAll();
        Payment[] payments = new PaymentRepository(folder).findAll();
        int admins = 0;
        for (int i = 0; i < users.length; i++) {
            if (users[i].isAdmin()) {
                admins++;
            }
            for (int j = 0; j < i; j++) {
                if (users[i].getUsername().equalsIgnoreCase(users[j].getUsername())) {
                    throw new IOException("Duplicate username.");
                }
            }
            if (!users[i].isAdmin() && users[i].getRoleIds().length() == 0) {
                throw new IOException("USER has no role: " + users[i].getId());
            }
            String[] selected = users[i].getRoleIds().split("\\|", -1);
            for (int j = 0; j < selected.length; j++) {
                if (selected[j].length() == 0 && users[i].isAdmin()) {
                    continue;
                }
                boolean found = false;
                for (int k = 0; k < roles.length; k++) {
                    if (roles[k].getId().equals(selected[j])) {
                        found = true;
                    }
                }
                if (!found) {
                    throw new IOException("Unknown role on account " + users[i].getId());
                }
            }
        }
        if (admins == 0) {
            throw new IOException("At least one ADMIN is required.");
        }
        for (int i = 0; i < roles.length; i++) {
            for (int j = 0; j < i; j++) {
                if (roles[i].getName().equalsIgnoreCase(roles[j].getName())) {
                    throw new IOException("Duplicate role name.");
                }
            }
        }
        uniquePartners(customers);
        uniquePartners(suppliers);
        long[] quantities = new long[stock.length];
        for (int i = 0; i < stock.length; i++) {
            quantities[i] = stock[i].getQuantity();
            for (int j = 0; j < i; j++) {
                if (stock[i].getCode().equalsIgnoreCase(stock[j].getCode())) {
                    throw new IOException("Duplicate product code.");
                }
            }
        }
        FinanceEntry[] all = new FinanceEntry[finance.length + sales.length + purchases.length];
        int at = 0;
        for (int i = 0; i < finance.length; i++) {
            Rules.date(finance[i].getDate());
            all[at++] = finance[i];
        }
        for (int i = 0; i < purchases.length; i++) {
            Purchase item = purchases[i];
            Rules.date(item.getDate());
            requirePartner(suppliers, item.getSupplier());
            quantities[productIndex(stock, item.getProductId())] += item.getQuantity();
            all[at++] = new FinanceEntry("PURCHASE:" + item.getId(), item.getDate(), "EXPENSE", item.getSupplier(),
                    item.total(), "PURCHASE");
        }
        for (int i = 0; i < sales.length; i++) {
            Sale item = sales[i];
            Rules.date(item.getDate());
            requirePartner(customers, item.getCustomer());
            quantities[productIndex(stock, item.getProductId())] -= item.getQuantity();
            all[at++] = new FinanceEntry("SALE:" + item.getId(), item.getDate(), "INCOME", item.getCustomer(),
                    item.total(), "SALE");
        }
        for (int i = 0; i < quantities.length; i++) {
            if (quantities[i] < 0 || quantities[i] > Integer.MAX_VALUE) {
                throw new IOException("Invalid stock balance: " + stock[i].getId());
            }
        }
        for (int i = 0; i < payments.length; i++) {
            boolean found = false;
            for (int j = 0; j < all.length; j++) {
                if (all[j].getId().equals(payments[i].reference)) {
                    found = true;
                    if (!payments[i].voided) {
                        Rules.paymentDate(payments[i].date, all[j].getDate());
                        all[j].addPayment(payments[i].amount);
                    }
                }
            }
            if (!found) {
                throw new IOException("Unknown payment reference: " + payments[i].id);
            }
        }
    }

    /**
     * ค้นหา index สินค้าจาก ID; ถ้าไม่พบให้โยน IOException เพื่อไม่ข้ามเอกสารที่อ้างผิด
     */
    private static int productIndex(StockItem[] stock, String id) throws IOException {
        for (int i = 0; i < stock.length; i++) {
            if (stock[i].getId().equals(id)) {
                return i;
            }
        }
        throw new IOException("Unknown product: " + id);
    }

    /**
     * ตรวจว่าชื่อในเอกสารตรงกับคู่ค้าในทะเบียนแบบตรงทุกตัวอักษร
     * ถ้าไม่พบให้โยน IOException
     */
    private static void requirePartner(Partner[] partners, String name) throws IOException {
        for (int i = 0; i < partners.length; i++) {
            if (partners[i].getName().equals(name)) {
                return;
            }
        }
        throw new IOException("Unknown customer / supplier in documents.");
    }

    /**
     * ตรวจชื่อคู่ค้าซ้ำแบบไม่แยกตัวพิมพ์ใหญ่เล็กภายใน array ที่ได้รับ
     */
    private static void uniquePartners(Partner[] partners) throws IOException {
        for (int i = 0; i < partners.length; i++) {
            for (int j = 0; j < i; j++) {
                if (partners[i].getName().equalsIgnoreCase(partners[j].getName())) {
                    throw new IOException("Duplicate partner name.");
                }
            }
        }
    }
}

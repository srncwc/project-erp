package erp.service;

import erp.model.*;
import erp.repository.*;
import java.io.*;

/**
 * ควบคุมขั้นตอนและกฎธุรกิจของการซื้อ ขาย สต็อก คู่ค้า และการรับจ่ายเงิน
 * ตรวจสิทธิ์และข้อมูลที่เชื่อมกันก่อนส่งให้ Repository บันทึก
 */
public class ErpService {
    private AuthService auth;
    private SaleRepository sales;
    private PurchaseRepository purchases;
    private StockRepository stock;
    private FinanceRepository finance;
    private PaymentRepository payments;
    private File data;

    /**
     * ตรวจสิทธิ์ Sales เมื่อ customer เป็น true หรือ Procurement เมื่อ false
     * แล้วอ่านทะเบียนคู่ค้า
     */
    public Partner[] partners(boolean customer) throws IOException {
        auth.require(customer ? "Sales" : "Procurement");
        return new PartnerRepository(data, customer).findAll();
    }

    /**
     * ตรวจว่าชื่อคู่ค้ามีในทะเบียนลูกค้าหรือผู้จัดหาตามชนิดงาน
     * ถ้าไม่พบให้หยุดก่อนบันทึกเอกสาร
     */
    private void requirePartner(String name, boolean customer) throws IOException {
        Partner[] all = new PartnerRepository(data, customer).findAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getName().equals(name)) {
                return;
            }
        }
        throw new IllegalArgumentException("Select a registered customer / supplier.");
    }

    /**
     * ตรวจว่ามีเอกสารขายหรือซื้อใช้ชื่อคู่ค้านี้อยู่หรือไม่
     */
    private boolean partnerUsed(String name, boolean customer) throws IOException {
        if (customer) {
            Sale[] rows = sales.findAll();
            for (int i = 0; i < rows.length; i++) {
                if (rows[i].getCustomer().equals(name)) {
                    return true;
                }
            }
        } else {
            Purchase[] rows = purchases.findAll();
            for (int i = 0; i < rows.length; i++) {
                if (rows[i].getSupplier().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ตรวจสิทธิ์ผ่าน partners() และห้ามเปลี่ยนชื่อคู่ค้าที่มีเอกสารอ้างแล้ว
     * ช่องทางติดต่อยังแก้ได้ จากนั้นส่งให้ Repository บันทึก
     */
    public void savePartner(Partner value, boolean customer) throws IOException {
        Partner[] all = partners(customer);
        for (int i = 0; i < all.length; i++) {
            if (all[i].getId().equals(value.getId()) && !all[i].getName().equals(value.getName())
                    && partnerUsed(all[i].getName(), customer)) {
                throw new IllegalArgumentException("This name is used by a document. Only contact details can change.");
            }
        }
        new PartnerRepository(data, customer).save(value);
    }

    /**
     * ตรวจสิทธิ์และห้ามลบคู่ค้าที่มีเอกสารอ้างชื่อ ก่อนสั่ง Repository ลบ
     */
    public void deletePartner(String id, boolean customer) throws IOException {
        Partner[] all = partners(customer);
        for (int i = 0; i < all.length; i++) {
            if (all[i].getId().equals(id) && partnerUsed(all[i].getName(), customer)) {
                throw new IllegalArgumentException("This partner is used by a document.");
            }
        }
        new PartnerRepository(data, customer).delete(id);
    }

    /**
     * สร้าง Repository ของแต่ละข้อมูลจากโฟลเดอร์เดียวกัน และเก็บ AuthService
     * ที่รับมาไว้ตรวจผู้ใช้
     */
    public ErpService(File data, AuthService auth) {
        this.auth = auth;
        this.data = data;
        payments = new PaymentRepository(data);
        sales = new SaleRepository(data);
        purchases = new PurchaseRepository(data);
        stock = new StockRepository(data);
        finance = new FinanceRepository(data);
    }

    /**
     * ตรวจสิทธิ์ Sales แล้วคืนรายการขายทั้งหมดจาก Repository
     */
    public Sale[] sales() throws IOException {
        auth.require("Sales");
        return sales.findAll();
    }

    /**
     * ตรวจสิทธิ์ Procurement แล้วคืนรายการซื้อทั้งหมดจาก Repository
     */
    public Purchase[] purchases() throws IOException {
        auth.require("Procurement");
        return purchases.findAll();
    }

    /**
     * ตรวจสิทธิ์ Warehouse แล้วคำนวณยอดคงเหลือจากยอดตั้งต้น + ซื้อ - ขาย
     */
    public StockItem[] stock() throws IOException {
        auth.require("Warehouse");
        return balances(sales.findAll(), purchases.findAll());
    }

    /**
     * ตรวจว่ามีสิทธิ์ Sales, Procurement หรือ Warehouse อย่างน้อยหนึ่งโมดูล
     * คืนสินค้าพร้อมยอดคงเหลือเพื่อใช้เลือกในฟอร์ม ไม่ได้ให้สิทธิ์แก้ทะเบียนสินค้า
     */
    public StockItem[] products() throws IOException {
        if (!auth.canAccess("Sales") && !auth.canAccess("Procurement") && !auth.canAccess("Warehouse")) {
            throw new IllegalArgumentException("No product lookup permission.");
        }
        return balances(sales.findAll(), purchases.findAll());
    }

    /**
     * อ่านทะเบียนสินค้าแล้วค้นหาด้วย ID
     * ถ้าสินค้าหายให้หยุดและแจ้งให้เปิดฟอร์มใหม่
     */
    private StockItem product(String id) throws IOException {
        StockItem[] all = stock.findAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getId().equals(id)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("Product no longer exists. Reopen the form.");
    }

    /**
     * รวมยอดตั้งต้น + จำนวนซื้อ - จำนวนขาย โดยเชื่อมด้วย productId
     * ตรวจว่ายอดอยู่ในช่วง 0 ถึง Integer.MAX_VALUE แล้วคืน StockItem[] ชุดคำนวณ
     * ตัวอย่าง ตั้งต้น 10 ซื้อ 5 ขาย 2 จะเหลือ 13; ไม่เขียน stock.csv
     */
    private StockItem[] balances(Sale[] saleRows, Purchase[] purchaseRows) throws IOException {
        StockItem[] opening = stock.findAll();
        long[] quantities = new long[opening.length];
        for (int i = 0; i < opening.length; i++) {
            quantities[i] = opening[i].getQuantity();
        }
        for (int i = 0; i < purchaseRows.length; i++) {
            int index = productIndex(opening, purchaseRows[i].getProductId());
            quantities[index] += purchaseRows[i].getQuantity();
        }
        for (int i = 0; i < saleRows.length; i++) {
            int index = productIndex(opening, saleRows[i].getProductId());
            quantities[index] -= saleRows[i].getQuantity();
        }
        StockItem[] result = new StockItem[opening.length];
        for (int i = 0; i < opening.length; i++) {
            if (quantities[i] < 0 || quantities[i] > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Stock for " + opening[i].getCode()
                        + " would be outside 0.." + Integer.MAX_VALUE + ". Check quantity and related sales.");
            }
            result[i] = new StockItem(opening[i].getId(), opening[i].getCode(), opening[i].getName(),
                    opening[i].getUnit(), (int) quantities[i]);
        }
        return result;
    }

    /**
     * ค้นหา index สินค้าด้วย ID ถ้าไม่พบให้โยนข้อผิดพลาด ไม่ข้ามเอกสารนั้นเงียบ ๆ
     */
    private int productIndex(StockItem[] rows, String id) {
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].getId().equals(id)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Missing Warehouse reference: " + id);
    }

    /**
     * สร้าง array จำลองหลังเพิ่ม แก้ หรือลบรายการขาย เพื่อใช้ตรวจสต็อกก่อนบันทึก
     * value มี ID ว่างหมายถึงเพิ่ม; deletedId ไม่เป็น null หมายถึงลบ; ยังไม่เขียน
     * CSV
     */
    private Sale[] changedSales(Sale value, String deletedId) throws IOException {
        Sale[] old = sales.findAll();
        if (deletedId != null && old.length == 0) {
            throw new IllegalArgumentException("Sale no longer exists.");
        }
        boolean adding = value != null && value.getId().length() == 0;
        Sale[] next = new Sale[old.length + (adding ? 1 : deletedId != null ? -1 : 0)];
        int at = 0;
        boolean found = adding;
        for (int i = 0; i < old.length; i++) {
            if ((deletedId != null && old[i].getId().equals(deletedId))
                    || (value != null && old[i].getId().equals(value.getId()))) {
                found = true;
                if (deletedId != null) {
                    continue;
                }
                next[at++] = value;
            } else {
                if (at >= next.length) {
                    throw new IllegalArgumentException("Sale no longer exists. Refresh the page.");
                }
                next[at++] = old[i];
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Sale no longer exists. Refresh the page.");
        }
        if (adding) {
            next[at] = value;
        }
        return next;
    }

    /**
     * ตรวจสิทธิ์ กฎเอกสาร ลูกค้า ประวัติรับเงิน และสินค้า จากนั้นทดลองคำนวณสต็อก
     * ถ้าผ่านให้เพิ่มหรือแก้ sales.csv; ID ว่างคือรายการใหม่
     * ยอดสต็อกและเอกสาร Finance จะคำนวณตามเมื่ออ่าน
     * ไม่ได้บันทึกรับเงินจริงอัตโนมัติ
     */
    public void saveSale(Sale value) throws IOException {
        auth.require("Sales");
        Rules.transaction(value.getDate(), value.getCustomer(), value.getQuantity(), value.getUnitPriceSatang());
        requirePartner(value.getCustomer(), true);
        requireUnsettled("SALE:" + value.getId(), false);
        StockItem item = product(value.getProductId());
        Sale checked = new Sale(value.getId(), value.getDate(), value.getCustomer(), item.getName(),
                value.getQuantity(), value.getUnitPriceSatang(), item.getId());
        balances(changedSales(checked, null), purchases.findAll());
        if (checked.getId().length() == 0) {
            sales.add(checked);
            value.setId(checked.getId());
        } else {
            sales.update(checked);
        }
    }

    /**
     * ตรวจสิทธิ์และประวัติ Payment แล้วจำลองสต็อกหลังลบก่อนสั่งลบรายการขาย
     */
    public void deleteSale(String id) throws IOException {
        auth.require("Sales");
        requireUnsettled("SALE:" + id, true);
        balances(changedSales(null, id), purchases.findAll());
        sales.delete(id);
    }

    /**
     * สร้าง array จำลองหลังเพิ่ม แก้ หรือลบรายการซื้อ
     * เพื่อตรวจยอดคงเหลือก่อนเขียนไฟล์
     */
    private Purchase[] changedPurchases(Purchase value, String deletedId) throws IOException {
        Purchase[] old = purchases.findAll();
        if (deletedId != null && old.length == 0) {
            throw new IllegalArgumentException("Purchase no longer exists.");
        }
        boolean adding = value != null && value.getId().length() == 0;
        Purchase[] next = new Purchase[old.length + (adding ? 1 : deletedId != null ? -1 : 0)];
        int at = 0;
        boolean found = adding;
        for (int i = 0; i < old.length; i++) {
            if ((deletedId != null && old[i].getId().equals(deletedId))
                    || (value != null && old[i].getId().equals(value.getId()))) {
                found = true;
                if (deletedId != null) {
                    continue;
                }
                next[at++] = value;
            } else {
                if (at >= next.length) {
                    throw new IllegalArgumentException("Purchase no longer exists. Refresh the page.");
                }
                next[at++] = old[i];
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Purchase no longer exists. Refresh the page.");
        }
        if (adding) {
            next[at] = value;
        }
        return next;
    }

    /**
     * ตรวจสิทธิ์ กฎเอกสาร ผู้จัดหา ประวัติจ่ายเงิน และสินค้า
     * แล้วคำนวณสต็อกที่เสนอใหม่
     * ผ่านแล้วจึงเพิ่มหรือแก้ purchases.csv ไม่ได้บันทึกจ่ายเงินจริงอัตโนมัติ
     */
    public void savePurchase(Purchase value) throws IOException {
        auth.require("Procurement");
        Rules.transaction(value.getDate(), value.getSupplier(), value.getQuantity(), value.getUnitCostSatang());
        requirePartner(value.getSupplier(), false);
        requireUnsettled("PURCHASE:" + value.getId(), false);
        StockItem item = product(value.getProductId());
        Purchase checked = new Purchase(value.getId(), value.getDate(), value.getSupplier(), item.getName(),
                value.getQuantity(), value.getUnitCostSatang(), item.getId());
        balances(sales.findAll(), changedPurchases(checked, null));
        if (checked.getId().length() == 0) {
            purchases.add(checked);
            value.setId(checked.getId());
        } else {
            purchases.update(checked);
        }
    }

    /**
     * ตรวจสิทธิ์และประวัติ Payment แล้วคำนวณสต็อกหลังถอนรายการซื้อ
     * ถ้ายอดไม่ผ่านจะหยุดก่อนลบ purchases.csv
     */
    public void deletePurchase(String id) throws IOException {
        auth.require("Procurement");
        requireUnsettled("PURCHASE:" + id, true);
        balances(sales.findAll(), changedPurchases(null, id));
        purchases.delete(id);
    }

    /**
     * ตรวจว่ารหัสสินค้านี้ถูกอ้างโดยเอกสารขายหรือซื้อหรือไม่
     */
    private boolean isReferenced(String id) throws IOException {
        Sale[] s = sales.findAll();
        Purchase[] p = purchases.findAll();
        for (int i = 0; i < s.length; i++) {
            if (s[i].getProductId().equals(id)) {
                return true;
            }
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i].getProductId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ตรวจสิทธิ์ Warehouse แล้วคืนว่าสินค้ามีเอกสารซื้อขายอ้างอิงหรือไม่
     */
    public boolean isStockLinked(String id) throws IOException {
        auth.require("Warehouse");
        return isReferenced(id);
    }

    /**
     * ตรวจสิทธิ์และค่าของสินค้า; ถ้ามีเอกสารอ้างแล้วห้ามเปลี่ยนยอดคงเหลือหรือหน่วย
     * กรณีสินค้าที่เชื่อมแล้วจะเก็บยอดตั้งต้นเดิมลง Repository
     * ไม่ใช้ยอดคงเหลือเขียนทับ
     */
    public void saveStock(StockItem value) throws IOException {
        auth.require("Warehouse");
        Rules.text(value.getCode(), "Product code", 30, false);
        Rules.text(value.getName(), "Product name", 100, false);
        Rules.text(value.getUnit(), "Unit", 20, false);
        if (value.getId().length() == 0 || !isReferenced(value.getId())) {
            Rules.quantity("" + value.getQuantity(), 0, Rules.MAX_QUANTITY);
        }
        if (value.getId().length() == 0) {
            stock.add(value);
            return;
        }
        StockItem opening = product(value.getId());
        if (isReferenced(value.getId())) {
            StockItem[] current = balances(sales.findAll(), purchases.findAll());
            if (current[productIndex(current, value.getId())].getQuantity() != value.getQuantity()
                    || !opening.getUnit().equals(value.getUnit())) {
                throw new IllegalArgumentException(
                        "Linked product: quantity and unit are locked. Change the related sale/purchase instead.");
            }
            value = new StockItem(value.getId(), value.getCode(), value.getName(), value.getUnit(),
                    opening.getQuantity());
        }
        stock.update(value);
    }

    /**
     * ตรวจสิทธิ์ Warehouse และห้ามลบสินค้าที่มีเอกสารอ้างอิง ก่อนสั่งลบทะเบียน
     */
    public void deleteStock(String id) throws IOException {
        auth.require("Warehouse");
        if (isReferenced(id)) {
            throw new IllegalArgumentException("Product is used by a sale or purchase and cannot be deleted.");
        }
        stock.delete(id);
    }

    /**
     * รวมรายการเงินกรอกเองกับยอดเอกสารที่สร้างจาก Sales และ Purchase ในหน่วยความจำ
     * อ่าน Payment มารวมเฉพาะรายการที่ไม่ void เพื่อหายอดรับจ่ายจริง
     * ไม่เขียนรายการซื้อขายซ้ำลง finance.csv
     */
    private FinanceEntry[] allFinance() throws IOException {
        FinanceEntry[] manual = finance.findAll();
        Sale[] s = sales.findAll();
        Purchase[] p = purchases.findAll();
        int size = manual.length + s.length + p.length;
        FinanceEntry[] result = new FinanceEntry[size];
        int at = 0;
        for (int i = 0; i < manual.length; i++) {
            result[at++] = manual[i];
        }
        for (int i = 0; i < s.length; i++) {
            result[at++] = new FinanceEntry("SALE:" + s[i].getId(), s[i].getDate(), "INCOME",
                    s[i].getCustomer() + " - " + s[i].getProduct(), s[i].total(), "SALE");
        }
        for (int i = 0; i < p.length; i++) {
            result[at++] = new FinanceEntry("PURCHASE:" + p[i].getId(), p[i].getDate(), "EXPENSE",
                    p[i].getSupplier() + " - " + p[i].getProduct(), p[i].total(), "PURCHASE");
        }
        Payment[] ledger = payments.findAll();
        for (int i = 0; i < ledger.length; i++) {
            if (ledger[i].voided) {
                continue;
            }
            boolean found = false;
            for (int j = 0; j < result.length; j++) {
                if (result[j].getId().equals(ledger[i].reference)) {
                    result[j].addPayment(ledger[i].amount);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IOException("Payment has a missing source: " + ledger[i].id);
            }
        }
        return result;
    }

    /**
     * ตรวจสิทธิ์ Finance แล้วคืนยอดเอกสารพร้อมยอดรับจ่ายและยอดค้างที่คำนวณใหม่
     */
    public FinanceEntry[] finance() throws IOException {
        auth.require("Finance");
        return allFinance();
    }

    /**
     * ตรวจสิทธิ์ วันที่ คำอธิบาย ยอดเงิน
     * และประวัติการชำระก่อนเพิ่มหรือแก้รายการกรอกเอง
     * รายการที่มาจากซื้อขายต้องแก้เอกสารต้นทาง ไม่บันทึกผ่าน method นี้
     */
    public void saveFinance(FinanceEntry value) throws IOException {
        auth.require("Finance");
        Rules.date(value.getDate());
        Rules.text(value.getDescription(), "Description", 200, false);
        Rules.money(Values.moneyText(value.getAmountSatang()), 1, Rules.MAX_MONEY);
        requireUnsettled(value.getId(), false);
        if (value.isLinked() || value.getId().indexOf(':') >= 0) {
            throw new IllegalArgumentException("Edit linked money in Sales or Procurement.");
        }
        if (value.getId().length() == 0) {
            finance.add(value);
        } else {
            finance.update(value);
        }
    }

    /**
     * ตรวจสิทธิ์และห้ามลบรายการที่เชื่อมจากซื้อขายหรือมีประวัติ Payment
     * รายการกรอกเองที่ผ่านเงื่อนไขจึงส่งให้ Repository ลบ
     */
    public void deleteFinance(String id) throws IOException {
        auth.require("Finance");
        if (id.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Delete linked money through Sales or Procurement.");
        }
        requireUnsettled(id, true);
        finance.delete(id);
    }

    /**
     * กรณีแก้ไข: ห้ามมี Payment ที่ยังไม่ void อ้างเอกสารนี้
     * กรณีลบ: ห้ามมีประวัติ Payment ใด ๆ แม้ void แล้ว เพื่อไม่ให้ ID
     * ถูกใช้ซ้ำกับประวัติเก่า
     */
    private void requireUnsettled(String reference, boolean deleting) throws IOException {
        Payment[] all = payments.findAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i].reference.equals(reference) && (deleting || !all[i].voided)) {
                throw new IllegalArgumentException(deleting ? "Payment history exists. This document cannot be deleted."
                        : "Void the payment(s) in Finance with a reason before editing this document.");
            }
        }
    }

    /**
     * ตรวจสิทธิ์ Finance แล้วคืนประวัติ Payment ของ reference ที่ระบุ รวมรายการ
     * void
     */
    public Payment[] paymentHistory(String reference) throws IOException {
        auth.require("Finance");
        Payment[] all = payments.findAll();
        int count = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i].reference.equals(reference)) {
                count++;
            }
        }
        Payment[] result = new Payment[count];
        int at = 0;
        for (int i = 0; i < all.length; i++) {
            if (all[i].reference.equals(reference)) {
                result[at++] = all[i];
            }
        }
        return result;
    }

    /**
     * ตรวจสิทธิ์ Finance แล้วอ่านและคำนวณรายการใหม่ก่อนค้นหาด้วย ID
     * ถ้าไม่พบให้ผู้ใช้โหลดหน้าใหม่
     */
    public FinanceEntry financeEntry(String id) throws IOException {
        auth.require("Finance");
        FinanceEntry[] all = allFinance();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getId().equals(id)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("Document no longer exists. Refresh Finance.");
    }

    /**
     * อ่านเอกสารล่าสุดเทียบกับ expected
     * เพื่อไม่บันทึกจากข้อมูลหน้าจอที่เปลี่ยนไปแล้ว
     * ตรวจวันที่ ยอดไม่เกินค้าง และหมายเหตุ แล้วบันทึก Payment พร้อมผู้ทำรายการ
     * amount ใช้สตางค์; ไม่เปลี่ยนจำนวนสินค้า
     */
    public void recordPayment(FinanceEntry expected, String date, long amount, String note) throws IOException {
        FinanceEntry current = financeEntry(expected.getId());
        if (current.getAmountSatang() != expected.getAmountSatang() || current.getSettled() != expected.getSettled()
                || !current.getDate().equals(expected.getDate()) || !current.getType().equals(expected.getType())
                || !current.getDescription().equals(expected.getDescription())) {
            throw new IllegalArgumentException("Document changed. Reopen the payment form.");
        }
        Rules.paymentDate(date, current.getDate());
        Rules.money(Values.moneyText(amount), 1, current.getOutstanding());
        Rules.text(note, "Note", 200, true);
        UserAccount user = auth.currentUser();
        payments.add(new Payment(new String[] { "", current.getId(), date, Values.moneyText(amount),
                user.getId() + " / " + user.getUsername(), note.trim(), "false", "", "", "" }));
    }

    /**
     * ตรวจสิทธิ์และเหตุผล แล้วทำเครื่องหมาย void พร้อมวันที่และผู้ยกเลิก
     * เก็บรายการเดิมใน payments.csv ไว้เป็นประวัติ ไม่ลบแถวทิ้ง
     */
    public void voidPayment(String id, String reason) throws IOException {
        auth.require("Finance");
        Rules.text(reason, "Reason", 200, false);
        Payment[] all = payments.findAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i].id.equals(id)) {
                if (all[i].voided) {
                    throw new IllegalArgumentException("This payment is already voided.");
                }
                UserAccount user = auth.currentUser();
                all[i].voided = true;
                all[i].voidDate = Rules.today();
                all[i].voidedBy = user.getId() + " / " + user.getUsername();
                all[i].voidReason = reason.trim();
                payments.update(all[i]);
                return;
            }
        }
        throw new IllegalArgumentException("Payment not found.");
    }

    /**
     * ตรวจสิทธิ์ Overview แล้วรวมข้อมูลซื้อขาย สต็อก การเงิน และบัญชีเป็น Overview
     * ยอดเงินจริงคิดจาก Payment ที่ยังไม่ void แยกจากยอดเอกสาร
     */
    public Overview overview() throws IOException {
        auth.require("Overview");
        return new Overview(sales.findAll(), purchases.findAll(),
                balances(sales.findAll(), purchases.findAll()), allFinance(), auth.users(), auth.roles());
    }
}

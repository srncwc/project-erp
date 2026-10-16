package erp.model;

/**
 * ข้อมูลยอดเอกสารทางการเงิน ทั้งรายการกรอกเองและรายการจากการซื้อขาย
 * แยกยอดเอกสารออกจากยอดรับจ่ายจริงและยอดค้าง ซึ่งรวมจาก Payment
 * ที่ยังไม่ถูกยกเลิก
 */
public class FinanceEntry {
    private String id;
    private String date;
    private String type;
    private String description;
    private long amountSatang;
    private String source;
    private long settled;

    /**
     * รวม amount หน่วยสตางค์เข้า settled และตรวจว่าไม่เกินยอดเอกสาร
     * ผู้เรียกต้องส่งยอดที่ถูกต้อง; method นี้ไม่ได้ตรวจค่าติดลบเองและไม่เขียน
     * payments.csv
     */
    public void addPayment(long amount) {
        if (amount > amountSatang - settled) {
            throw new IllegalArgumentException("Payments exceed document amount: " + id);
        }
        settled += amount;
    }

    /**
     * คืนยอดรับหรือจ่ายที่รวมไว้ หน่วยสตางค์
     */
    public long getSettled() {
        return settled;
    }

    /**
     * คืนยอดค้างเป็นสตางค์ โดยคำนวณยอดเอกสารลบยอดที่ชำระแล้ว
     */
    public long getOutstanding() {
        return amountSatang - settled;
    }

    /**
     * คืนข้อความสถานะตามยอดเอกสารและยอดที่ชำระ เช่น ยังไม่จ่าย บางส่วน หรือครบแล้ว
     */
    public String getPaymentText() {
        if (amountSatang == 0) {
            return "No payment due";
        }
        if (settled == 0) {
            return "INCOME".equals(type) ? "Not received" : "Unpaid";
        }
        if (settled < amountSatang) {
            return "Partial";
        }
        return "INCOME".equals(type) ? "Received" : "Paid";
    }

    /**
     * สร้างยอดเอกสารในหน่วยความจำ โดยแบบไม่ระบุ source ใช้ MANUAL
     * MANUAL ต้องมากกว่าศูนย์ ส่วน source อื่นยอมให้ยอดศูนย์;
     * ยังไม่บันทึกหรือรับจ่ายเงิน
     */
    public FinanceEntry(String id, String date, String type, String description, long amountSatang) {
        this(id, date, type, description, amountSatang, "MANUAL");
    }

    /**
     * สร้างยอดเอกสารในหน่วยความจำ โดยแบบไม่ระบุ source ใช้ MANUAL
     * MANUAL ต้องมากกว่าศูนย์ ส่วน source อื่นยอมให้ยอดศูนย์;
     * ยังไม่บันทึกหรือรับจ่ายเงิน
     */
    public FinanceEntry(String id, String date, String type, String description, long amountSatang, String source) {
        if (amountSatang < 0 || ("MANUAL".equals(source) && amountSatang == 0)) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            throw new IllegalArgumentException("Choose INCOME or EXPENSE.");
        }
        this.source = source;
        this.id = id;
        this.date = Values.date(date);
        this.type = Values.required(type, "Type");
        this.description = Values.required(description, "Description");
        this.amountSatang = amountSatang;
    }

    /**
     * คืนแหล่งที่มาของรายการ เช่น MANUAL, SALE หรือ PURCHASE
     * โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getSource() {
        return source;
    }

    /**
     * คืน true เมื่อ source ไม่ใช่ MANUAL เพื่อแยกรายการที่เชื่อมเอกสารซื้อขาย
     */
    public boolean isLinked() {
        return !"MANUAL".equals(source);
    }

    /**
     * คืนรหัสรายการ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getId() {
        return id;
    }

    /**
     * กำหนด ID ใน object ที่อยู่ในหน่วยความจำ การเรียกอย่างเดียวไม่เขียนไฟล์
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * คืนวันที่ของรายการเป็นข้อความ YYYY-MM-DD โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getDate() {
        return date;
    }

    /**
     * คืนประเภทรายการ INCOME หรือ EXPENSE โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getType() {
        return type;
    }

    /**
     * คืนคำอธิบายรายการ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getDescription() {
        return description;
    }

    /**
     * คืนยอดเอกสารเป็นสตางค์ โดยไม่อ่านหรือเขียนไฟล์
     */
    public long getAmountSatang() {
        return amountSatang;
    }
}

package erp.model;

/**
 * ข้อมูลรายการขายหนึ่งรายการ เก็บลูกค้า สินค้า จำนวน และราคาต่อหน่วยเป็นสตางค์
 * คำนวณยอดด้วย total(); การสร้าง object ยังไม่บันทึกไฟล์หรือตัดสต็อก
 */
public class Sale {
    private String id;
    private String date;
    private String customer;
    private String product;
    private String productId;
    private int quantity;
    private long unitPriceSatang;

    /**
     * สร้างข้อมูลเอกสารหนึ่งรายการ ตรวจจำนวนอย่างน้อย 1 ราคาหรือต้นทุนไม่ติดลบ และไม่คูณเกิน long
     * เก็บ productId เพื่อเชื่อมสินค้า; ราคาต่อหน่วยใช้สตางค์ เช่น 5000 คือ 50 บาท
     * ยังไม่บันทึกไฟล์ และยังต้องผ่านกฎเพิ่มเติมใน ErpService
     */
    public Sale(String id, String date, String customer, String product, int quantity, long unitPriceSatang, String productId) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        if (unitPriceSatang < 0) {
            throw new IllegalArgumentException("Unit price must be at least 0.");
        }
        // ตรวจเพดาน long ก่อนคูณ เพื่อไม่ให้ยอดรวมเกินช่วงตัวเลขที่เก็บได้
        if (unitPriceSatang > Long.MAX_VALUE / quantity) {
            throw new IllegalArgumentException("Total amount is too large.");
        }
        this.productId = Values.required(productId, "Warehouse product ID");
        this.id = id;
        this.date = Values.date(date);
        this.customer = Values.required(customer, "Customer");
        this.product = Values.required(product, "Product");
        this.quantity = quantity;
        this.unitPriceSatang = unitPriceSatang;
    }

    /**
     * คืนID สินค้าที่ใช้เชื่อมทะเบียน Warehouse โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getProductId() { return productId; }

    /**
     * คืนรหัสรายการ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getId() {
        return id;
    }

    /**
     * กำหนด ID ของรายการในหน่วยความจำ และตรวจ productId เดิมว่าไม่ว่าง
     * การเรียก method นี้อย่างเดียวไม่เขียน CSV
     */
    public void setId(String id) {
        this.productId = Values.required(productId, "Warehouse product ID");
        this.id = id;
    }

    /**
     * คืนวันที่ของรายการเป็นข้อความ YYYY-MM-DD โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getDate() {
        return date;
    }

    /**
     * คืนชื่อลูกค้า โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getCustomer() {
        return customer;
    }

    /**
     * คืนชื่อสินค้าที่เก็บในเอกสาร โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getProduct() {
        return product;
    }

    /**
     * คืนจำนวนสินค้า โดยไม่อ่านหรือเขียนไฟล์
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * คืนราคาขายต่อหน่วยเป็นสตางค์ โดยไม่อ่านหรือเขียนไฟล์
     */
    public long getUnitPriceSatang() {
        return unitPriceSatang;
    }

    /**
     * คำนวณจำนวนคูณราคาหรือต้นทุนต่อหน่วย แล้วคืนยอดรวมเป็นสตางค์
     * เช่น 2 ชิ้น คูณ 5000 สตางค์ ได้ 10000 สตางค์ (100 บาท)
     */
    public long total() {
        return unitPriceSatang * quantity;
    }
}

package erp.model;

/**
 * ข้อมูลสินค้า ได้แก่ รหัส ชื่อ หน่วย และจำนวน
 * เมื่ออ่าน stock.csv จำนวนคือยอดตั้งต้น; เมื่อ Service คำนวณแล้ว object ที่คืนจะถือยอดคงเหลือ
 */
public class StockItem {
    private String id;
    private String code;
    private String name;
    private String unit;
    private int quantity;

    /**
     * สร้างข้อมูลสินค้าและตรวจจำนวนไม่ติดลบกับข้อความที่ต้องมี
     * จำนวนอาจเป็นยอดตั้งต้นหรือยอดคงเหลือ ตามแหล่งที่สร้าง object นี้
     */
    public StockItem(String id, String code, String name, String unit, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be at least 0.");
        }
        this.id = id;
        this.code = Values.required(code, "Product code");
        this.name = Values.required(name, "Product name");
        this.unit = Values.required(unit, "Unit");
        this.quantity = quantity;
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
     * คืนรหัสสินค้า code โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getCode() {
        return code;
    }

    /**
     * คืนชื่อที่เก็บใน object โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getName() {
        return name;
    }

    /**
     * คืนหน่วยสินค้า โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getUnit() {
        return unit;
    }

    /**
     * คืนจำนวนใน object นี้: Repository อ่านเป็นยอดตั้งต้น ส่วน balances() สร้างเป็นยอดคงเหลือ
     */
    public int getQuantity() {
        return quantity;
    }
}

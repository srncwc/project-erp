package erp.model;

/**
 * ข้อมูลคู่ค้าหนึ่งราย ได้แก่ ID ชื่อ และช่องทางติดต่อ
 * ใช้ได้ทั้งลูกค้าและผู้จัดหา โดย Repository เป็นผู้เลือกไฟล์ที่จะจัดเก็บ
 */
public class Partner {
    private String id;
    private String name;
    private String contact;
    /**
     * สร้างข้อมูลคู่ค้า ตรวจชื่อไม่เกิน 100 ตัวและข้อมูลติดต่อไม่เกิน 120 ตัว
     * ชื่อจำเป็น แต่ข้อมูลติดต่อเว้นว่างได้; ยังไม่บันทึกไฟล์
     */
    public Partner(String id, String name, String contact) {
        this.id = id;
        this.name = Rules.text(name, "Name", 100, false);
        this.contact = Rules.text(contact, "Contact", 120, true);
    }
    /**
     * คืนรหัสรายการ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getId() { return id; }
    /**
     * กำหนด ID ใน object ที่อยู่ในหน่วยความจำ การเรียกอย่างเดียวไม่เขียนไฟล์
     */
    public void setId(String id) { this.id = id; }
    /**
     * คืนชื่อที่เก็บใน object โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getName() { return name; }
    /**
     * คืนช่องทางติดต่อ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getContact() { return contact; }
}

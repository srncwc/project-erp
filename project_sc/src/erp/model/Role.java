package erp.model;

/**
 * ข้อมูลกลุ่มสิทธิ์ โดยเก็บชื่อโมดูลที่อนุญาตคั่นด้วย |
 * ตัวอย่าง Sales|Warehouse หมายถึงอนุญาตทั้งสองโมดูล
 */
public class Role {
    public static final String[] MODULES = {"Sales", "Procurement", "Warehouse", "Finance"};
    private String id;
    private String name;
    private String modules;

    /**
     * สร้าง Role พร้อมตรวจว่าชื่อไม่ว่างและโมดูลทุกค่าที่คั่นด้วย | เป็นชื่อที่รองรับ
     */
    public Role(String id, String name, String modules) {
        this.id = id;
        this.name = Values.required(name, "Role name");
        this.modules = Values.required(modules, "At least one module");
        String[] chosen = modules.split("\\|", -1);
        for (int i = 0; i < chosen.length; i++) {
            if (!contains("Sales|Procurement|Warehouse|Finance", chosen[i])) {
                throw new IllegalArgumentException("Unknown module: " + chosen[i]);
            }
        }
    }

    /**
     * แยกรายการด้วย | แล้วเทียบแต่ละค่าทั้งคำ
     * เช่น R001 จะไม่ตรงกับ R0010 ต่างจากการค้นหาข้อความบางส่วน
     */
    public static boolean contains(String list, String item) {
        if (item == null || item.length() == 0) { return false; }
        String[] parts = list.split("\\|", -1);
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals(item)) { return true; }
        }
        return false;
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
     * คืนชื่อโมดูลที่คั่นด้วย | โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getModules() { return modules; }
}

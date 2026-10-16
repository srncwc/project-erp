package erp.model;

/**
 * ข้อมูลบัญชีผู้ใช้ ได้แก่ ID ชื่อเข้าใช้ รหัสผ่าน ชื่อแสดง ประเภทบัญชี และ Role IDs
 * เก็บข้อมูลในหน่วยความจำ; การ Login และตรวจสิทธิ์เป็นหน้าที่ของ AuthService
 */
public class UserAccount {
    private String id;
    private String username;
    private String password;
    private String displayName;
    private String accountType;
    private String roleIds;

    /**
      * สร้างข้อมูลบัญชีในหน่วยความจำและตรวจประเภท ADMIN/USER กับค่าจำเป็น
      * constructor แบบสี่ค่าใช้ USER และ Role ว่าง; กฎ Role และรหัสผ่านใหม่ตรวจเพิ่มใน AuthService
      */
    public UserAccount(String id, String username, String password, String displayName) {
        this(id, username, password, displayName, "USER", "");
    }

    /**
     * สร้างข้อมูลบัญชีในหน่วยความจำและตรวจประเภท ADMIN/USER กับค่าจำเป็น
     * constructor แบบสี่ค่าใช้ USER และ Role ว่าง; กฎ Role และรหัสผ่านใหม่ตรวจเพิ่มใน AuthService
     */
    public UserAccount(String id, String username, String password, String displayName, String accountType,
            String roleIds) {
        if (!"ADMIN".equals(accountType) && !"USER".equals(accountType)) {
            throw new IllegalArgumentException("Choose ADMIN or USER.");
        }
        this.accountType = accountType;
        this.roleIds = roleIds;
        Values.required(password, "Password");
        this.id = id;
        this.username = Values.required(username, "Username");
        this.password = password;
        this.displayName = Values.required(displayName, "Display name");
    }

    /**
     * คืนประเภทบัญชี ADMIN หรือ USER โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getAccountType() {
        return accountType;
    }

    /**
     * คืนRole IDs ที่คั่นด้วย | โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getRoleIds() {
        return roleIds;
    }

    /**
     * คืน true เมื่อ accountType เป็น ADMIN เท่านั้น
     */
    public boolean isAdmin() {
        return "ADMIN".equals(accountType);
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
     * คืนชื่อที่ใช้เข้าสู่ระบบ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getUsername() {
        return username;
    }

    /**
     * คืนรหัสผ่านที่เก็บใน object นี้ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getPassword() {
        return password;
    }

    /**
     * คืนชื่อสำหรับแสดงบนหน้าจอ โดยไม่อ่านหรือเขียนไฟล์
     */
    public String getDisplayName() {
        return displayName;
    }
}

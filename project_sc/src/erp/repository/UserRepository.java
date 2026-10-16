package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก UserAccount ใน users.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class UserRepository {
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ users.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public UserRepository(File folder) {
        csv = new CsvFile(folder, "users.csv", "id,username,password,displayName,accountType,roleIds");
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น UserAccount[] ตามลำดับในไฟล์
     */
    public UserAccount[] findAll() throws IOException {
        String[][] rows = csv.read();
        UserAccount[] records = new UserAccount[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            records[i] = new UserAccount(
                    row[0], row[1], row[2], row[3], row[4], row[5]);
        }
        return records;
    }

    /**
     * เพิ่ม UserAccount ผ่าน CsvFile แล้วนำ ID ที่บันทึกสำเร็จกลับมาใส่ object
     * ตรวจชื่อเข้าใช้หรือรหัสสินค้าซ้ำก่อนเพิ่มตามชนิด Repository
     */
    public void add(UserAccount value) throws IOException {
        checkUnique(value);
        String id = csv.add(toRow(value), "U");
        value.setId(id);
    }

    /**
     * เขียนข้อมูลใหม่ทับแถวที่มี ID เดิมใน users.csv ไม่เพิ่มรายการใหม่
     */
    public void update(UserAccount value) throws IOException {
        checkUnique(value);
        csv.update(toRow(value));
    }

    /**
     * ห้ามลบเมื่อเหลือบัญชีไม่เกินหนึ่งรายการ แล้วจึงส่ง ID ให้ CsvFile ลบ
     * ตรวจจำนวนบัญชีเท่านั้น; การรักษา Admin และห้ามลบตนเองตรวจใน AuthService
     */
    public void delete(String id) throws IOException {
        if (findAll().length <= 1) {
            throw new IllegalArgumentException("Keep at least one account to open the system.");
        }
        csv.delete(id);
    }

    /**
     * ตรวจ username ซ้ำแบบไม่แยกตัวพิมพ์ โดยยกเว้นบัญชีที่มี ID เดียวกัน
     */
    private void checkUnique(UserAccount value) throws IOException {
        UserAccount[] records = findAll();
        for (int i = 0; i < records.length; i++) {
            if (!records[i].getId().equals(value.getId())
                    && records[i].getUsername().equalsIgnoreCase(value.getUsername())) {
                throw new IllegalArgumentException("Username already exists.");
            }
        }
    }

    /**
     * แปลง UserAccount เป็น String[] เรียงตาม header ของไฟล์
     * เป็นการเตรียมข้อมูล ยังไม่ได้เขียนไฟล์
     */
    private String[] toRow(UserAccount value) {
        return new String[] {
                value.getId(),
                value.getUsername(),
                value.getPassword(),
                value.getDisplayName(),
                value.getAccountType(),
                value.getRoleIds()
        };
    }
}

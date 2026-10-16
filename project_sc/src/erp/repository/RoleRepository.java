package erp.repository;

import erp.model.Role;
import java.io.*;

/**
 * อ่านและบันทึก Role ใน roles.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class RoleRepository {
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ roles.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public RoleRepository(File folder) {
        csv = new CsvFile(folder, "roles.csv", "id,name,modules");
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น Role[] ตามลำดับในไฟล์
     */
    public Role[] findAll() throws IOException {
        String[][] rows = csv.read();
        Role[] roles = new Role[rows.length];
        for (int i = 0; i < rows.length; i++) {
            roles[i] = new Role(rows[i][0], rows[i][1], rows[i][2]);
        }
        return roles;
    }

    /**
     * อ่านรายการเดิมเพื่อตรวจชื่อซ้ำแบบไม่แยกตัวพิมพ์ โดยยกเว้น ID
     * ของรายการที่กำลังแก้
     * ID ว่างให้เพิ่มพร้อมรับ ID ใหม่; มี ID ให้แก้แถวเดิม
     */
    public void save(Role role) throws IOException {
        Role[] roles = findAll();
        for (int i = 0; i < roles.length; i++) {
            if (!roles[i].getId().equals(role.getId()) && roles[i].getName().equalsIgnoreCase(role.getName())) {
                throw new IllegalArgumentException("Role name already exists.");
            }
        }
        String[] row = { role.getId(), role.getName(), role.getModules() };
        if (role.getId().length() == 0) {
            role.setId(csv.add(row, "R"));
        } else {
            csv.update(row);
        }
    }

    /**
     * ส่ง ID ให้ CsvFile ลบแถวจาก roles.csv
     */
    public void delete(String id) throws IOException {
        csv.delete(id);
    }
}

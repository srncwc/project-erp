package erp.repository;
import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก Partner ใน customers.csv หรือ suppliers.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class PartnerRepository {
    private CsvFile csv;
    private String prefix;
    /**
     * เตรียม CsvFile สำหรับ customers.csv หรือ suppliers.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง; customer เป็น true เลือกลูกค้า false เลือกผู้จัดหา
     */
    public PartnerRepository(File folder, boolean customer) {
        csv = new CsvFile(folder, customer ? "customers.csv" : "suppliers.csv", "id,name,contact");
        prefix = customer ? "C" : "V";
    }
    /**
     * อ่านแถว CSV แล้วแปลงเป็น Partner[] ตามลำดับในไฟล์
     */
    public Partner[] findAll() throws IOException {
        String[][] rows = csv.read();
        Partner[] result = new Partner[rows.length];
        for (int i = 0; i < rows.length; i++) { result[i] = new Partner(rows[i][0], rows[i][1], rows[i][2]); }
        return result;
    }
    /**
     * อ่านรายการเดิมเพื่อตรวจชื่อซ้ำแบบไม่แยกตัวพิมพ์ โดยยกเว้น ID ของรายการที่กำลังแก้
     * ID ว่างให้เพิ่มพร้อมรับ ID ใหม่; มี ID ให้แก้แถวเดิม
     */
    public void save(Partner value) throws IOException {
        Partner[] all = findAll();
        for (int i = 0; i < all.length; i++) {
            if (!all[i].getId().equals(value.getId()) && all[i].getName().equalsIgnoreCase(value.getName())) {
                throw new IllegalArgumentException("This name already exists.");
            }
        }
        String[] row = {value.getId(), value.getName(), value.getContact()};
        if (value.getId().length() == 0) { value.setId(csv.add(row, prefix)); }
        else { csv.update(row); }
    }
    /**
     * ส่ง ID ให้ CsvFile ลบแถวจาก customers.csv หรือ suppliers.csv
     */
    public void delete(String id) throws IOException { csv.delete(id); }
}

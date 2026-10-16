package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก Purchase ใน purchases.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class PurchaseRepository {
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ purchases.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public PurchaseRepository(File folder) {
        csv = new CsvFile(folder, "purchases.csv", "id,date,supplier,product,quantity,unitCost,productId");
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น Purchase[] ตามลำดับในไฟล์
     */
    public Purchase[] findAll() throws IOException {
        String[][] rows = csv.read();
        Purchase[] records = new Purchase[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            records[i] = new Purchase(
                    row[0], row[1], row[2], row[3], Integer.parseInt(row[4]), Values.money(row[5]), row[6]);
        }
        return records;
    }

    /**
     * เพิ่ม Purchase ผ่าน CsvFile แล้วนำ ID ที่บันทึกสำเร็จกลับมาใส่ object
     */
    public void add(Purchase value) throws IOException {
        String id = csv.add(toRow(value), "P");
        value.setId(id);
    }

    /**
     * เขียนข้อมูลใหม่ทับแถวที่มี ID เดิมใน purchases.csv ไม่เพิ่มรายการใหม่
     */
    public void update(Purchase value) throws IOException {
        csv.update(toRow(value));
    }

    /**
     * ส่ง ID ให้ CsvFile ลบแถวจาก purchases.csv
     */
    public void delete(String id) throws IOException {
        csv.delete(id);
    }

    /**
     * แปลง Purchase เป็น String[] เรียงตาม header ของไฟล์
     * เป็นการเตรียมข้อมูล ยังไม่ได้เขียนไฟล์
     */
    private String[] toRow(Purchase value) {
        return new String[] {
                value.getId(),
                value.getDate(),
                value.getSupplier(),
                value.getProduct(),
                "" + value.getQuantity(),
                Values.moneyText(value.getUnitCostSatang()),
                value.getProductId()
        };
    }
}

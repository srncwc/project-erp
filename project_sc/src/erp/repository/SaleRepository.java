package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก Sale ใน sales.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class SaleRepository {
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ sales.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public SaleRepository(File folder) {
        csv = new CsvFile(folder, "sales.csv", "id,date,customer,product,quantity,unitPrice,productId");
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น Sale[] ตามลำดับในไฟล์
     */
    public Sale[] findAll() throws IOException {
        String[][] rows = csv.read();
        Sale[] records = new Sale[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            records[i] = new Sale(
                    row[0], row[1], row[2], row[3], Integer.parseInt(row[4]), Values.money(row[5]), row[6]);
        }
        return records;
    }

    /**
     * เพิ่ม Sale ผ่าน CsvFile แล้วนำ ID ที่บันทึกสำเร็จกลับมาใส่ object
     */
    public void add(Sale value) throws IOException {
        String id = csv.add(toRow(value), "S");
        value.setId(id);
    }

    /**
     * เขียนข้อมูลใหม่ทับแถวที่มี ID เดิมใน sales.csv ไม่เพิ่มรายการใหม่
     */
    public void update(Sale value) throws IOException {
        csv.update(toRow(value));
    }

    /**
     * ส่ง ID ให้ CsvFile ลบแถวจาก sales.csv
     */
    public void delete(String id) throws IOException {
        csv.delete(id);
    }

    /**
     * แปลง Sale เป็น String[] เรียงตาม header ของไฟล์
     * เป็นการเตรียมข้อมูล ยังไม่ได้เขียนไฟล์
     */
    private String[] toRow(Sale value) {
        return new String[] {
                value.getId(),
                value.getDate(),
                value.getCustomer(),
                value.getProduct(),
                "" + value.getQuantity(),
                Values.moneyText(value.getUnitPriceSatang()),
                value.getProductId()
        };
    }
}

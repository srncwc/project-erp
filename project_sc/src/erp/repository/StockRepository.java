package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก StockItem ใน stock.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class StockRepository {
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ stock.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public StockRepository(File folder) {
        csv = new CsvFile(folder, "stock.csv", "id,code,name,unit,openingQuantity");
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น StockItem[] ตามลำดับในไฟล์
     * quantity ที่อ่านคือ openingQuantity (ยอดตั้งต้น) ไม่ใช่ยอดคงเหลือ
     */
    public StockItem[] findAll() throws IOException {
        String[][] rows = csv.read();
        StockItem[] records = new StockItem[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            records[i] = new StockItem(
                    row[0], row[1], row[2], row[3], Integer.parseInt(row[4]));
        }
        return records;
    }

    /**
     * เพิ่ม StockItem ผ่าน CsvFile แล้วนำ ID ที่บันทึกสำเร็จกลับมาใส่ object
     * ตรวจชื่อเข้าใช้หรือรหัสสินค้าซ้ำก่อนเพิ่มตามชนิด Repository
     */
    public void add(StockItem value) throws IOException {
        checkUnique(value);
        String id = csv.add(toRow(value), "W");
        value.setId(id);
    }

    /**
     * เขียนข้อมูลใหม่ทับแถวที่มี ID เดิมใน stock.csv ไม่เพิ่มรายการใหม่
     */
    public void update(StockItem value) throws IOException {
        checkUnique(value);
        csv.update(toRow(value));
    }

    /**
     * ส่ง ID ให้ CsvFile ลบแถวจาก stock.csv
     */
    public void delete(String id) throws IOException {
        csv.delete(id);
    }

    /**
     * ตรวจรหัสสินค้า code ซ้ำแบบไม่แยกตัวพิมพ์ โดยยกเว้นสินค้าที่มี ID เดียวกัน
     */
    private void checkUnique(StockItem value) throws IOException {
        StockItem[] records = findAll();
        for (int i = 0; i < records.length; i++) {
            if (!records[i].getId().equals(value.getId())
                    && records[i].getCode().equalsIgnoreCase(value.getCode())) {
                throw new IllegalArgumentException("Product code already exists.");
            }
        }
    }

    /**
     * แปลง StockItem เป็น String[] เรียงตาม header ของไฟล์
     * เป็นการเตรียมข้อมูล ยังไม่ได้เขียนไฟล์
     */
    private String[] toRow(StockItem value) {
        return new String[] {
                value.getId(),
                value.getCode(),
                value.getName(),
                value.getUnit(),
                "" + value.getQuantity()
        };
    }
}

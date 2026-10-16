package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก FinanceEntry ใน finance.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 * ไฟล์นี้เก็บเฉพาะรายการกรอกเอง ไม่เก็บยอดซื้อขายที่คำนวณมาหรือประวัติ Payment
 */
public class FinanceRepository {
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ finance.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public FinanceRepository(File folder) {
        csv = new CsvFile(folder, "finance.csv", "id,date,type,description,amount");
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น FinanceEntry[] ตามลำดับในไฟล์
     */
    public FinanceEntry[] findAll() throws IOException {
        String[][] rows = csv.read();
        FinanceEntry[] records = new FinanceEntry[rows.length];
        for (int i = 0; i < rows.length; i++) {
            String[] row = rows[i];
            records[i] = new FinanceEntry(
                    row[0], row[1], row[2], row[3], Values.money(row[4]));
        }
        return records;
    }

    /**
     * เพิ่ม FinanceEntry ผ่าน CsvFile แล้วนำ ID ที่บันทึกสำเร็จกลับมาใส่ object
     */
    public void add(FinanceEntry value) throws IOException {
        String id = csv.add(toRow(value), "F");
        value.setId(id);
    }

    /**
     * เขียนข้อมูลใหม่ทับแถวที่มี ID เดิมใน finance.csv ไม่เพิ่มรายการใหม่
     */
    public void update(FinanceEntry value) throws IOException {
        csv.update(toRow(value));
    }

    /**
     * ส่ง ID ให้ CsvFile ลบแถวจาก finance.csv
     */
    public void delete(String id) throws IOException {
        csv.delete(id);
    }

    /**
     * แปลง FinanceEntry เป็น String[] เรียงตาม header ของไฟล์
     * เป็นการเตรียมข้อมูล ยังไม่ได้เขียนไฟล์
     */
    private String[] toRow(FinanceEntry value) {
        return new String[] {
                value.getId(),
                value.getDate(),
                value.getType(),
                value.getDescription(),
                Values.moneyText(value.getAmountSatang())
        };
    }
}

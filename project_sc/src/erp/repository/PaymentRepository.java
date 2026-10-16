package erp.repository;

import erp.model.*;
import java.io.*;

/**
 * อ่านและบันทึก Payment ใน payments.csv โดยแปลงระหว่าง object กับแถว CSV
 * ส่งรายละเอียดอ่านเขียนไฟล์ให้ CsvFile; การตรวจสิทธิ์เป็นหน้าที่ของ Service
 */
public class PaymentRepository {
    public static final String HEADER = "id,reference,date,amount,recordedBy,note,voided,voidDate,voidedBy,voidReason";
    private CsvFile csv;

    /**
     * เตรียม CsvFile สำหรับ payments.csv และ header ที่ตรงกับข้อมูล
     * ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public PaymentRepository(File folder) {
        csv = new CsvFile(folder, "payments.csv", HEADER);
    }

    /**
     * อ่านแถว CSV แล้วแปลงเป็น Payment[] ตามลำดับในไฟล์
     */
    public Payment[] findAll() throws IOException {
        String[][] rows = csv.read();
        Payment[] result = new Payment[rows.length];
        for (int i = 0; i < rows.length; i++) {
            result[i] = new Payment(rows[i]);
        }
        return result;
    }

    /**
     * เพิ่ม Payment ผ่าน CsvFile แล้วนำ ID ที่บันทึกสำเร็จกลับมาใส่ object
     */
    public void add(Payment payment) throws IOException {
        payment.id = csv.add(payment.toRow(), "T");
    }

    /**
     * เขียนข้อมูลใหม่ทับแถวที่มี ID เดิมใน payments.csv ไม่เพิ่มรายการใหม่
     */
    public void update(Payment payment) throws IOException {
        csv.update(payment.toRow());
    }
}

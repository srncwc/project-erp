package erp;

import erp.ui.AppFrame;
import erp.repository.*;
import java.io.*;
import javax.swing.*;

/**
 * จุดเริ่มต้นของโปรแกรม เตรียมโฟลเดอร์ข้อมูลและตรวจความพร้อมก่อนเปิด AppFrame
 * ส่งงานสร้างหน้าจอไปทำบน Event Dispatch Thread (EDT) ของ Swing
 */
public class Main implements Runnable {
    /**
     * ส่ง Main.run() ไปทำบน EDT ซึ่งเป็นเธรดจัดการหน้าจอและเหตุการณ์ของ Swing
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Main());
    }

    /**
     * กำหนดโฟลเดอร์ data ตรวจ encoding จองล็อก กู้ไฟล์ที่ค้าง และตรวจข้อมูล
     * ถ้าขั้นตอนตรวจไม่ผ่านให้แจ้งข้อผิดพลาดและไม่เปิดหน้าต่างหลัก
     */
    public void run() {
        // กำหนด data โดยอ้างอิง working directory ที่ใช้รัน ไม่ใช่ตำแหน่ง Main.java
        File data = new File("data");
        // รายชื่อไฟล์ข้อมูลที่ระบบ ERP จำเป็นต้องใช้
        String[] names = DataFiles.NAMES;

        // ตรวจสอบ Encoding เพื่อให้อ่านและเขียนข้อมูลภาษาไทยได้ถูกต้อง
        if (!"UTF-8".equalsIgnoreCase(System.getProperty("file.encoding"))) {
            JOptionPane.showMessageDialog(null, "Run with UTF-8 encoding (JDK 18 or newer by default).");
            return;
        }
        // ระบบล็อกเพื่อป้องกันไม่ให้ ERP สองโปรแกรมเข้าไปแก้ไขไฟล์ CSV
        // ชุดเดียวกันพร้อมกัน
        InstanceLock lock = new InstanceLock(data);
        try {
            // จองล็อกของโปรแกรมก่อนแตะข้อมูล ไม่ใช่การขอสิทธิ์ไฟล์จากระบบปฏิบัติการ
            lock.acquire();
            // ตรวจสอบและกู้คืนไฟล์ CSV หากการบันทึกครั้งก่อนถูกขัดจังหวะ
            for (int i = 0; i < names.length; i++) {
                SafeFiles.recover(new File(data, names[i]));
            }
        } catch (Exception error) {
            // คืน Lock เมื่อเกิดข้อผิดพลาดระหว่างเตรียมไฟล์
            lock.run();
            JOptionPane.showMessageDialog(null, error.getMessage(), "Cannot open data", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // ตรวจว่ามีไฟล์ข้อมูลที่จำเป็นครบก่อนเปิดโปรแกรม
        for (int i = 0; i < names.length; i++) {
            if (!new File(data, names[i]).isFile()) {
                lock.run();
                JOptionPane.showMessageDialog(null,
                        "Cannot find data/" + names[i] + "\nSet the working directory to ERP, beside src and data.");
                return;
            }
        }
        // อ่านตรวจข้อมูลและความสัมพันธ์ใน CSV ถ้าไม่ผ่านให้ปล่อยล็อกและแสดงข้อผิดพลาด
        try {
            DataFiles.verify(data);
        } catch (Exception error) {

            lock.run();
            JOptionPane.showMessageDialog(null, error.getMessage(), "Check data", JOptionPane.ERROR_MESSAGE);
            return;
        }
        AppFrame frame = new AppFrame(data);
        frame.setVisible(true);
    }
}
package erp.repository;
import java.io.*;

/**
 * ใช้ session-lock.csv ป้องกัน ERP ที่ใช้กลไกนี้เปิดข้อมูลชุดเดียวกันพร้อมกัน
 * ไม่ใช่การล็อกที่ห้ามโปรแกรมภายนอกแก้ CSV และไฟล์ล็อกอาจค้างหลังโปรแกรมหยุดผิดปกติ
 */
public class InstanceLock implements Runnable {
    private File file;
    private boolean acquired;
    /**
     * กำหนดตำแหน่ง session-lock.csv ยังไม่สร้างไฟล์ล็อกจนกว่าจะเรียก acquire()
     */
    public InstanceLock(File data) { file=new File(data,"session-lock.csv"); }
    /**
     * สร้างไฟล์ล็อกด้วย createNewFile(); ถ้ามีอยู่แล้วให้หยุดเปิดข้อมูลชุดนี้
     * เมื่อล็อกสำเร็จลงทะเบียนให้เรียก run() เมื่อ JVM ปิดตามปกติ
     */
    public void acquire() throws IOException {
        if(!file.createNewFile()){
            throw new IOException("This data folder is already open or the previous session ended unexpectedly.\n"
                    +"Close all ERP windows first. After a crash only: delete data/session-lock.csv and reopen.");
        }
        acquired=true;
        try(BufferedWriter writer=new BufferedWriter(new FileWriter(file))){writer.write("state\nopen\n");}
        Runtime.getRuntime().addShutdownHook(new Thread(this));
    }
    /**
     * ถ้า instance นี้เคยได้ล็อก ให้พยายามลบไฟล์ล็อกและล้าง acquired
     * ไม่ได้คืนค่า และโค้ดนี้ไม่ได้ตรวจผลสำเร็จของ file.delete()
     */
    public void run() { if(acquired){file.delete();acquired=false;} }
}

package erp.repository;

import java.io.*;

/**
 * ช่วยแทนที่ CSV รายไฟล์ โดยใช้ .next.csv เป็นไฟล์ใหม่ และ .previous.csv
 * เก็บไฟล์เดิมชั่วคราว
 * พยายามคืนไฟล์เดิมเมื่อแทนที่ไม่สำเร็จ
 * ไม่ใช่ฟีเจอร์สำรองข้อมูลทั้งชุดให้ผู้ใช้เลือกย้อนหลัง
 */
public class SafeFiles {
    /**
     * คืนตำแหน่งไฟล์ใหม่โดยต่อท้าย .next.csv เช่น sales.csv.next.csv
     * สร้างเพียง File object ยังไม่ได้สร้างหรือเขียนไฟล์จริง
     */
    public static File next(File file) {
        return new File(file.getPath() + ".next.csv");
    }

    /**
     * ตรวจไฟล์ที่ค้างจากการแทนที่: ถ้าไฟล์จริงหายแต่มี .previous.csv
     * ให้เปลี่ยนชื่อคืน
     * ถ้ามีไฟล์จริงอยู่แล้วให้ลบไฟล์เดิมที่ค้าง และลบ .next.csv ที่ยังไม่ commit
     * ไม่ตรวจว่าเนื้อหา CSV ถูกต้องหรือไม่
     */
    public static void recover(File file) throws IOException {
        File previous = new File(file.getPath() + ".previous.csv");
        if (previous.exists()) {
            if (!file.exists()) {
                if (!previous.renameTo(file)) {
                    throw new IOException("Cannot restore " + file.getName());
                }
            } else if (!file.isFile() || !previous.delete()) {
                throw new IOException("Cannot finish recovery for " + file.getName());
            }
        }
        File pending = next(file);
        if (pending.exists() && !pending.delete()) {
            throw new IOException("Cannot remove pending CSV: " + pending.getName());
        }
    }

    /**
     * ย้ายไฟล์จริงเดิมเป็น .previous.csv แล้วนำ .next.csv มาแทนไฟล์จริง
     * หากนำไฟล์ใหม่มาแทนไม่ได้จะพยายามคืนไฟล์เดิม และโยน IOException เมื่อเกิดปัญหา
     * เมื่อสำเร็จจะพยายามลบไฟล์เดิมที่สำรองชั่วคราว ไม่รับประกันกรณี storage เสีย
     */
    public static void commit(File file) throws IOException {
        File pending = next(file);
        File previous = new File(file.getPath() + ".previous.csv");
        if (file.exists() && (!file.isFile() || !file.renameTo(previous))) {
            throw new IOException("Cannot move original CSV: " + file.getName());
        }
        if (!pending.renameTo(file)) {
            if (previous.exists() && !previous.renameTo(file)) {
                throw new IOException("Replacement failed; original data remains at " + previous.getAbsolutePath());
            }
            throw new IOException("Could not replace " + file.getName() + "; original restored.");
        }
        if (previous.exists()) {
            previous.delete();
        }
    }

}

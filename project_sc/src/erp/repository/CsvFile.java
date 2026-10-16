package erp.repository;

import java.io.*;

/**
 * อ่านและเขียน CSV แบบง่าย โดยหนึ่งแถวแทนหนึ่งรายการและคอลัมน์แรกเป็น ID
 * ไม่รองรับ comma, double quote หรือขึ้นบรรทัดใหม่ภายในค่า; ส่งการแทนที่ไฟล์ให้ SafeFiles
 */
public class CsvFile {
    private File file;
    private String header;

    /**
     * เก็บตำแหน่งไฟล์และ header ที่คาดหวัง ยังไม่อ่านหรือสร้างไฟล์จริง
     */
    public CsvFile(File folder, String filename, String header) {
        this.file = new File(folder, filename);
        this.header = header;
    }

    /**
     * จัดการไฟล์ที่ค้างผ่าน SafeFiles แล้วอ่าน CSV ทีละบรรทัด
     * ตรวจ header จำนวนคอลัมน์ ค่าในช่อง และ ID ว่าง/ซ้ำ; คืน String[][] ที่ไม่รวมหัวตาราง
     * หากผิดรูปแบบให้โยน IOException ไม่ได้แสดง dialog เอง
     */
    public String[][] read() throws IOException {
        SafeFiles.recover(file);
        String[][] rows = new String[0][];
        int columns = header.split(",", -1).length;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String first = reader.readLine();
            // ข้าม BOM ที่ต้น header หากโปรแกรมแก้ไขข้อความใส่มาในไฟล์
            if (first != null && first.startsWith("\uFEFF")) {
                first = first.substring(1);
            }
            if (!header.equals(first)) {
                throw new IOException("Incorrect CSV header: " + file.getName());
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] row = line.split(",", -1);
                if (row.length != columns) {
                    throw new IOException("Incorrect column count in " + file.getName() + ". Commas inside values are not supported.");
                }
                for (int i = 0; i < row.length; i++) {
                    checkField(row[i]);
                }
                if (row[0].trim().length() == 0) {
                    throw new IOException("An ID is missing in " + file.getName());
                }
                // ขยายขนาดตารางให้ใหญ่ขึ้น 1 แถว เพื่อรองรับข้อมูลใหม่
                String[][] expanded = new String[rows.length + 1][];
                for (int i = 0; i < rows.length; i++) {
                    if (rows[i][0].equals(row[0])) {
                        throw new IOException("Duplicate ID in " + file.getName());
                    }
                    expanded[i] = rows[i];
                }
                expanded[rows.length] = row;
                rows = expanded;
            }
        }
        return rows;
    }

    /**
     * ตรวจว่าไม่เป็น null และไม่มี comma, double quote หรือขึ้นบรรทัดใหม่
     * เป็นข้อจำกัดของตัวอ่านเขียนแบบง่ายนี้ ไม่ใช่ข้อห้ามของ CSV ทุกชนิด
     */
    private void checkField(String text) throws IOException {
        if (text == null || text.indexOf(',') >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            throw new IOException("CSV values cannot contain commas, double quotes or line breaks.");
        }
    }

    /**
     * ตรวจจำนวนคอลัมน์และค่าแต่ละช่อง แล้วเขียน header กับข้อมูลทั้งหมดลง .next.csv
     * ปิด writer ก่อนเรียก SafeFiles.commit() เพื่อนำไปแทนไฟล์จริง
     */
    private void write(String[][] rows) throws IOException {
        SafeFiles.recover(file);
        int columns = header.split(",", -1).length;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].length != columns) {
                throw new IOException("Incorrect column count.");
            }
            for (int j = 0; j < rows[i].length; j++) {
                checkField(rows[i][j]);
            }
        }
        // เขียนไฟล์ชั่วคราวให้ครบและปิด writer ก่อนเรียก commit() แทนที่ไฟล์จริง
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SafeFiles.next(file)))) {
            writer.write(header);
            writer.newLine();
            for (int i = 0; i < rows.length; i++) {
                for (int j = 0; j < rows[i].length; j++) {
                    if (j > 0) {
                        writer.write(",");
                    }
                    writer.write(rows[i][j]);
                }
                writer.newLine();
            }
        }
        SafeFiles.commit(file);
    }

    /**
     * อ่านข้อมูลเดิม หาเลข ID สูงสุดที่ยังอยู่ในไฟล์แล้วเพิ่มหนึ่ง
     * เช่น U001, U002 จะได้ U003; ใส่ ID ลง row[0] แล้วเขียนชุดข้อมูลใหม่
     * คืน ID เมื่อเขียนสำเร็จ; เลขที่เคยลบอาจถูกนำกลับมาใช้หากเป็นเลขท้ายสุด
     */
    public String add(String[] row, String prefix) throws IOException {
        String[][] rows = read();
        int highest = 0;
        for (int i = 0; i < rows.length; i++) {
            String id = rows[i][0];
            if (id.startsWith(prefix)) {
                int number = Integer.parseInt(id.substring(prefix.length()));
                if (number > highest) {
                    highest = number;
                }
            }
        }
        if (highest == Integer.MAX_VALUE) {
            throw new IOException("No more IDs are available.");
        }
        String number = "" + (highest + 1);
        while (number.length() < 3) {
            number = "0" + number;
        }
        row[0] = prefix + number;
        String[][] expanded = new String[rows.length + 1][];
        for (int i = 0; i < rows.length; i++) {
            expanded[i] = rows[i];
        }
        expanded[rows.length] = row;
        write(expanded);
        return row[0];
    }

     /**
      * หาแถวที่ ID ตรงกับ row[0] แล้วแทนทั้งแถวและเขียนไฟล์ใหม่
      * ถ้าไม่พบ ID ให้โยน IOException ไม่สร้างรายการเพิ่ม
      */
    public void update(String[] row) throws IOException {
        String[][] rows = read();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i][0].equals(row[0])) {
                rows[i] = row;
                write(rows);
                return;
            }
        }
        throw new IOException("This record no longer exists. Reopen the module.");
    }

    /**
     * อ่านข้อมูล หา ID แล้วสร้าง array ใหม่ที่ไม่รวมแถวนั้น ก่อนเขียนกลับ
     * ถ้าไม่พบ ID ให้โยน IOException
     */
    public void delete(String id) throws IOException {
        String[][] rows = read();
        int selected = -1;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i][0].equals(id)) {
                selected = i;
            }
        }
        if (selected == -1) {
            throw new IOException("This record no longer exists. Reopen the module.");
        }
        String[][] remaining = new String[rows.length - 1][];
        // ตัวนับสำหรับตารางใหม่
        int next = 0;
        for (int i = 0; i < rows.length; i++) {
            if (i != selected) {
                remaining[next] = rows[i];
                next++;
            }
        }
        write(remaining);
    }
}

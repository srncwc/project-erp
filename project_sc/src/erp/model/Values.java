package erp.model;

/**
 * ตัวช่วยตรวจและแปลงค่าพื้นฐาน เช่น ข้อความ จำนวน และเงิน
 * เงินในหน่วยความจำใช้ long หน่วยสตางค์ ส่วนข้อความบนหน้าจอและ CSV ใช้บาท
 */
public class Values {
    /**
     * ตรวจว่าข้อความไม่เป็น null หรือว่างหลัง trim แล้วคืนข้อความที่ตัดช่องว่างหัวท้าย
     */
    public static String required(String text, String name) {
        if (text == null || text.trim().length() == 0) {
            throw new IllegalArgumentException(name + " is required.");
        }
        return text.trim();
    }

    /**
     * คืน true เมื่อข้อความมีอย่างน้อยหนึ่งตัวและทุกตัวเป็นเลข 0–9
     */
    public static boolean digits(String text) {
        if (text.length() == 0) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * ตรวจรูปแบบ YYYY-MM-DD และช่วงพื้นฐานของปี เดือน วัน
     * ตรวจวันเพียง 1–31; การตรวจว่าวันนั้นมีจริงในเดือนต้องใช้ Rules.date()
     */
    public static String date(String text) {
        text = required(text, "Date");
        if (text.length() != 10 || text.charAt(4) != '-' || text.charAt(7) != '-') {
            throw new IllegalArgumentException("Enter the date as YYYY-MM-DD.");
        }
        String year = text.substring(0, 4);
        String month = text.substring(5, 7);
        String day = text.substring(8, 10);
        if (!digits(year) || !digits(month) || !digits(day)) {
            throw new IllegalArgumentException("Enter the date as YYYY-MM-DD.");
        }
        int y = Integer.parseInt(year);
        int m = Integer.parseInt(month);
        int d = Integer.parseInt(day);
        if (y < 1 || m < 1 || m > 12 || d < 1 || d > 31) {
            throw new IllegalArgumentException("Check the year, month and day.");
        }
        return text;
    }

    /**
     * แปลงข้อความเลข 0–9 เป็น int และตรวจว่าไม่น้อยกว่า minimum
     * หากว่าง รูปแบบผิด หรือเกินช่วง int ให้โยน IllegalArgumentException
     */
    public static int quantity(String text, int minimum) {
        text = required(text, "Quantity");
        try {
            if (!digits(text)) {
                throw new NumberFormatException();
            }
            int value = Integer.parseInt(text);
            if (value < minimum) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Quantity must be a whole number from " + minimum + " to " + Integer.MAX_VALUE + ".");
        }
    }

    /**
     * แปลงจำนวนบาทที่มีทศนิยมไม่เกินสองหลักเป็น long หน่วยสตางค์
     * เช่น "12.5" ได้ 1250 และ "50.00" ได้ 5000 โดยไม่ใช้ double คำนวณเงิน
     */
    public static long money(String text) {
        text = required(text, "Amount");
        String[] parts = text.split("\\.", -1);
        if (parts.length > 2 || !digits(parts[0])) {
            throw new IllegalArgumentException("Enter a non-negative amount, for example 125.50.");
        }
        String satang = "00";
        if (parts.length == 2) {
            if (!digits(parts[1]) || parts[1].length() > 2) {
                throw new IllegalArgumentException("Use one or two decimal places.");
            }
            satang = parts[1].length() == 1 ? parts[1] + "0" : parts[1];
        }
        try {
            // ต่อเลขบาทกับสตางค์สองหลัก เช่น 12.50 เป็น 1250 เพื่อหลีกเลี่ยงความคลาดเคลื่อนของ double
            return Long.parseLong(parts[0] + satang);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Amount is too large.");
        }
    }

    /**
     * แปลงเงินหน่วยสตางค์เป็นข้อความบาทสองตำแหน่ง
     * เช่น 5025 ได้ "50.25" สำหรับแสดงบนหน้าจอหรือเขียน CSV
     * ผู้เรียกควรส่งยอดไม่ติดลบ; Overview จัดเครื่องหมายลบแยกก่อนแสดง
     */
    public static String moneyText(long satang) {
        long remainder = satang % 100;
        return satang / 100 + "." + (remainder < 10 ? "0" : "") + remainder;
    }
}

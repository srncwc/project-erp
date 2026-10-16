package erp.model;



/**
 * รวมกฎตรวจข้อมูลที่ UI และ Service ใช้ร่วมกัน เช่น ความยาว จำนวน เงิน และวันที่
 * ข้อมูลที่ไม่ผ่านจะโยน IllegalArgumentException ให้ผู้เรียกจัดการ ไม่แสดงหน้าต่างเอง
 */
public class Rules {
    public static final int MAX_QUANTITY = 1000000;
    public static final long MAX_MONEY = 99999999999L;

    /**
     * ตรวจความยาวและห้าม comma, double quote หรือขึ้นบรรทัดใหม่ตามข้อจำกัด CSV รุ่นนี้
     * optional เป็น false ต้องมีข้อความ; คืนค่าที่ตัดช่องว่างหัวท้ายแล้ว
     */
    public static String text(String value, String name, int max, boolean optional) {
        if (value == null) {
            value = "";
        }
        if (value.length() > max) {
            throw new IllegalArgumentException(name + ": maximum " + max + " characters.");
        }
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(name + ": no comma / double quote / line break.");
        }
        if (!optional) {
            Values.required(value, name);
        }
        return value.trim();
    }

    /**
     * แปลงข้อความเป็นจำนวนเต็มและตรวจช่วง min ถึงค่าที่น้อยกว่าระหว่าง max กับ MAX_QUANTITY
     * ตัวอย่าง "2" จะได้ int 2 เมื่ออยู่ในช่วงที่อนุญาต
     */
    public static int quantity(String value, int min, long max) {
        int result = Values.quantity(value, min);
        long limit = Math.min(max, MAX_QUANTITY);
        if (result > limit) {
            throw new IllegalArgumentException("Quantity must be " + min + ".." + limit + ".");
        }
        return result;
    }

    /**
     * แปลงข้อความจำนวนบาทเป็น long หน่วยสตางค์ แล้วตรวจช่วงที่กำหนด
     * min, max และค่าที่คืนเป็นสตางค์ เช่น "50.25" คืน 5025; ไม่เกิน MAX_MONEY
     */
    public static long money(String value, long min, long max) {
        long amount = Values.money(value);
        long limit = Math.min(max, MAX_MONEY);
        if (amount < min || amount > limit) {
            throw new IllegalArgumentException(
                    "Amount must be " + Values.moneyText(min) + ".." + Values.moneyText(limit) + ".");
        }
        return amount;
    }

    /**
     * ตรวจรูปแบบ YYYY-MM-DD และวันจริงในเดือน รวมปีอธิกสุรทิน
     * เช่น 2026-02-30 ไม่ผ่าน; คืนวันที่ที่รับมาเมื่อถูกต้อง ไม่ได้ดึงวันที่วันนี้
     */
    public static String date(String value) {
        value = Values.date(value);
        int year = Integer.parseInt(value.substring(0, 4));
        int month = Integer.parseInt(value.substring(5, 7));
        int day = Integer.parseInt(value.substring(8, 10));
        int[] days = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) {
            days[1] = 29;
        }
        if (day > days[month - 1]) {
            throw new IllegalArgumentException("This date does not exist in the calendar.");
        }
        return value;
    }

    /**
     * คืนวันที่ปัจจุบันจาก Calendar ของเครื่องในรูปแบบ YYYY-MM-DD
     */
    public static String today() {
        java.util.Calendar now = java.util.Calendar.getInstance();
        int month = now.get(java.util.Calendar.MONTH) + 1;
        int day = now.get(java.util.Calendar.DAY_OF_MONTH);
        return now.get(java.util.Calendar.YEAR) + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "")
                + day;
    }

    /**
     * ตรวจวันที่รับจ่ายให้เป็นวันจริง ไม่ก่อนวันที่เอกสารและไม่หลังวันนี้
     * ตัวอย่าง หากเอกสารเป็น 2026-10-01 และวันนี้ 2026-10-16 จะรับวันที่ในช่วงนี้
     */
    public static String paymentDate(String date, String documentDate) {
        date = date(date);
        if (date.compareTo(documentDate) < 0 || date.compareTo(today()) > 0) {
            throw new IllegalArgumentException("Payment date must be " + documentDate + ".." + today() + ".");
        }
        return date;
    }

    /**
     * ตรวจวันที่ ชื่อคู่ค้า จำนวน ราคาต่อหน่วย และเพดานยอดรวมของเอกสาร
     * price ใช้หน่วยสตางค์; method นี้ไม่ตรวจสิทธิ์หรือจำนวนสต็อก
     */
    public static void transaction(String date, String party, int quantity, long price) {
        date(date);
        text(party, "Customer / Supplier", 100, false);
        quantity("" + quantity, 1, MAX_QUANTITY);
        money(Values.moneyText(price), 0, MAX_MONEY);
        if (price > MAX_MONEY / quantity) {
            throw new IllegalArgumentException("Document total exceeds 999999999.99.");
        }
    }
}

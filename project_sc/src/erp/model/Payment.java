package erp.model;

/**
 * ข้อมูลการรับหรือจ่ายเงินจริงหนึ่งรายการ อ้างอิงเอกสารต้นทางด้วย reference
 * เก็บยอดเป็นสตางค์ รวมทั้งสถานะและประวัติการยกเลิก (void)
 */
public class Payment {
    public String id, reference, date, recordedBy, note, voidDate, voidedBy, voidReason;
    public long amount;
    public boolean voided;

    /**
     * แปลง String[] ตามลำดับคอลัมน์ payments.csv เป็นข้อมูลรับจ่าย
     * ตรวจวันที่ ยอดเงิน ผู้บันทึก และรายละเอียด void; ยังไม่เขียนไฟล์
     */
    public Payment(String[] row) {
        id = row[0];
        reference = Values.required(row[1], "Reference");
        date = Rules.date(row[2]);
        amount = Rules.money(row[3], 1, Rules.MAX_MONEY);
        recordedBy = Values.required(row[4], "Recorded by");
        note = Rules.text(row[5], "Note", 200, true);
        if (!"true".equals(row[6]) && !"false".equals(row[6])) {
            throw new IllegalArgumentException("Invalid payment void flag.");
        }
        voided = "true".equals(row[6]);
        voidDate = row[7];
        voidedBy = row[8];
        voidReason = row[9];
        if (voided) {
            Rules.date(voidDate);
            Values.required(voidedBy, "Voided by");
            Rules.text(voidReason, "Reason", 200, false);
        } else if (voidDate.length() + voidedBy.length() + voidReason.length() > 0) {
            throw new IllegalArgumentException("Unexpected void details.");
        }
    }

    /**
     * แปลงข้อมูล Payment เป็นแถว CSV โดยแปลง amount จากสตางค์เป็นข้อความบาท
     */
    public String[] toRow() {
        return new String[] { id, reference, date, Values.moneyText(amount), recordedBy, note, "" + voided, voidDate,
                voidedBy, voidReason };
    }
}

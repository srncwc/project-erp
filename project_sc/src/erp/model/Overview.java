package erp.model;

/**
 * รวมข้อมูลสำหรับหน้า Overview จาก array ที่ได้รับ โดยไม่อ่านหรือบันทึก CSV เอง
 * แยกยอดซื้อขายตามเอกสาร ออกจากเงินจริงที่รับจ่ายและยอดค้าง;
 * เงินสุทธิไม่ใช่กำไรทางบัญชี
 */
public class Overview {
    public long salesTotal;
    public long purchasesTotal;
    public long income;
    public long expense;
    public long pendingIncome;
    public long pendingExpense;
    public int salesCount;
    public int purchasesCount;
    public int userCount;
    public int adminCount;
    public int roleCount;
    public StockItem[] stock;
    public FinanceEntry[] entries;

    /**
     * รวมยอดจาก array ที่รับมา โดยแยกยอดซื้อขายตามเอกสารจากยอดเงินที่ชำระแล้ว
     * นับบัญชี Admin และ Role และรวมยอดค้างแต่ละประเภท หน่วยเงินเป็นสตางค์
     */
    public Overview(Sale[] sales, Purchase[] purchases, StockItem[] stock,
            FinanceEntry[] entries, UserAccount[] users, Role[] roles) {
        this.stock = stock;
        this.entries = entries;
        userCount = users.length;
        roleCount = roles.length;
        for (int i = 0; i < users.length; i++) {
            if (users[i].isAdmin()) {
                adminCount++;
            }
        }
        for (int i = 0; i < sales.length; i++) {
            salesCount++;
            salesTotal = add(salesTotal, sales[i].total());
        }
        for (int i = 0; i < purchases.length; i++) {
            purchasesCount++;
            purchasesTotal = add(purchasesTotal, purchases[i].total());
        }
        for (int i = 0; i < entries.length; i++) {
            if ("INCOME".equals(entries[i].getType())) {
                income = add(income, entries[i].getSettled());
                pendingIncome = add(pendingIncome, entries[i].getOutstanding());
            } else {
                expense = add(expense, entries[i].getSettled());
                pendingExpense = add(pendingExpense, entries[i].getOutstanding());
            }
        }
    }

    /**
     * บวกยอดโดยตรวจไม่ให้ผลรวมเกิน Long.MAX_VALUE
     * ใช้กับยอดไม่ติดลบที่ส่งมาจากข้อมูลสรุป
     */
    private long add(long a, long b) {
        if (a > Long.MAX_VALUE - b) {
            throw new IllegalArgumentException("Dashboard total is too large.");
        }
        return a + b;
    }

    /**
     * คืนเงินจริงที่รับลบเงินจริงที่จ่ายเป็นข้อความบาท พร้อมเครื่องหมายลบถ้าติดลบ
     * เป็นกระแสเงินสดสุทธิ ไม่ใช่กำไรทางบัญชี
     */
    public String netText() {
        return income >= expense ? Values.moneyText(income - expense) : "-" + Values.moneyText(expense - income);
    }
}

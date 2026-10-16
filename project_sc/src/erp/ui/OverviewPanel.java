package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * หน้าสรุปสำหรับ Admin แสดงยอดซื้อขาย เงินรับจ่ายจริง ยอดค้าง สต็อก และบัญชี
 * แบบ Real-time
 */
public class OverviewPanel extends JPanel implements ActionListener {
    private AppFrame frame;
    private ErpService service;
    private JPanel content = new JPanel(new BorderLayout(0, 20));
    private JButton refresh = Ui.button("Refresh", true);

    /**
     * สร้างหน้าจอ Overview พร้อมจัดวาง Layout และผูก Event Listener
     *
     * @param frame   หน้าต่างหลักของระบบ ใช้สำหรับเปลี่ยนหน้า (Navigation)
     * @param service Service กลางของระบบ ใช้ดึงข้อมูลสรุปภาพรวม
     */
    public OverviewPanel(AppFrame frame, ErpService service) {
        this.frame = frame;
        this.service = service;
        setLayout(new BorderLayout(0, 18));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(26, 28, 24, 28));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(Ui.label("Overview", 28, true, Ui.INK), BorderLayout.WEST);
        heading.add(refresh, BorderLayout.EAST);
        refresh.addActionListener(this);
        add(heading, BorderLayout.NORTH);
        content.setOpaque(false);
        add(content, BorderLayout.CENTER);
        reload();
    }

    /**
     * ดึงข้อมูล Overview จาก ErpService
     * สร้างกล่องสรุป 6 ใบ (ยอดขาย, ยอดซื้อ, กระแสเงินสด, เงินรับ, เงินจ่าย,
     * บัญชีผู้ใช้)
     * สร้างตารางสินค้า พร้อมระบบเตือนภัยอัตโนมัติ (แจ้งเตือนหากสต็อก <= 5 หรือหมด)
     * เพิ่มหมายเหตุอธิบายหลักการคำนวณ (เช่น กระแสเงินสด != กำไรทางบัญชี)
     * หากเกิดข้อผิดพลาด จะแสดงข้อความแจ้งเตือนแทนตารางข้อมูล
     */
    public void reload() {
        content.removeAll();
        try {
            Overview data = service.overview();
            JPanel cards = new JPanel(new GridLayout(2, 3, 14, 14));
            cards.setOpaque(false);
            cards.add(card("Sales", Values.moneyText(data.salesTotal), data.salesCount + " linked sales", "Sales"));
            cards.add(card("Procurement", Values.moneyText(data.purchasesTotal),
                    data.purchasesCount + " linked purchases", "Procurement"));
            cards.add(card("Net cash flow", data.netText(), "Actual receipts minus payments; THB", "Finance"));
            cards.add(card("Money received", Values.moneyText(data.income),
                    "To receive: " + Values.moneyText(data.pendingIncome), "Finance"));
            cards.add(card("Money paid", Values.moneyText(data.expense),
                    "To pay: " + Values.moneyText(data.pendingExpense), "Finance"));
            cards.add(card("Accounts & roles", data.userCount + " accounts",
                    data.adminCount + " admins / " + data.roleCount + " roles", "Administration"));
            content.add(cards, BorderLayout.NORTH);

            JPanel warehouse = new JPanel(new BorderLayout(0, 12));
            warehouse.setOpaque(false);
            int low = 0;
            String[][] rows = new String[data.stock.length][5];
            for (int i = 0; i < data.stock.length; i++) {
                StockItem item = data.stock[i];
                if (item.getQuantity() <= 5) {
                    low++;
                }
                rows[i] = new String[] { item.getCode(), item.getName(), "" + item.getQuantity(), item.getUnit(),
                        item.getQuantity() == 0 ? "Out of stock"
                                : item.getQuantity() <= 5 ? "Low (<= 5)" : "Available" };
            }
            warehouse.add(Ui.label("Warehouse  /  " + data.stock.length + " products  /  " + low + " low or empty",
                    17, true, Ui.INK), BorderLayout.NORTH);
            DataTable table = new DataTable(new String[] { "Code", "Product", "Available", "Unit", "Status" },
                    new int[] { 2 });
            table.setRows(rows);
            warehouse.add(table, BorderLayout.CENTER);
            content.add(warehouse, BorderLayout.CENTER);
            JPanel notes = new JPanel(new GridLayout(2, 1, 0, 6));
            notes.setOpaque(false);
            notes.add(Ui.label(
                    data.entries.length + " finance entries / All sales and purchases are linked to Warehouse.",
                    12, false, Ui.MUTED));
            notes.add(Ui.label(
                    "Stock follows documents. Cash follows active payments only; cash flow is not accounting profit.",
                    12, false, Ui.MUTED));
            content.add(notes, BorderLayout.SOUTH);
        } catch (Exception error) {
            content.add(Ui.label("Overview could not be loaded. " + error.getMessage(), 13, false, Ui.DARK_ORANGE));
        }
        content.revalidate();
        content.repaint();
    }

    /**
     * สร้างกล่องแสดงหัวข้อ ยอด และรายละเอียด พร้อมปุ่มส่งชื่อโมดูลให้ AppFrame
     */
    private JPanel card(String title, String value, String detail, String module) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Ui.SOFT);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Ui.LINE),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JButton button = Ui.button(title, false);
        button.setActionCommand(module);
        button.addActionListener(this);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(Ui.SOFT);
        panel.add(button, BorderLayout.NORTH);
        panel.add(Ui.label(value, 23, true, Ui.DARK_ORANGE), BorderLayout.CENTER);
        panel.add(Ui.label(detail, 11, false, Ui.MUTED), BorderLayout.SOUTH);
        return panel;
    }

    /**
     * จัดการเหตุการณ์เมื่อกดปุ่มต่างๆ บนหน้าจอ
     * - หากกดปุ่ม Refresh: จะเรียกเมธอด reload() เพื่อดึงข้อมูลล่าสุด
     * - หากกดปุ่มบนการ์ดสรุป: จะสั่งให้ AppFrame เปิดโมดูลที่ตรงกับการ์ดนั้น
     *
     * @param event เหตุการณ์ที่เกิดขึ้นจากปุ่มบนหน้าจอ
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == refresh) {
            reload();
        } else {
            frame.openModule(event.getActionCommand());
        }
    }
}

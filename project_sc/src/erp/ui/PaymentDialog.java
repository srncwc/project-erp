package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * หน้าต่างประวัติรับจ่ายจริงของเอกสารหนึ่งรายการ
 * ให้บันทึกเงินหรือยกเลิกรายการพร้อมเหตุผล ผ่าน ErpService โดยไม่เปลี่ยนสต็อก
 */
public class PaymentDialog extends JDialog implements ActionListener {
    private AppFrame frame;
    private ErpService service;
    private String reference;
    private FinanceEntry current;
    private Payment[] records = new Payment[0];
    private JLabel summary = Ui.label(" ", 14, true, Ui.INK);
    private DataTable table = new DataTable(new String[] { "ID", "Date", "Amount", "Recorded by", "Note", "Status",
            "Void date", "Voided by", "Reason" }, new int[] { 2 });
    private JButton record = Ui.button("Record payment", true), cancel = Ui.button("Void selected payment", false),
            close = Ui.button("Close", false);

    /**
     * เตรียม modal dialog ของเอกสารที่อ้างด้วย reference และโหลดประวัติ
     * ยังไม่แสดงหน้าต่างจนกว่าผู้เรียกจะสั่ง setVisible(true)
     */
    public PaymentDialog(AppFrame frame, ErpService service, String reference) {
        super(frame, "Receipts / Payments — " + reference, true);
        this.frame = frame;
        this.service = service;
        this.reference = reference;
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        page.add(summary, BorderLayout.NORTH);
        page.add(table, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(record);
        buttons.add(cancel);
        buttons.add(close);
        page.add(buttons, BorderLayout.SOUTH);
        record.addActionListener(this);
        cancel.addActionListener(this);
        close.addActionListener(this);
        setContentPane(page);
        setSize(1050, 600);
        setLocationRelativeTo(frame);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        reload();
    }

    /**
     * อ่านเอกสารและประวัติรับจ่ายใหม่ แล้วแสดงยอดรวม ยอดชำระ และยอดค้าง
     * เปิดปุ่มบันทึกเมื่อยังมียอดค้าง; หากอ่านผิดพลาดปิดปุ่มบันทึกและ void
     */
    private void reload() {
        try {
            current = service.financeEntry(reference);
            records = service.paymentHistory(reference);
            boolean income = "INCOME".equals(current.getType());
            summary.setText(
                    reference + " | " + current.getType() + " | Total " + Values.moneyText(current.getAmountSatang())
                            + " | Settled " + Values.moneyText(current.getSettled()) + " | Outstanding "
                            + Values.moneyText(current.getOutstanding()));
            summary.setToolTipText(current.getDescription());
            record.setText(income ? "Record receipt" : "Record payment");
            record.setEnabled(current.getOutstanding() > 0);
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                Payment item = records[i];
                rows[i] = new String[] { item.id, item.date, Values.moneyText(item.amount), item.recordedBy, item.note,
                        item.voided ? "Voided" : "Active", item.voidDate, item.voidedBy, item.voidReason };
            }
            table.setRows(rows);
        } catch (Exception error) {
            record.setEnabled(false);
            cancel.setEnabled(false);
            JOptionPane.showMessageDialog(this, error.getMessage());
        }
    }

    /**
     * แยกปุ่มปิด บันทึกเงิน และยกเลิกรายการที่เลือก
     * การ void ต้องเลือกแถวที่ยังไม่ยกเลิกและกรอกเหตุผลก่อนส่งให้ Service
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == close) {
            dispose();
            return;
        }
        if (event.getSource() == record) {
            recordMoney();
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a payment first.");
            return;
        }
        if (records[row].voided) {
            JOptionPane.showMessageDialog(this, "This payment is already voided.");
            return;
        }
        JTextField reason = new JTextField();
        EntryForm form = new EntryForm("Void payment " + records[row].id, new String[] { "Reason" },
                new JComponent[] { reason });
        while (form.showDialog(frame)) {
            try {
                service.voidPayment(records[row].id, reason.getText());
                reload();
                return;
            } catch (Exception error) {
                JOptionPane.showMessageDialog(this, error.getMessage());
            }
        }
    }

    /**
     * เปิดฟอร์มรับจ่ายโดยตั้งยอดเริ่มต้นเป็นยอดค้างทั้งหมด ผู้ใช้ลดเพื่อแบ่งจ่ายได้
     * ส่งเอกสารที่เห็นเป็น expected ให้ Service ตรวจเทียบข้อมูลล่าสุด
     * ถ้าบันทึกเกิดข้อผิดพลาดให้แจ้ง โหลดใหม่ และออกจาก method
     * เพื่อให้เปิดฟอร์มใหม่
     */
    private void recordMoney() {
        FinanceEntry expected = current;
        JTextField date = new JTextField(Rules.today());
        JTextField amount = new JTextField(Values.moneyText(expected.getOutstanding()));
        JTextField note = new JTextField();
        date.putClientProperty("documentDate", expected.getDate());
        amount.putClientProperty("maxMoney", expected.getOutstanding());
        EntryForm form = new EntryForm("INCOME".equals(expected.getType()) ? "Record receipt" : "Record payment",
                new String[] { "Payment date", "Amount", "Note" }, new JComponent[] { date, amount, note });
        while (form.showDialog(frame)) {
            try {
                service.recordPayment(expected, date.getText(), Values.money(amount.getText()), note.getText());
                reload();
                return;
            } catch (Exception error) {
                JOptionPane.showMessageDialog(this, error.getMessage());
                reload();
                return;
            }
        }
    }
}

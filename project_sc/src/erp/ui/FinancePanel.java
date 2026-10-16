package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

/**
 * แสดงผลเอกสาร ยอดรับจ่าย และยอดค้าง ทั้งรายการกรอกเองและรายการจากซื้อขาย
 * รายการที่เชื่อมเอกสารเปิดดูการรับจ่ายผ่าน PaymentDialog
 * แทนการแก้ยอดต้นทางที่หน้านี้
 */
public class FinancePanel extends JPanel implements ActionListener {

    private AppFrame frame;
    private ErpService service;
    private DataTable table = new DataTable(
            new String[] { "ID", "Date", "Type", "Description", "Amount", "Settled", "Outstanding", "Payment status" },
            new int[] { 4, 5, 6 });
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add", true);
    private JButton edit = Ui.button("Edit", false);
    private JButton delete = Ui.button("Delete", false);
    private JButton payment = Ui.button("Receipts / Payments", false);
    private JButton back = Ui.button("Back", false);
    // records เก็บรายการต้นทาง; DataTable.getSelectedRow() คืน index
    // ต้นทางแม้ตารางถูกกรอง ไม่ใช่ ID
    private FinanceEntry[] records = new FinanceEntry[0];

    /**
     * สร้างหน้า Finance ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public FinancePanel(AppFrame frame, ErpService service) {
        this.frame = frame;
        this.service = service;

        Ui.modulePage(this, "Finance",
                "Linked entries: Edit opens payments. Manual entries: Edit changes details.",
                table, status, add, edit, delete, back, payment);
        payment.addActionListener(this);

        add.addActionListener(this);
        edit.addActionListener(this);
        delete.addActionListener(this);
        back.addActionListener(this);
        reload();
    }

    /**
     * รับปุ่มเพิ่ม แก้ไข ลบ กลับเมนู หรือ Receipts / Payments
     * ปุ่มรับจ่ายเปิดประวัติของรายการที่เลือกผ่าน openPayments()
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == payment) {
            FinanceEntry item = selected();
            if (item != null) {
                openPayments(item);
            }
        } else if (source == add) {
            editRecord(null);
        } else if (source == edit) {
            FinanceEntry selected = selected();
            if (selected != null) {
                editRecord(selected);
            }
        } else if (source == delete) {
            deleteSelected();
        } else if (source == back) {
            frame.showMenu();
        }
    }

    /**
     * อ่านข้อมูลล่าสุดจาก Service แล้วแปลงเป็นข้อความส่งให้ DataTable
     * ถ้าอ่านผิดพลาดให้ล้างรายการที่แสดงและแจ้งข้อผิดพลาด
     */
    public void reload() {
        try {
            records = service.finance();
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                FinanceEntry item = records[i];
                rows[i] = new String[] {
                        item.getId(),
                        item.getDate(),
                        item.getType(),
                        item.getDescription(),
                        Values.moneyText(item.getAmountSatang()),
                        Values.moneyText(item.getSettled()),
                        Values.moneyText(item.getOutstanding()),
                        item.getPaymentText()
                };
            }
            table.setRows(rows);
            status.setText(records.length + " records - Select a row to edit or delete.");
        } catch (Exception error) {
            records = new FinanceEntry[0];
            table.setRows(new String[0][]);
            status.setText("Data could not be loaded.");
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * อ่าน index ของข้อมูลเดิมจาก DataTable แล้วคืนรายการใน records
     * ถ้ายังไม่เลือกให้แสดงข้อความและคืน null
     */
    private FinanceEntry selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select a record first.");
            return null;
        }
        return records[row];
    }

    /**
     * รายการที่เชื่อมซื้อขายให้เปิด PaymentDialog แทนฟอร์มแก้รายละเอียด
     * รายการกรอกเองที่มียอดชำระให้เปิดประวัติเพื่อ void ก่อน
     * รายการกรอกเองที่แก้ได้จึงเปิดฟอร์มและส่งให้ Service ตรวจบันทึก
     */
    private void editRecord(FinanceEntry existing) {
        if (existing != null && !existing.isLinked() && existing.getSettled() > 0) {
            JOptionPane.showMessageDialog(this, "Void payments with a reason before changing these details.");
            openPayments(existing);
            return;
        }
        if (existing != null && existing.isLinked()) {
            openPayments(existing);
            return;
        }

        // รายการใหม่เริ่มวันที่ว่างให้ผู้ใช้กรอกเอง;
        // เมื่อแก้ไขจะเติมวันที่จากเอกสารเดิม
        JTextField date = new JTextField();
        JComboBox<String> type = new JComboBox<String>(new String[] { "INCOME", "EXPENSE" });
        JTextField description = new JTextField();
        JTextField amount = new JTextField("0.00");
        String id = "";
        String dialogTitle = "Add Finance";

        if (existing != null) {
            id = existing.getId();
            date.setText(existing.getDate());
            type.setSelectedItem(existing.getType());
            description.setText(existing.getDescription());
            amount.setText(Values.moneyText(existing.getAmountSatang()));
            dialogTitle = "Edit Finance";
        }

        String[] labels = { "Date (YYYY-MM-DD)", "Type", "Description", "Amount" };
        JComponent[] inputs = { date, type, description, amount };

        EntryForm form = new EntryForm(dialogTitle, labels, inputs);
        // เข้าลูปเมื่อ Save ผ่านการตรวจฟอร์ม; ถ้า Model/Service
        // โยนข้อผิดพลาดที่จับไว้จะวนเปิดใหม่
        while (form.showDialog(frame)) {
            try {
                FinanceEntry value = new FinanceEntry(id, date.getText(),
                        (String) type.getSelectedItem(), description.getText(), Values.money(amount.getText()));
                service.saveFinance(value);

                reload();
                if (existing == null) {
                    status.setText("Record added.");
                } else {
                    status.setText("Record updated.");
                }
                return;
            } catch (IOException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException error) {
                JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * เปิด PaymentDialog ของ ID ที่เลือกแบบ modal แล้วโหลด Finance
     * ใหม่เมื่อปิดหน้าต่าง
     */
    private void openPayments(FinanceEntry item) {
        new PaymentDialog(frame, service, item.getId()).setVisible(true);
        reload();
    }

    /**
     * อ่านรายการที่เลือก ขอให้ผู้ใช้ยืนยัน แล้วส่ง ID ให้ Service ลบ
     * สำเร็จโหลดตารางใหม่ ถ้าไม่ผ่านให้แสดงข้อผิดพลาด
     */
    private void deleteSelected() {
        FinanceEntry item = selected();
        if (item == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete " + item.getId() + "?",
                "Delete record", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.deleteFinance(item.getId());
            reload();
            status.setText("Record deleted.");
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }
}

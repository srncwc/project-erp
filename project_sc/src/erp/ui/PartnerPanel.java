package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.event.*;
import javax.swing.*;

/**
 * หน้าจอทะเบียนลูกค้าหรือผู้จัดหา ตามค่า customer ที่รับมา
 * ส่งงานเพิ่ม แก้ไข และลบให้ ErpService โดยใช้สิทธิ์ของ Sales หรือ Procurement
 */
public class PartnerPanel extends JPanel implements ActionListener {
    private AppFrame frame;
    private ErpService service;
    private boolean customer;
    private Partner[] records = new Partner[0];
    private DataTable table = new DataTable(new String[] { "ID", "Name", "Contact" }, new int[] {});
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add", true), edit = Ui.button("Edit", false), delete = Ui.button("Delete", false),
            back = Ui.button("Back", false);

    /**
     * สร้างหน้า Partner ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public PartnerPanel(AppFrame frame, ErpService service, boolean customer) {
        this.frame = frame;
        this.service = service;
        this.customer = customer;
        Ui.modulePage(this, customer ? "Customers" : "Suppliers",
                "Registered names used by documents are kept; contact details remain editable.", table, status, add,
                edit, delete, back);
        add.addActionListener(this);
        edit.addActionListener(this);
        delete.addActionListener(this);
        back.addActionListener(this);
        reload();
    }

    /**
     * อ่านข้อมูลล่าสุดจาก Service แล้วแปลงเป็นข้อความส่งให้ DataTable
     * ถ้าอ่านผิดพลาดให้ล้างรายการที่แสดงและแจ้งข้อผิดพลาด
     */
    public void reload() {
        try {
            records = service.partners(customer);
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                rows[i] = new String[] { records[i].getId(), records[i].getName(), records[i].getContact() };
            }
            table.setRows(rows);
            status.setText(records.length + " registered partners.");
        } catch (Exception error) {
            records = new Partner[0];
            table.setRows(new String[0][]);
            JOptionPane.showMessageDialog(this, error.getMessage());
        }
    }

    /**
     * แยกปุ่มที่ผู้ใช้กดเพื่อเพิ่ม แก้ไข ลบ หรือกลับเมนู
     * การเปลี่ยนข้อมูลส่งต่อให้ Service ตรวจสิทธิ์และเงื่อนไข
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == back) {
            frame.showMenu();
            return;
        }
        if (event.getSource() == add) {
            editPartner(null);
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select a partner first.");
            return;
        }
        if (event.getSource() == edit) {
            editPartner(records[row]);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete " + records[row].getName() + "?", "Delete partner",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.deletePartner(records[row].getId(), customer);
            reload();
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
        }
    }

    /**
     * เปิดฟอร์มชื่อและช่องทางติดต่อ พร้อมรายชื่ออื่นไว้ตรวจซ้ำขณะกรอก
     * Save แล้วส่ง Partner ให้ Service ตรวจและบันทึก ตามชนิดลูกค้าหรือผู้จัดหา
     */
    private void editPartner(Partner existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getName());
        JTextField contact = new JTextField(existing == null ? "" : existing.getContact());
        String[] duplicate = new String[records.length];
        for (int i = 0; i < records.length; i++) {
            duplicate[i] = existing != null && records[i].getId().equals(existing.getId()) ? "" : records[i].getName();
        }
        name.putClientProperty("duplicates", duplicate);
        EntryForm form = new EntryForm(existing == null ? "Add partner" : "Edit partner",
                new String[] { "Name", "Contact" }, new JComponent[] { name, contact });
        while (form.showDialog(frame)) {
            try {
                service.savePartner(
                        new Partner(existing == null ? "" : existing.getId(), name.getText(), contact.getText()),
                        customer);
                reload();
                return;
            } catch (Exception error) {
                JOptionPane.showMessageDialog(this, error.getMessage());
            }
        }
    }
}

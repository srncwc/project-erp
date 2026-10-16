package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

/**
 * หน้าจอทะเบียนสินค้าและจำนวนคงเหลือที่คำนวณจากเอกสารซื้อขาย
 * สินค้าที่มีเอกสารอ้างอิงจะล็อกการแก้จำนวนและหน่วยในฟอร์ม
 */
public class WarehousePanel extends JPanel implements ActionListener {

    private AppFrame frame;
    private ErpService service;
    private DataTable table = new DataTable(
            new String[] { "ID", "Product code", "Product name", "Unit", "Current quantity" },
            new int[] { 4 });
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add", true);
    private JButton edit = Ui.button("Edit", false);
    private JButton delete = Ui.button("Delete", false);
    private JButton back = Ui.button("Back", false);
    // records เก็บรายการต้นทาง; DataTable.getSelectedRow() คืน index
    // ต้นทางแม้ตารางถูกกรอง ไม่ใช่ ID
    private StockItem[] records = new StockItem[0];

    /**
     * สร้างหน้า Warehouse ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public WarehousePanel(AppFrame frame, ErpService service) {
        this.frame = frame;
        this.service = service;

        Ui.modulePage(this, "Warehouse",
                "Opening stock + purchases - sales. Linked quantity/unit are locked.",
                table, status, add, edit, delete, back);

        add.addActionListener(this);
        edit.addActionListener(this);
        delete.addActionListener(this);
        back.addActionListener(this);
        reload();
    }

    /**
     * แยกปุ่มที่ผู้ใช้กดเพื่อเพิ่ม แก้ไข ลบ หรือกลับเมนู
     * การเปลี่ยนข้อมูลส่งต่อให้ Service ตรวจสิทธิ์และเงื่อนไข
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == add) {
            editRecord(null);
        } else if (source == edit) {
            StockItem selected = selected();
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
            records = service.stock();
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                StockItem item = records[i];
                rows[i] = new String[] {
                        item.getId(),
                        item.getCode(),
                        item.getName(),
                        item.getUnit(),
                        "" + item.getQuantity()
                };
            }
            table.setRows(rows);
            status.setText(records.length + " records - Select a row to edit or delete.");
        } catch (Exception error) {
            records = new StockItem[0];
            table.setRows(new String[0][]);
            status.setText("Data could not be loaded.");
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * อ่าน index ของข้อมูลเดิมจาก DataTable แล้วคืนรายการใน records
     * ถ้ายังไม่เลือกให้แสดงข้อความและคืน null
     */
    private StockItem selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select a record first.");
            return null;
        }
        return records[row];
    }

    /**
     * เปิดฟอร์มเพิ่มเมื่อ existing เป็น null หรือเติมข้อมูลเดิมเมื่อแก้ไข
     * Save ผ่านการตรวจฟอร์มแล้วจึงสร้าง Model ส่งให้ Service; สำเร็จโหลดตารางใหม่
     * ถ้า Service แจ้งข้อผิดพลาดที่จับไว้จะเปิดฟอร์มเดิมให้แก้ ส่วน Cancel
     * ออกโดยไม่บันทึก
     * สินค้าที่มีเอกสารเชื่อมจะล็อกจำนวนและหน่วย เพื่อให้ปรับยอดผ่านเอกสารต้นทาง
     */
    private void editRecord(StockItem existing) {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        JTextField unit = new JTextField("piece");
        JTextField quantity = new JTextField("0");
        String id = "";
        String dialogTitle = "Add Warehouse";

        if (existing != null) {
            id = existing.getId();
            code.setText(existing.getCode());
            name.setText(existing.getName());
            unit.setText(existing.getUnit());
            quantity.setText("" + existing.getQuantity());
            try {
                // จำนวนคงเหลือของสินค้าที่เชื่อมแล้วคำนวณจากเอกสาร
                // จึงล็อกจำนวนและหน่วยในฟอร์มนี้
                boolean linked = service.isStockLinked(existing.getId());
                quantity.setEditable(!linked);
                unit.setEditable(!linked);
                if (linked) {
                    quantity.setToolTipText("Calculated from opening stock + purchases - sales.");
                }
            } catch (Exception error) {
                JOptionPane.showMessageDialog(this, error.getMessage());
                return;
            }
            dialogTitle = "Edit Warehouse";
        }

        String[] labels = { "Product code", "Product name", "Unit", "Current quantity" };
        JComponent[] inputs = { code, name, unit, quantity };

        String[] duplicates = new String[records.length];
        for (int i = 0; i < records.length; i++) {
            duplicates[i] = existing != null && records[i].getId().equals(existing.getId()) ? "" : records[i].getCode();
        }
        code.putClientProperty("duplicates", duplicates);
        EntryForm form = new EntryForm(dialogTitle, labels, inputs);
        // เข้าลูปเมื่อ Save ผ่านการตรวจฟอร์ม; ถ้า Model/Service
        // โยนข้อผิดพลาดที่จับไว้จะวนเปิดใหม่
        while (form.showDialog(frame)) {
            try {
                StockItem value = new StockItem(id, code.getText(), name.getText(),
                        unit.getText(), Values.quantity(quantity.getText(), 0));
                service.saveStock(value);

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
     * อ่านรายการที่เลือก ขอให้ผู้ใช้ยืนยัน แล้วส่ง ID ให้ Service ลบ
     * สำเร็จโหลดตารางใหม่ ถ้าไม่ผ่านให้แสดงข้อผิดพลาด
     */
    private void deleteSelected() {
        StockItem item = selected();
        if (item == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete " + item.getId() + "?",
                "Delete record", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.deleteStock(item.getId());
            reload();
            status.setText("Record deleted.");
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }
}

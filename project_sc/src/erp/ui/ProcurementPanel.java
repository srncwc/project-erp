package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

/**
 * หน้าจอรายการซื้อ รับข้อมูลผู้จัดหา สินค้า จำนวน และต้นทุน
 * ส่ง Purchase ให้ ErpService ตรวจเงื่อนไขและบันทึก แล้วโหลดตารางใหม่
 */
public class ProcurementPanel extends JPanel implements ActionListener {

    private AppFrame frame;
    private ErpService service;
    private DataTable table = new DataTable(
            new String[] { "ID", "Date", "Supplier", "Product", "Quantity", "Unit cost", "Total" },
            new int[] { 4, 5, 6 });
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add", true);
    private JButton edit = Ui.button("Edit", false);
    private JButton delete = Ui.button("Delete", false);
    private JButton back = Ui.button("Back", false);
    // records เก็บรายการต้นทาง; DataTable.getSelectedRow() คืน index
    // ต้นทางแม้ตารางถูกกรอง ไม่ใช่ ID
    private Purchase[] records = new Purchase[0];

    /**
     * สร้างหน้า Procurement ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public ProcurementPanel(AppFrame frame, ErpService service) {
        this.frame = frame;
        this.service = service;

        Ui.modulePage(this, "Procurement",
                "Save updates stock. Record actual receipts / payments in Finance.",
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
            Purchase selected = selected();
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
            records = service.purchases();
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                Purchase item = records[i];
                rows[i] = new String[] {
                        item.getId(),
                        item.getDate(),
                        item.getSupplier(),
                        item.getProduct(),
                        "" + item.getQuantity(),
                        Values.moneyText(item.getUnitCostSatang()),
                        Values.moneyText(item.total())
                };
            }
            table.setRows(rows);
            status.setText(records.length + " records - Select a row to edit or delete.");
        } catch (Exception error) {
            records = new Purchase[0];
            table.setRows(new String[0][]);
            status.setText("Data could not be loaded.");
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * อ่าน index ของข้อมูลเดิมจาก DataTable แล้วคืนรายการใน records
     * ถ้ายังไม่เลือกให้แสดงข้อความและคืน null
     */
    private Purchase selected() {
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
     */
    private void editRecord(Purchase existing) {
        ProductPicker product;
        try {
            product = new ProductPicker(service.products());
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
            return;
        }

        // รายการใหม่เริ่มวันที่ว่างให้ผู้ใช้กรอกเอง;
        // เมื่อแก้ไขจะเติมวันที่จากเอกสารเดิม
        JTextField date = new JTextField();
        PartnerPicker supplier;
        try {
            supplier = new PartnerPicker(service.partners(false));
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
            return;
        }
        JTextField quantity = new JTextField("1");
        JTextField unitCost = new JTextField("0.00");
        String id = "";
        String dialogTitle = "Add Procurement";

        if (existing != null) {
            id = existing.getId();
            date.setText(existing.getDate());
            supplier.selectName(existing.getSupplier());
            product.selectProduct(existing.getProductId());
            quantity.setText("" + existing.getQuantity());
            unitCost.setText(Values.moneyText(existing.getUnitCostSatang()));
            dialogTitle = "Edit Procurement";
        }

        String[] labels = { "Date (YYYY-MM-DD)", "Supplier", "Product", "Quantity", "Unit cost" };
        JComponent[] inputs = { date, supplier, product, quantity, unitCost };

        unitCost.putClientProperty("quantityField", quantity);
        quantity.putClientProperty("purchasePicker", product);
        quantity.putClientProperty("oldProduct", existing == null ? "" : existing.getProductId());
        quantity.putClientProperty("oldQuantity", existing == null ? 0 : existing.getQuantity());
        EntryForm form = new EntryForm(dialogTitle, labels, inputs);
        // เข้าลูปเมื่อ Save ผ่านการตรวจฟอร์ม; ถ้า Model/Service
        // โยนข้อผิดพลาดที่จับไว้จะวนเปิดใหม่
        while (form.showDialog(frame)) {
            try {
                Purchase value = new Purchase(id, date.getText(), supplier.selectedName(),
                        product.getProductName(), Values.quantity(quantity.getText(), 1),
                        Values.money(unitCost.getText()), product.getProductId());
                service.savePurchase(value);

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
        Purchase item = selected();
        if (item == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete " + item.getId() + "?",
                "Delete record", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.deletePurchase(item.getId());
            reload();
            status.setText("Record deleted.");
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }
}

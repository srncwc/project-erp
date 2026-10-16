package erp.ui;

import erp.model.*;
import erp.service.ErpService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

/**
 * หน้าจอรายการขาย รับการเพิ่ม แก้ไข ลบ และแสดงข้อมูลในตาราง
 * สร้าง Sale จากฟอร์มแล้วส่งให้ ErpService ตรวจสิทธิ์ สต็อก และบันทึก
 */
public class SalesPanel extends JPanel implements ActionListener {

    private AppFrame frame;
    private ErpService service;
    private DataTable table = new DataTable(
            new String[] { "ID", "Date", "Customer", "Product", "Quantity", "Unit price", "Total" },
            new int[] { 4, 5, 6 });
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add", true);
    private JButton edit = Ui.button("Edit", false);
    private JButton delete = Ui.button("Delete", false);
    private JButton back = Ui.button("Back", false);
    // records เก็บรายการต้นทาง; DataTable แปลงแถวที่เลือกหลังกรองกลับเป็น index ใน
    // records
    private Sale[] records = new Sale[0];

    /**
     * สร้างหน้า Sales ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public SalesPanel(AppFrame frame, ErpService service) {
        this.frame = frame;
        this.service = service;

        Ui.modulePage(this, "Sales",
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
            Sale selected = selected();
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
            records = service.sales();
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                Sale item = records[i];
                rows[i] = new String[] {
                        item.getId(),
                        item.getDate(),
                        item.getCustomer(),
                        item.getProduct(),
                        "" + item.getQuantity(),
                        Values.moneyText(item.getUnitPriceSatang()),
                        Values.moneyText(item.total())
                };
            }
            table.setRows(rows);
            status.setText(records.length + " records - Select a row to edit or delete.");
        } catch (Exception error) {
            records = new Sale[0];
            table.setRows(new String[0][]);
            status.setText("Data could not be loaded.");
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * อ่าน index ของข้อมูลเดิมจาก DataTable แล้วคืนรายการใน records
     * ถ้ายังไม่เลือกให้แสดงข้อความและคืน null
     */
    private Sale selected() {
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
    private void editRecord(Sale existing) {
        ProductPicker product;
        try {
            product = new ProductPicker(service.products());
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
            return;
        }

        JTextField date = new JTextField();
        PartnerPicker customer;
        try {
            customer = new PartnerPicker(service.partners(true));
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
            return;
        }
        JTextField quantity = new JTextField("1");
        JTextField unitPrice = new JTextField("0.00");
        String id = "";
        String dialogTitle = "Add Sales";

        if (existing != null) {
            id = existing.getId();
            date.setText(existing.getDate());
            customer.selectName(existing.getCustomer());
            product.selectProduct(existing.getProductId());
            quantity.setText("" + existing.getQuantity());
            unitPrice.setText(Values.moneyText(existing.getUnitPriceSatang()));
            dialogTitle = "Edit Sales";
        }

        String[] labels = { "Date (YYYY-MM-DD)", "Customer", "Product", "Quantity", "Unit price" };
        JComponent[] inputs = { date, customer, product, quantity, unitPrice };

        unitPrice.putClientProperty("quantityField", quantity);
        quantity.putClientProperty("stockPicker", product);
        quantity.putClientProperty("oldProduct", existing == null ? "" : existing.getProductId());
        quantity.putClientProperty("oldQuantity", existing == null ? 0 : existing.getQuantity());
        EntryForm form = new EntryForm(dialogTitle, labels, inputs);
        // Save ผ่านการตรวจฟอร์มจึงเข้าลูป;
        // ถ้าบันทึกไม่ผ่านให้แสดงข้อผิดพลาดแล้วเปิดฟอร์มเดิมใหม่
        while (form.showDialog(frame)) {
            try {
                Sale value = new Sale(id, date.getText(), customer.selectedName(),
                        product.getProductName(), Values.quantity(quantity.getText(), 1),
                        Values.money(unitPrice.getText()), product.getProductId());
                service.saveSale(value);

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
        Sale item = selected();
        if (item == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete " + item.getId() + "?",
                "Delete record", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.deleteSale(item.getId());
            reload();
            status.setText("Record deleted.");
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }
}

package erp.ui;

import erp.model.Role;
import erp.service.AuthService;
import java.awt.event.*;
import javax.swing.*;

/**
 * หน้าจอให้ Admin ตั้งชื่อ Role และเลือกโมดูลที่อนุญาต
 * ส่งให้ AuthService ตรวจสิทธิ์และความสัมพันธ์ก่อนบันทึกหรือลบ
 */
public class RolePanel extends JPanel implements ActionListener {
    private AppFrame frame;
    private AuthService auth;
    private Role[] records = new Role[0];
    private DataTable table = new DataTable(new String[] { "ID", "Role name", "Allowed modules" }, new int[] {});
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add Role", true);
    private JButton edit = Ui.button("Edit", false);
    private JButton delete = Ui.button("Delete", false);
    private JButton back = Ui.button("Back", false);

    /**
     * สร้างหน้า Role ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public RolePanel(AppFrame frame, AuthService auth) {
        this.frame = frame;
        this.auth = auth;
        Ui.modulePage(this, "Roles", "One role can grant several modules. Users can have several roles.",
                table, status, add, edit, delete, back);
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
            records = auth.roles();
            String[][] rows = new String[records.length][3];
            for (int i = 0; i < records.length; i++) {
                rows[i] = new String[] { records[i].getId(), records[i].getName(),
                        records[i].getModules().replace('|', '/') };
            }
            table.setRows(rows);
            status.setText(records.length + " roles. Assigned roles cannot be deleted.");
        } catch (Exception error) {
            records = new Role[0];
            table.setRows(new String[0][]);
            status.setText("Roles could not be loaded.");
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
            editRole(null);
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select a role first.");
            return;
        }
        Role selected = records[row];
        if (event.getSource() == edit) {
            editRole(selected);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete role " + selected.getName() + "?", "Delete role",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            auth.deleteRole(selected.getId());
            reload();
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
        }
    }

    /**
     * เปิดฟอร์มชื่อ Role และ checkbox โมดูล พร้อมข้อมูลตรวจชื่อซ้ำ
     * Save แล้วส่ง Role ให้ AuthService จากนั้นโหลดตารางและเมนูใหม่
     */
    private void editRole(Role existing) {
        JTextField name = new JTextField(existing == null ? "" : existing.getName());
        ChoiceList choices = new ChoiceList(Role.MODULES, Role.MODULES, existing == null ? "" : existing.getModules());
        String[] duplicates = new String[records.length];
        for (int i = 0; i < records.length; i++) {
            duplicates[i] = existing != null && records[i].getId().equals(existing.getId()) ? "" : records[i].getName();
        }
        name.putClientProperty("duplicates", duplicates);
        EntryForm form = new EntryForm(existing == null ? "Add Role" : "Edit Role",
                new String[] { "Role name", "Allowed modules" }, new JComponent[] { name, choices });
        while (form.showDialog(frame)) {
            try {
                auth.saveRole(new Role(existing == null ? "" : existing.getId(), name.getText(), choices.selected()));
                reload();
                frame.refreshNavigation();
                return;
            } catch (Exception error) {
                JOptionPane.showMessageDialog(this, error.getMessage());
            }
        }
    }
}

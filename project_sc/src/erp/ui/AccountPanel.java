package erp.ui;

import erp.model.*;
import erp.service.AuthService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.*;

/**
 * หน้าจอจัดการบัญชีผู้ใช้สำหรับ Admin
 * รับข้อมูลและ Role จากฟอร์ม แล้วส่งให้ AuthService ตรวจเงื่อนไขก่อนบันทึก
 */
public class AccountPanel extends JPanel implements ActionListener {

    private AppFrame frame;
    private AuthService auth;
    private DataTable table = new DataTable(
            new String[] { "ID", "Username", "Display name", "Type", "Role IDs" },
            new int[] {});
    private JLabel status = new JLabel(" ");
    private JButton add = Ui.button("Add", true);
    private JButton edit = Ui.button("Edit", false);
    private JButton delete = Ui.button("Delete", false);
    private JButton back = Ui.button("Back", false);
    // records เก็บรายการต้นทาง; DataTable.getSelectedRow() คืน index
    // ต้นทางแม้ตารางถูกกรอง ไม่ใช่ ID
    private UserAccount[] records = new UserAccount[0];

    /**
     * สร้างหน้า Account ผูกปุ่มกับ ActionListener และโหลดข้อมูลครั้งแรก
     */
    public AccountPanel(AppFrame frame, AuthService auth) {
        this.frame = frame;
        this.auth = auth;

        Ui.modulePage(this, "Administration",
                "ADMIN sees all modules. USER sees only modules granted by selected roles.",
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
            UserAccount selected = selected();
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
            records = auth.users();
            String[][] rows = new String[records.length][];
            for (int i = 0; i < records.length; i++) {
                UserAccount item = records[i];
                rows[i] = new String[] {
                        item.getId(),
                        item.getUsername(),
                        item.getDisplayName(),
                        item.getAccountType(),
                        item.getRoleIds()
                };
            }
            table.setRows(rows);
            status.setText(records.length + " records - Select a row to edit or delete.");
        } catch (Exception error) {
            records = new UserAccount[0];
            table.setRows(new String[0][]);
            status.setText("Data could not be loaded.");
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * อ่าน index ของข้อมูลเดิมจาก DataTable แล้วคืนรายการใน records
     * ถ้ายังไม่เลือกให้แสดงข้อความและคืน null
     */
    private UserAccount selected() {
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
     * ตอนแก้ไข ถ้า password ว่างจะใช้รหัสเดิม ไม่ใช่เปลี่ยนให้เป็นรหัสว่าง
     */
    private void editRecord(UserAccount existing) {
        Role[] roles;
        try {
            roles = auth.roles();
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
            return;
        }
        String[] ids = new String[roles.length];
        String[] captions = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            ids[i] = roles[i].getId();
            captions[i] = roles[i].getName() + " (" + roles[i].getModules() + ")";
        }
        ChoiceList choices = new ChoiceList(ids, captions, existing == null ? "" : existing.getRoleIds());
        JComboBox<String> accountType = new JComboBox<String>(new String[] { "USER", "ADMIN" });
        accountType.setSelectedItem(existing == null ? "USER" : existing.getAccountType());
        JTextField username = new JTextField();
        JPasswordField password = new JPasswordField();
        JTextField displayName = new JTextField();
        String id = "";
        String dialogTitle = "Add Account";
        String[] labels = { "Username", "Password", "Display name", "Account type",
                "Roles (USER only; ADMIN has full access)" };

        if (existing != null) {
            id = existing.getId();
            username.setText(existing.getUsername());
            displayName.setText(existing.getDisplayName());
            labels[1] = "Password (blank = keep current)";
            dialogTitle = "Edit Account";
        }

        choices.putClientProperty("accountType", accountType);
        JComponent[] inputs = { username, password, displayName, accountType, choices };

        String[] duplicates = new String[records.length];
        for (int i = 0; i < records.length; i++) {
            duplicates[i] = existing != null && records[i].getId().equals(existing.getId()) ? ""
                    : records[i].getUsername();
        }
        username.putClientProperty("duplicates", duplicates);
        EntryForm form = new EntryForm(dialogTitle, labels, inputs);
        // เข้าลูปเมื่อ Save ผ่านการตรวจฟอร์ม; ถ้า Model/Service
        // โยนข้อผิดพลาดที่จับไว้จะวนเปิดใหม่
        while (form.showDialog(frame)) {
            try {
                char[] entered = password.getPassword();
                String passwordValue = new String(entered);
                for (int i = 0; i < entered.length; i++) {
                    entered[i] = '\0';
                }
                // ช่อง password ว่างตอนแก้ไขหมายถึงใช้รหัสเดิม ไม่ใช่บันทึกรหัสเป็นค่าว่าง
                if (passwordValue.isEmpty() && existing != null) {
                    passwordValue = existing.getPassword();
                }
                UserAccount value = new UserAccount(id, username.getText(), passwordValue,
                        displayName.getText(), (String) accountType.getSelectedItem(),
                        "ADMIN".equals(accountType.getSelectedItem()) ? "" : choices.selected());
                auth.saveUser(value);
                frame.refreshNavigation();

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
        UserAccount item = selected();
        if (item == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete " + item.getId() + "?",
                "Delete record", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            auth.deleteUser(item.getId());
            reload();
            status.setText("Record deleted.");
        } catch (IOException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Please check", JOptionPane.ERROR_MESSAGE);
        }
    }
}

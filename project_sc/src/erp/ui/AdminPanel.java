package erp.ui;

import erp.service.AuthService;
import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import javax.swing.event.*;

/**
 * หน้าจอ Administration รวมแท็บ Accounts และ Roles
 * ตรวจสิทธิ์ก่อนสร้างหน้า และโหลดข้อมูลของแท็บที่ผู้ใช้เลือกใหม่
 */
public class AdminPanel extends JPanel implements ChangeListener {
    private AppFrame frame;
    private AuthService auth;
    private JTabbedPane tabs = new JTabbedPane();

    /**
     * ตรวจสิทธิ์ Administration แล้วสร้างแท็บ Accounts และ Roles
     * รับ frame และ auth เพื่อส่งต่อให้หน้าลูก; parameter data
     */
    public AdminPanel(AppFrame frame, AuthService auth, java.io.File data) throws IOException {
        this.frame = frame; 
        this.auth = auth;
        auth.require("Administration");
        setLayout(new BorderLayout());
        tabs.addTab("Accounts", new AccountPanel(frame, auth));
        tabs.addTab("Roles", new RolePanel(frame, auth));
        // tabs.addTab("Backup & Restore", new BackupPanel(frame, auth, data));
        tabs.setFont(new Font("Tahoma", Font.BOLD, 13));
        tabs.addChangeListener(this);
        add(tabs, BorderLayout.CENTER);
    }

    /**
     * เมื่อเปลี่ยนแท็บ ให้อ่านข้อมูลใหม่เฉพาะหน้า Accounts หรือ Roles ที่กำลังแสดง
     */
    public void stateChanged(ChangeEvent event) {
        Component selected = tabs.getSelectedComponent();
        if (selected instanceof AccountPanel) {
            ((AccountPanel) selected).reload();
        } else if (selected instanceof RolePanel) {
            ((RolePanel) selected).reload();
        }
    }
}

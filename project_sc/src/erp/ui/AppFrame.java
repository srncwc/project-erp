package erp.ui;

import erp.model.UserAccount;
import erp.service.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * หน้าต่างหลักของ ERP สร้าง AuthService และ ErpService ที่หน้าจอต่าง ๆ
 * ใช้ร่วมกัน
 * ควบคุมการ Login, Logout และเลือกหน้าให้ AppShell แสดง
 */
public class AppFrame extends JFrame {
    private java.io.File data;
    private AuthService auth;
    private ErpService service;
    private AppShell shell;

    /**
     * สร้างหน้าต่าง บริการตรวจบัญชี บริการธุรกิจ และ AppShell
     * ส่ง AuthService ตัวเดียวกันให้ส่วนที่ต้องตรวจผู้ใช้ แล้วเปิดหน้า Login
     */
    public AppFrame(File data) {
        super("ERP");
        this.data = data;
        auth = new AuthService(data);
        service = new ErpService(data, auth);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 800);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);
        shell = new AppShell(this);
        setContentPane(shell);
        showLogin();
    }

    /**
     * ล้าง signedInId ผ่าน auth.logout() ซ่อนเมนู และสร้าง LoginPanel ใหม่
     */
    public void showLogin() {
        auth.logout();
        shell.setAccount(null);
        shell.showPage(new LoginPanel(this, auth));
    }

    /**
     * เลือกหน้าแรกหลัง Login สำเร็จ: ADMIN ไป Overview ส่วน USER ไป Main Menu
     */
    public void signedIn(UserAccount user) {
        if (user.isAdmin()) {
            openModule("Overview");
        } else {
            showMenu();
        }
    }

    /**
     * ถาม AuthService ว่าผู้ใช้เข้าโมดูลได้หรือไม่
     * หากตรวจสิทธิ์เกิดข้อผิดพลาด ให้คืน false
     */
    public boolean canAccess(String module) {
        try {
            return auth.canAccess(module);
        } catch (Exception error) {
            return false;
        }
    }

    /**
     * อ่านบัญชีล่าสุดเพื่อแสดงชื่อและประเภทบัญชี แล้วอัปเดตเมนูตามสิทธิ์
     */
    public void refreshNavigation() throws IOException {
        UserAccount user = auth.currentUser();
        shell.setAccount(user.getDisplayName() + " / " + user.getAccountType());
        shell.refreshNavigation();
    }

    /**
     * อัปเดตชื่อบัญชีและเมนู แล้วเปิด MainMenuPanel
     * หากทำไม่สำเร็จให้แจ้งข้อความและกลับหน้า Login
     */
    public void showMenu() {
        try {
            refreshNavigation();
            shell.selectNavigation("Main Menu");
            shell.showPage(new MainMenuPanel(this, auth.currentUser().getDisplayName()));
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage());
            showLogin();
        }
    }

    /**
     * ตรวจสิทธิ์ก่อนสร้างหน้าที่เลือก แล้วให้ AppShell แสดงหน้าและเน้นปุ่มเมนู
     * Sales/Procurement ใช้ BusinessPanel; ถ้าเปิดไม่สำเร็จให้แจ้งข้อความแล้วเรียก
     * showMenu()
     */
    public void openModule(String name) {
        try {
            auth.require(name);
            refreshNavigation();
            JPanel page;
            if ("Overview".equals(name)) {
                page = new OverviewPanel(this, service);
            } else if ("Sales".equals(name)) {
                page = new BusinessPanel(this, service, true);
            } else if ("Procurement".equals(name)) {
                page = new BusinessPanel(this, service, false);
            } else if ("Warehouse".equals(name)) {
                page = new WarehousePanel(this, service);
            } else if ("Finance".equals(name)) {
                page = new FinancePanel(this, service);
            } else if ("Administration".equals(name)) {
                page = new AdminPanel(this, auth, data);
            } else {
                throw new IllegalArgumentException("Unknown module.");
            }
            shell.showPage(page);
            shell.selectNavigation(name);
        } catch (Exception error) {
            JOptionPane.showMessageDialog(this, error.getMessage(), "Cannot open module", JOptionPane.ERROR_MESSAGE);
            showMenu();
        }
    }
}

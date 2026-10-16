package erp.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * โครงหน้าจอหลัก ประกอบด้วยแถบชื่อบัญชี เมนูด้านซ้าย และพื้นที่แสดงหน้า
 * รับการคลิกเมนูแล้วส่งให้ AppFrame ตรวจสิทธิ์และเปลี่ยนหน้า
 */
public class AppShell extends JPanel implements ActionListener {
    private final AppFrame frame;
    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel sidebar = new JPanel(new BorderLayout());
    private final JPanel links = new JPanel(new GridLayout(0, 1));
    private final JLabel account = Ui.label(" ", 13, false, Color.WHITE);
    private final String[] names = { "Main Menu", "Overview", "Sales", "Procurement", "Warehouse", "Finance",
            "Administration" };
    private final JButton[] navigation = new JButton[names.length];
    private final JButton logout = Ui.button("Logout", false);

    /**
     * สร้างแถบชื่อบัญชี เมนูด้านซ้าย และพื้นที่เนื้อหา พร้อมผูกเหตุการณ์ปุ่ม
     */
    public AppShell(AppFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Ui.ORANGE);
        header.setBorder(BorderFactory.createEmptyBorder(17, 24, 17, 24));
        header.add(Ui.label("ERP", 20, true, Color.WHITE), BorderLayout.WEST);
        header.add(account, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        sidebar.setPreferredSize(new Dimension(195, 100));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, Ui.ORANGE));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel heading = Ui.label("WORKSPACE", 11, true, Ui.MUTED);
        heading.setBorder(BorderFactory.createEmptyBorder(22, 22, 16, 0));
        top.add(heading, BorderLayout.NORTH);
        links.setOpaque(false);
        for (int i = 0; i < names.length; i++) {
            navigation[i] = Ui.button(names[i], false);
            navigation[i].setHorizontalAlignment(SwingConstants.LEFT);
            navigation[i].addActionListener(this);
            links.add(navigation[i]);
        }
        top.add(links, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.NORTH);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(14, 16, 20, 16));
        bottom.add(logout, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);
        logout.addActionListener(this);
        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        selectNavigation("Main Menu");
    }

    /**
     * นำหน้าเดิมออกจากพื้นที่ content แล้วใส่ JPanel ที่ได้รับ
     * จัด layout และวาดหน้าจอใหม่ โดยไม่ได้บันทึกข้อมูล
     */
    public void showPage(JPanel page) {
        content.removeAll();
        content.add(page, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    /**
     * แสดงชื่อบัญชี; ถ้า text เป็น null ให้ล้างข้อความและซ่อน sidebar
     * การเรียก method นี้อย่างเดียวไม่ได้ Logout ใน AuthService
     */
    public void setAccount(String text) {
        account.setText(text == null ? " " : text);
        sidebar.setVisible(text != null);
    }

    /**
     * ตรวจสิทธิ์ของแต่ละเมนูผ่าน AppFrame แล้วแสดงเฉพาะเมนูที่อนุญาต
     */
    public void refreshNavigation() {
        links.removeAll();
        for (int i = 0; i < names.length; i++) {
            boolean allowed = frame.canAccess(names[i]);
            navigation[i].setVisible(allowed);
            if (allowed) {
                links.add(navigation[i]);
            }
        }
        sidebar.revalidate();
        sidebar.repaint();
    }

    /**
     * ปรับสี ตัวหนา และขอบของปุ่มที่ชื่อเมนูตรงกัน
     * เป็นการเน้นปุ่มเท่านั้น ไม่ได้เปิดหน้าหรือเปลี่ยนสิทธิ์
     */
    public void selectNavigation(String name) {
        for (int i = 0; i < navigation.length; i++) {
            boolean selected = names[i].equals(name);
            navigation[i].setBackground(selected ? Ui.SOFT : Color.WHITE);
            navigation[i].setForeground(selected ? Ui.DARK_ORANGE : Ui.INK);
            navigation[i].setFont(new Font("Tahoma", selected ? Font.BOLD : Font.PLAIN, 13));
            navigation[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 0, 0, selected ? Ui.ORANGE : Color.WHITE),
                    BorderFactory.createEmptyBorder(14, 19, 14, 10)));
        }
    }

    /**
     * แยกปุ่ม Logout, Main Menu และโมดูลอื่น แล้วให้ AppFrame ทำงานต่อ
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == logout) {
            frame.showLogin();
        } else if (event.getActionCommand().equals("Main Menu")) {
            frame.showMenu();
        } else {
            frame.openModule(event.getActionCommand());
        }
    }
}

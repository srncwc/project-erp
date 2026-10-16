package erp.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * แสดงเมนูโมดูลที่บัญชีปัจจุบันมีสิทธิ์เข้าใช้งาน
 * เมื่อผู้ใช้เลือกเมนู ส่งชื่อโมดูลให้ AppFrame เปิดหน้า
 */
public class MainMenuPanel extends JPanel implements ActionListener {
    private AppFrame frame;

    /**
     * สร้างเมนูตามสิทธิ์ผู้ใช้และแสดง displayName ที่รับมา พร้อมผูกเหตุการณ์ปุ่ม
     */
    public MainMenuPanel(AppFrame frame, String displayName) {
        this.frame = frame;
        setLayout(new BorderLayout(0, 26));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));
        JPanel heading = new JPanel(new GridLayout(3, 1, 0, 8));
        heading.setOpaque(false);
        heading.add(Ui.label("Main Menu", 26, true, Ui.INK));
        heading.add(Ui.label("Welcome, " + displayName, 13, false, Ui.DARK_ORANGE));
        heading.add(Ui.label("Choose a module to view, add, edit or delete records.", 13, false, Ui.MUTED));
        add(heading, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 16, 16));
        grid.setOpaque(false);
        String[] names = { "Overview", "Sales", "Procurement", "Warehouse", "Finance", "Administration", "Logout" };
        String[] descriptions = { "All departments at a glance", "Customer sales", "Supplier purchases",
                "Stock quantities",
                "Income and expenses", "User accounts", "Return to Login" };
        for (int i = 0; i < names.length; i++) {
            if (!"Logout".equals(names[i]) && !frame.canAccess(names[i])) {
                continue;
            }
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createLineBorder(Ui.LINE));
            JButton button = Ui.button(names[i], false);
            button.setFont(new Font("Tahoma", Font.PLAIN, 22));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setBorder(BorderFactory.createEmptyBorder(18, 26, 8, 26));
            button.addActionListener(this);
            card.add(button, BorderLayout.CENTER);
            JLabel description = Ui.label(descriptions[i], 13, false, Ui.MUTED);
            description.setBorder(BorderFactory.createEmptyBorder(0, 26, 24, 26));
            card.add(description, BorderLayout.SOUTH);
            grid.add(card);
        }
        // กำหนดความสูงเมนูเพื่อไม่ให้กล่องยืดเต็มหน้าจอเมื่อมีโมดูลที่เข้าได้เพียงรายการเดียว
        int rows = (grid.getComponentCount() + 1) / 2;
        grid.setPreferredSize(new Dimension(800, rows * 160));
        JPanel cards = new JPanel(new BorderLayout());
        cards.setBackground(Color.WHITE);
        cards.add(grid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
        add(Ui.label("Modules are shown according to your assigned roles.", 12, false, Ui.MUTED), BorderLayout.SOUTH);
    }

    /**
     * ถ้า action command เป็น Logout ให้ AppFrame กลับหน้า Login
     * ค่าอื่นส่งเป็นชื่อโมดูลให้ AppFrame ตรวจสิทธิ์และเปิดหน้า
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals("Logout")) {
            frame.showLogin();
        } else {
            frame.openModule(event.getActionCommand());
        }
    }
}

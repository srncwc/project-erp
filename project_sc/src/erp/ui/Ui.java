package erp.ui;

import java.awt.*;
import javax.swing.*;

/**
 * รวมสีและตัวช่วยจัดหน้าตาของ label ปุ่ม ช่องกรอก และหน้าโมดูล
 * จัดรูปแบบการแสดงผล ไม่ตรวจสิทธิ์หรือบันทึกข้อมูล
 */
public class Ui {
    public static final Color ORANGE = new Color(204, 115, 68);
    public static final Color DARK_ORANGE = new Color(149, 69, 31);
    public static final Color INK = new Color(34, 38, 46);
    public static final Color MUTED = new Color(115, 113, 111);
    public static final Color LINE = new Color(220, 201, 189);
    public static final Color CREAM = new Color(243, 227, 217);
    public static final Color SOFT = new Color(251, 238, 230);
    public static final Color BACKGROUND = new Color(247, 245, 243);

    /**
     * สร้าง JLabel พร้อมกำหนดข้อความ font ขนาด ความหนา และสี
     */
    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Tahoma", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    /**
     * สร้างปุ่มตามรูปแบบร่วมของระบบ; primary เลือกรูปแบบปุ่มหลัก
     * ยังไม่ผูก ActionListener ให้ปุ่ม
     */
    public static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Tahoma", Font.BOLD, 13));
        button.setBackground(primary ? ORANGE : Color.WHITE);
        button.setForeground(primary ? Color.WHITE : DARK_ORANGE);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ORANGE : LINE),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)));
        return button;
    }

    /**
     * กำหนด font สี ขนาด และขอบของช่องกรอก ไม่ตรวจค่าหรือเปลี่ยนสิทธิ์
     */
    public static void input(JComponent input) {
        input.setFont(new Font("Tahoma", Font.PLAIN, 14));
        input.setBackground(Color.WHITE);
        input.setForeground(INK);
        input.setPreferredSize(new Dimension(360, 38));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)));
    }

    /**
     * จัดหัวข้อ ตาราง ข้อความสถานะ และปุ่มของหน้าโมดูล
     * แบบมี extra จะแสดงปุ่มเพิ่มเติมด้วย; ผู้เรียกต้องผูกเหตุการณ์ปุ่มเอง
     */
    public static void modulePage(JPanel page, String title, String description,
            DataTable table, JLabel status, JButton add, JButton edit, JButton delete, JButton back) {
        modulePage(page, title, description, table, status, add, edit, delete, back, null);
    }

    /**
     * จัดหัวข้อ ตาราง ข้อความสถานะ และปุ่มของหน้าโมดูล
     * แบบมี extra จะแสดงปุ่มเพิ่มเติมด้วย; ผู้เรียกต้องผูกเหตุการณ์ปุ่มเอง
     */
    public static void modulePage(JPanel page, String title, String description,
            DataTable table, JLabel status, JButton add, JButton edit, JButton delete, JButton back, JButton extra) {
        page.setLayout(new BorderLayout(0, 24));
        page.setBackground(Color.WHITE);
        page.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));

        JPanel heading = new JPanel(new BorderLayout(16, 0));
        heading.setOpaque(false);
        JPanel words = new JPanel(new GridLayout(2, 1, 0, 12));
        words.setOpaque(false);
        words.add(label(title, 26, true, INK));
        words.add(label(description, 12, false, MUTED));
        heading.add(words, BorderLayout.CENTER);
        JPanel addArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        addArea.setOpaque(false);
        addArea.add(add);
        heading.add(addArea, BorderLayout.EAST);
        page.add(heading, BorderLayout.NORTH);
        page.add(table, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 20));
        footer.setOpaque(false);
        status.setFont(new Font("Tahoma", Font.PLAIN, 12));
        status.setForeground(MUTED);
        footer.add(status, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(edit);
        actions.add(delete);
        if (extra != null) {
            actions.add(extra);
        }
        footer.add(actions, BorderLayout.WEST);
        footer.add(back, BorderLayout.EAST);
        page.add(footer, BorderLayout.SOUTH);
    }
}

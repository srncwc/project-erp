package erp.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * สร้างฟอร์มร่วมจากชื่อช่องและ component ที่ส่งมา พร้อมปุ่ม Save/Cancel
 * ให้ FieldCheck ตรวจช่องกรอก; showDialog คืนผลหลังปิด dialog แล้วผู้เรียกจึงนำค่าไปบันทึก
 */
public class EntryForm extends JPanel implements ActionListener {
    private JButton save = Ui.button("Save", true);
    private JButton cancel = Ui.button("Cancel", false);
    private JDialog dialog;
    private String title;
    // บันทึกว่าผู้ใช้กด "Save" จริงหรือไม่ (true = กด Save, false = กด Cancel
    // หรือปิดหน้าต่างทิ้ง)
    private boolean accepted;
    // ตัวช่วยสำหรับเช็กความถูกต้องของข้อมูลในแต่ละช่อง
    private FieldCheck[] checks;

    /**
     * สร้างช่องตาม labels และ inputs ที่ต้องเรียงลำดับตรงกัน พร้อม FieldCheck ของแต่ละช่อง
     * ผูกปุ่ม Save/Cancel และตรวจสถานะปุ่มครั้งแรก แต่ยังไม่เปิด dialog
     */
    public EntryForm(String title, String[] labels, JComponent[] inputs) {
        this.title = title;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        JLabel heading = Ui.label(title, 18, true, Ui.INK);
        heading.setOpaque(true);
        heading.setBackground(Ui.CREAM);
        heading.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setBackground(Color.WHITE);
        fields.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        checks = new FieldCheck[labels.length];
        for (int i = 0; i < labels.length; i++) {
            JPanel row = new JPanel(new BorderLayout(0, 6));
            row.setOpaque(false);
            row.add(Ui.label(labels[i], 12, false, Ui.MUTED), BorderLayout.NORTH);
            if (!(inputs[i] instanceof ChoiceList)) {
                Ui.input(inputs[i]);
            }
            row.add(inputs[i], BorderLayout.CENTER);
            JLabel feedback = Ui.label(" ", 11, false, Ui.MUTED);
            feedback.setPreferredSize(new Dimension(440, 30));
            row.add(feedback, BorderLayout.SOUTH);
            checks[i] = new FieldCheck(inputs[i], labels[i], feedback, this);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            fields.add(row);
            fields.add(Box.createVerticalStrut(15));
        }
        JScrollPane scroll = new JScrollPane(fields);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setPreferredSize(new Dimension(500, Math.min(fields.getPreferredSize().height + 2, 580)));
        add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(0, 14, 20, 14));
        actions.add(cancel);
        actions.add(save);
        add(actions, BorderLayout.SOUTH);
        save.addActionListener(this);
        cancel.addActionListener(this);
        updateFields(false);
    }

    /**
     * เรียก validate() ของทุกช่อง แล้วเปิด Save เฉพาะเมื่อผ่านทั้งหมด
     * showAll เป็น true แสดงทุกข้อผิดพลาด; false ใช้ touched หรือข้อความที่มีในช่องช่วยตัดสิน
     * คืน true เมื่อทุกช่องผ่าน ไม่ได้บันทึกข้อมูล
     */
    public boolean updateFields(boolean showAll) {
        boolean valid = true;
        if (checks == null) {
            return false;
        }
        for (int i = 0; i < checks.length; i++) {
            if (checks[i] != null && !checks[i].validate(showAll)) {
                valid = false;
            }
        }
        save.setEnabled(valid);
        return valid;
    }

    /**
     * สร้างและแสดง modal dialog; method นี้คืนค่าหลัง dialog ปิด แต่ Swing ยังรับเหตุการณ์ใน dialog ได้
     * คืน true เมื่อกด Save และตรวจผ่าน; false เมื่อ Cancel หรือปิดหน้าต่าง
     */
    public boolean showDialog(JFrame owner) {
        accepted = false;
        dialog = new JDialog(owner, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        dialog.setContentPane(this);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return accepted;
    }

    /**
     * ถ้ากด Save ให้ตรวจทุกช่องก่อน จากนั้นตั้ง accepted และปิด dialog
     * การปิดทำให้ showDialog() คืนค่าให้ผู้เรียกนำไปบันทึกต่อ
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == save && !updateFields(true)) {
            return;
        }
        accepted = event.getSource() == save;
        // ปิดหน้าต่าง เพื่อให้โค้ดที่ "หยุดรอ" อยู่ก่อนหน้านี้ ทำงานต่อ
        dialog.dispose();
    }
}

package erp.ui;

import erp.model.Role;
import java.awt.*;
import javax.swing.*;

/**
 * ชุด checkbox สำหรับเลือก Role หรือโมดูลได้หลายรายการ
 * จับคู่ ID กับข้อความที่แสดง และคืน ID ที่เลือกคั่นด้วย |
 */
public class ChoiceList extends JPanel {
    private String[] ids;
    private JCheckBox[] boxes;

    /**
     * สร้าง checkbox จาก IDs และข้อความที่จับคู่กัน พร้อมเลือกค่าจาก selected
     * ที่คั่นด้วย |
     */
    public ChoiceList(String[] ids, String[] captions, String selected) {
        this.ids = ids;
        boxes = new JCheckBox[ids.length];
        setLayout(new GridLayout(0, 1, 0, 4));
        setBackground(Color.WHITE);
        for (int i = 0; i < ids.length; i++) {
            boxes[i] = new JCheckBox(captions[i], Role.contains(selected, ids[i]));
            boxes[i].setFont(new Font("Tahoma", Font.PLAIN, 13));
            boxes[i].setBackground(Color.WHITE);
            add(boxes[i]);
        }
        if (ids.length == 0) {
            add(Ui.label("Create a role in the Roles tab first.", 12, false, Ui.MUTED));
        }
        setPreferredSize(new Dimension(420, Math.max(34, ids.length * 28)));
    }

    /**
     * ผูก listener ที่รับมาให้ checkbox
     * ทุกตัวเพื่อให้ฟอร์มรู้เมื่อผู้ใช้เปลี่ยนตัวเลือก
     */
    public void addActionListener(java.awt.event.ActionListener listener) {
        for (int i = 0; i < boxes.length; i++) {
            boxes[i].addActionListener(listener);
        }
    }

    /**
     * คืน IDs ของ checkbox ที่เลือกคั่นด้วย |; ถ้าไม่เลือกเลยคืนข้อความว่าง
     */
    public String selected() {
        String result = "";
        for (int i = 0; i < ids.length; i++) {
            if (boxes[i].isSelected()) {
                result += (result.length() == 0 ? "" : "|") + ids[i];
            }
        }
        return result;
    }
}

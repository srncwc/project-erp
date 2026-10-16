package erp.ui;

import erp.model.Partner;
import javax.swing.*;

/**
 * ตัวเลือกชื่อคู่ค้าจาก Partner[] ที่ได้รับ
 * คืนชื่อที่เลือกเพื่อนำไปสร้างเอกสารซื้อขาย
 */
public class PartnerPicker extends JComboBox<String> {
    /**
     * สร้างตัวเลือกจากชื่อคู่ค้าที่ได้รับ ไม่อ่านไฟล์เอง
     */
    public PartnerPicker(Partner[] records) {
        for (int i = 0; i < records.length; i++) {
            addItem(records[i].getName());
        }
        setSelectedIndex(-1);
    }

    /**
     * กำหนดชื่อที่ต้องการเลือกในรายการ เช่น ชื่อจากเอกสารเดิมตอนแก้ไข
     */
    public void selectName(String name) {
        setSelectedItem(name);
    }

    /**
     * คืนชื่อที่เลือก หากยังไม่ได้เลือกให้โยนข้อผิดพลาด
     */
    public String selectedName() {
        if (getSelectedIndex() < 0) {
            throw new IllegalArgumentException("Select a partner. Add one in the Customers / Suppliers tab first.");
        }
        return (String) getSelectedItem();
    }
}

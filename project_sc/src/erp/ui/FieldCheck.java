package erp.ui;

import erp.model.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.Border;

/**
 * ตรวจค่าของช่องกรอกตามชื่อช่องและข้อมูลประกอบจาก client property
 * ตรวจซ้ำเมื่อข้อความหรือตัวเลือกเปลี่ยน แล้วให้ EntryForm อัปเดตปุ่ม Save
 * แสดงข้อผิดพลาดเมื่อ showAll เป็น true, เคยโต้ตอบกับช่อง
 */
public class FieldCheck implements DocumentListener, FocusListener, ActionListener {
    private JComponent input;
    private String name;
    private JLabel message;
    private EntryForm form;
    private Border border;
    private boolean touched;

    /**
     * เก็บช่องกรอก ชื่อช่อง ป้ายข้อความ และฟอร์มแม่ พร้อมผูก listener ตามชนิด
     * component
     */
    public FieldCheck(JComponent input, String name, JLabel message, EntryForm form) {
        this.input = input;
        this.name = name;
        this.message = message;
        this.form = form;
        border = input.getBorder();
        input.getAccessibleContext().setAccessibleName(name);
        if (input instanceof JTextField) {
            ((JTextField) input).getDocument().addDocumentListener(this);
        }
        if (input instanceof JComboBox) {
            ((JComboBox<?>) input).addActionListener(this);
        }
        if (input instanceof ChoiceList) {
            ((ChoiceList) input).addActionListener(this);
        }
        input.addFocusListener(this);
    }

    /**
     * เลือกความยาวสูงสุดตามชื่อช่อง เช่น Username 30 และ Password 64
     * ชื่อที่ไม่ตรงเงื่อนไขใช้ค่าเริ่มต้น 100
     */
    private int maxLength() {
        if (name.startsWith("Username") || name.equals("Product code")) {
            return 30;
        }
        if (name.startsWith("Password")) {
            return 64;
        }
        if (name.equals("Unit")) {
            return 20;
        }
        if (name.equals("Role name")) {
            return 60;
        }
        if (name.equals("Description") || name.equals("Note") || name.equals("Reason")) {
            return 200;
        }
        if (name.equals("Contact")) {
            return 120;
        }
        return 100;
    }

    /**
     * คืนข้อความคำแนะนำตามชื่อและชนิดช่อง ยังไม่ได้ตรวจค่าหรือบันทึกข้อมูล
     */
    private String hint() {
        if (name.startsWith("Date") || name.equals("Payment date")) {
            return "YYYY-MM-DD; actual calendar date (year 0001..9999).";
        }
        if (name.toLowerCase().contains("quantity")) {
            return "Whole number; maximum " + Rules.MAX_QUANTITY + ".";
        }
        if (name.startsWith("Unit price") || name.startsWith("Unit cost") || name.contains("Amount")) {
            return "Up to 999999999.99; at most 2 decimal places.";
        }
        if (name.startsWith("Password")) {
            return name.contains("blank") ? "Blank keeps current password; new password 8..64 characters."
                    : "8..64 characters; no comma / double quote / line break.";
        }
        if (input instanceof JTextField) {
            return "Maximum " + maxLength() + " characters; no comma / double quote / line break.";
        }
        return "Select a value.";
    }

    /**
     * ตรวจตามชนิดช่องและกฎจาก Rules รวมค่าประกอบ เช่น สต็อก วันที่เอกสาร
     * และยอดรวมจำนวนคูณราคา
     * ข้าม component ที่ปิดใช้งานหรือช่องข้อความที่แก้ไม่ได้; ถ้าผิดให้โยน
     * IllegalArgumentException
     */
    private void checkValue() {
        if (!input.isEnabled()) {
            return;
        }
        if (input instanceof JTextField) {
            JTextField text = (JTextField) input;
            if (!text.isEditable()) {
                return;
            }
            String value = text.getText();
            if (name.startsWith("Date") || name.equals("Payment date")) {
                Rules.date(value);
                String documentDate = (String) input.getClientProperty("documentDate");
                if (documentDate != null) {
                    Rules.paymentDate(value, documentDate);
                }
            } else if (name.toLowerCase().contains("quantity")) {
                int min = name.startsWith("Current") ? 0 : 1;
                long max = Rules.MAX_QUANTITY;
                ProductPicker picker = (ProductPicker) input.getClientProperty("stockPicker");
                if (picker != null) {
                    max = picker.getQuantity();
                    if (picker.getProductId().equals(input.getClientProperty("oldProduct"))) {
                        max += (Integer) input.getClientProperty("oldQuantity");
                    }
                }
                ProductPicker purchasePicker = (ProductPicker) input.getClientProperty("purchasePicker");
                if (purchasePicker != null) {
                    max = (long) Integer.MAX_VALUE - purchasePicker.getQuantity();
                    if (purchasePicker.getProductId().equals(input.getClientProperty("oldProduct"))) {
                        max += (Integer) input.getClientProperty("oldQuantity");
                    }
                }
                Rules.quantity(value, min, max);
            } else if (name.startsWith("Unit price") || name.startsWith("Unit cost") || name.contains("Amount")) {
                long min = name.startsWith("Unit") ? 0 : 1;
                long max = Rules.MAX_MONEY;
                Long boundary = (Long) input.getClientProperty("maxMoney");
                if (boundary != null) {
                    max = Math.min(max, boundary);
                }
                JTextField quantity = (JTextField) input.getClientProperty("quantityField");
                if (quantity != null && quantity.getText().trim().length() > 0) {
                    int count = Rules.quantity(quantity.getText(), 1, Rules.MAX_QUANTITY);
                    max = Rules.MAX_MONEY / count;
                }
                Rules.money(value, min, max);
            } else {
                boolean optional = name.equals("Contact") || name.equals("Note") || name.contains("blank");
                Rules.text(value, name, maxLength(), optional);
                if (name.startsWith("Password") && value.length() > 0 && value.length() < 8) {
                    throw new IllegalArgumentException("New password must have 8..64 characters.");
                }
                String[] duplicates = (String[]) input.getClientProperty("duplicates");
                if (duplicates != null) {
                    for (int i = 0; i < duplicates.length; i++) {
                        if (value.trim().equalsIgnoreCase(duplicates[i])) {
                            throw new IllegalArgumentException(name + " already exists.");
                        }
                    }
                }
            }
        } else if (input instanceof JComboBox) {
            if (((JComboBox<?>) input).getSelectedIndex() < 0) {
                throw new IllegalArgumentException("Select " + name + ".");
            }
        } else if (input instanceof ChoiceList) {
            JComboBox<?> type = (JComboBox<?>) input.getClientProperty("accountType");
            if (type != null && "ADMIN".equals(type.getSelectedItem())) {
                return;
            }
            if (((ChoiceList) input).selected().length() == 0) {
                throw new IllegalArgumentException("Select at least one role / module.");
            }
        }
    }

    /**
     * เรียก checkValue() แล้วจับข้อผิดพลาดมาแสดงใต้ช่องและเปลี่ยนขอบ
     * แสดง error เมื่อ showAll หรือ touched เป็น true หรือช่องข้อความไม่ว่าง
     * แม้ยังไม่แสดง error ก็คืน false ถ้าค่าไม่ผ่าน เพื่อให้ฟอร์มปิดปุ่ม Save
     */
    public boolean validate(boolean showAll) {
        String error = "";
        try {
            checkValue();
        } catch (IllegalArgumentException failure) {
            error = failure.getMessage();
        }
        boolean nonempty = input instanceof JTextField && ((JTextField) input).getText().length() > 0;
        boolean show = error.length() > 0 && (showAll || touched || nonempty);
        message.setForeground(show ? new Color(170, 35, 25) : Ui.MUTED);
        String text = show ? error : hint();
        if (input instanceof JTextField && !((JTextField) input).isEditable()) {
            text = "Read only — calculated or linked data.";
        }
        message.setText("<html>" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</html>");
        input.setBorder(
                show ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180, 45, 35)),
                        BorderFactory.createEmptyBorder(6, 9, 6, 9)) : border);
        input.setToolTipText(text);
        return error.length() == 0;
    }

    /**
     * จำว่าผู้ใช้โต้ตอบกับช่องนี้แล้ว และสั่งตรวจทั้งฟอร์มอีกครั้ง
     * เพื่อให้ช่องที่สัมพันธ์กัน เช่น จำนวนและราคา ปรับผลตามกัน
     */
    private void changed() {
        touched = true;
        form.updateFields(false);
    }

    /**
     * เมื่อพิมพ์หรือวางข้อความ ให้เรียก changed() เพื่อตรวจทั้งฟอร์ม
     */
    public void insertUpdate(DocumentEvent e) {
        changed();
    }

    /**
     * เมื่อลบข้อความ ให้เรียก changed() เพื่อตรวจทั้งฟอร์ม
     */
    public void removeUpdate(DocumentEvent e) {
        changed();
    }

    /**
     * รับเหตุการณ์เปลี่ยนคุณลักษณะข้อความ แต่ไม่มีคำสั่งเพิ่มเติมในรุ่นนี้
     */
    public void changedUpdate(DocumentEvent e) {

    }

    /**
     * เมื่อย้ายออกจากช่อง ให้เรียก changed() เพื่อตรวจและแสดงผลของช่องที่โต้ตอบแล้ว
     */
    public void focusLost(FocusEvent e) {
        changed();
    }

    /**
     * ไม่มีคำสั่งเพิ่มเติมเมื่อย้ายเข้ามาที่ช่อง
     */
    public void focusGained(FocusEvent e) {
    }

    /**
     * เมื่อตัวเลือกหรือ checkbox เปลี่ยน ให้เรียก changed() เพื่อตรวจทั้งฟอร์ม
     */
    public void actionPerformed(ActionEvent e) {
        changed();
    }
}

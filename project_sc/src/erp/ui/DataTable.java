package erp.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import erp.model.Rules;

/**
 * ตารางที่สร้างจาก JPanel และ JButton พร้อมค้นหาข้อความและกรองช่วงวันที่
 * ใช้ mapping แปลงแถวหลังกรองกลับเป็น index ของข้อมูลเดิม; ไม่บันทึก CSV เอง
 */
public class DataTable extends JPanel implements ActionListener, DocumentListener {
    private String[] columns;
    private int[] numberColumns;
    private JPanel rows = new JPanel(new GridLayout(0, 1));
    private JButton[][] cells = new JButton[0][0];
    private int selectedRow = -1;
    private String[][] data = new String[0][];
    private int[] mapping = new int[0];
    private JTextField search = new JTextField(14);
    private JTextField from = new JTextField(8);
    private JTextField to = new JTextField(8);
    private JLabel filterMessage = Ui.label(" ", 11, false, Ui.MUTED);
    private int dateColumn = -1;

    /**
     * สร้างหัวตาราง พื้นที่เลื่อน และช่องค้นหา
     * ถ้ามีคอลัมน์ชื่อ Date ให้แสดงช่องกรองวันที่; numberColumns
     * ระบุคอลัมน์ตัวเลขที่จัดชิดขวา
     */
    public DataTable(String[] columns, int[] numberColumns) {
        this.columns = columns;
        this.numberColumns = numberColumns;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Ui.LINE));
        JPanel heading = new JPanel(new GridLayout(1, columns.length));
        heading.setBackground(Ui.CREAM);
        heading.setPreferredSize(new Dimension(Math.max(800, columns.length * 110), 44));
        for (int i = 0; i < columns.length; i++) {
            JLabel label = Ui.label(columns[i], 12, true, Ui.INK);
            label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            heading.add(label);
        }
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(rows, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setColumnHeaderView(heading);
        add(scroll, BorderLayout.CENTER);
        JPanel filters = new JPanel(new BorderLayout(0, 3));
        filters.setBackground(Color.WHITE);
        JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        fields.setOpaque(false);
        fields.add(new JLabel("Search"));
        fields.add(search);
        search.getAccessibleContext().setAccessibleName("Search all columns");
        for (int i = 0; i < columns.length; i++) {
            if ("Date".equals(columns[i])) {
                dateColumn = i;
            }
        }
        if (dateColumn >= 0) {
            fields.add(new JLabel("From"));
            fields.add(from);
            fields.add(new JLabel("To"));
            fields.add(to);
            from.setToolTipText("YYYY-MM-DD; blank = no start date");
            to.setToolTipText("YYYY-MM-DD; blank = no end date");
            from.getAccessibleContext().setAccessibleName("From date");
            to.getAccessibleContext().setAccessibleName("To date");
        }
        filters.add(fields, BorderLayout.NORTH);
        filters.add(filterMessage, BorderLayout.SOUTH);
        add(filters, BorderLayout.NORTH);
        search.getDocument().addDocumentListener(this);
        from.getDocument().addDocumentListener(this);
        to.getDocument().addDocumentListener(this);
    }

    /**
     * เก็บข้อมูลชุดใหม่แล้วเรียก applyFilter() โดยยังใช้คำค้นและช่วงวันที่ปัจจุบัน
     * การวาดใหม่จะล้างแถวที่เลือกเดิม
     */
    public void setRows(String[][] data) {
        this.data = data;
        applyFilter();
    }

    /**
     * ตรวจคำค้นและวันที่ แล้วกรองข้อมูลเฉพาะที่จะแสดงโดยไม่แก้ข้อมูลต้นทาง
     * สร้าง mapping จับคู่แถวที่เห็นกับ index เดิม;
     * ถ้าตัวกรองผิดให้แสดงข้อความและไม่มีแถว
     */
    private void applyFilter() {
        String error = "";
        try {
            if (search.getText().length() > 100) {
                throw new IllegalArgumentException("Search: maximum 100 characters.");
            }
            if (from.getText().length() > 0) {
                Rules.date(from.getText());
            }
            if (to.getText().length() > 0) {
                Rules.date(to.getText());
            }
            if (from.getText().length() > 0 && to.getText().length() > 0
                    && from.getText().compareTo(to.getText()) > 0) {
                throw new IllegalArgumentException("From date must not be after To date.");
            }
        } catch (IllegalArgumentException invalid) {
            error = invalid.getMessage();
        }
        int[] indices = new int[data.length];
        int count = 0;
        if (error.length() == 0) {
            for (int i = 0; i < data.length; i++) {
                String text = "";
                for (int j = 0; j < data[i].length; j++) {
                    text += " " + data[i][j];
                }
                boolean match = text.toLowerCase(java.util.Locale.ROOT)
                        .contains(search.getText().trim().toLowerCase(java.util.Locale.ROOT));
                if (dateColumn >= 0) {
                    String date = data[i][dateColumn];
                    if (from.getText().length() > 0 && date.compareTo(from.getText()) < 0) {
                        match = false;
                    }
                    if (to.getText().length() > 0 && date.compareTo(to.getText()) > 0) {
                        match = false;
                    }
                }
                if (match) {
                    indices[count++] = i;
                }
            }
        }
        mapping = new int[count];
        String[][] visible = new String[count][];
        for (int i = 0; i < count; i++) {
            mapping[i] = indices[i];
            visible[i] = data[indices[i]];
        }
        filterMessage.setText(error.length() == 0 ? count + " / " + data.length + " records" : error);
        filterMessage.setForeground(error.length() == 0 ? Ui.MUTED : Color.RED);
        renderRows(visible);
    }

    /**
     * กรองตารางใหม่เมื่อพิมพ์หรือวางข้อความค้นหาหรือวันที่
     */
    public void insertUpdate(DocumentEvent e) {
        applyFilter();
    }

    /**
     * กรองตารางใหม่เมื่อลบข้อความค้นหาหรือวันที่
     */
    public void removeUpdate(DocumentEvent e) {
        applyFilter();
    }

    /**
     * กรองตารางใหม่เมื่อได้รับเหตุการณ์เปลี่ยนคุณลักษณะของ document
     */
    public void changedUpdate(DocumentEvent e) {
        applyFilter();
    }

    /**
     * สร้างปุ่ม cell สำหรับแถวที่ผ่านตัวกรอง แล้วล้างสถานะการเลือกแถว
     */
    private void renderRows(String[][] data) {
        selectedRow = -1;
        rows.removeAll();
        cells = new JButton[data.length][columns.length];
        for (int i = 0; i < data.length; i++) {
            JPanel row = new JPanel(new GridLayout(1, columns.length));
            row.setPreferredSize(new Dimension(Math.max(800, columns.length * 110), 43));
            for (int j = 0; j < columns.length; j++) {
                JButton cell = new JButton(data[i][j]);
                cell.setToolTipText(data[i][j]);
                cell.setFont(new Font("Tahoma", Font.PLAIN, 13));
                cell.setForeground(Ui.INK);
                cell.setBackground(Color.WHITE);
                cell.setContentAreaFilled(false);
                cell.setOpaque(true);
                cell.setHorizontalAlignment(SwingConstants.LEFT);
                for (int k = 0; k < numberColumns.length; k++) {
                    if (j == numberColumns[k]) {
                        cell.setHorizontalAlignment(SwingConstants.RIGHT);
                    }
                }
                cell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(238, 238, 238)),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)));
                // ทุก cell ในแถวเดียวกันส่ง index ของแถวที่แสดง ส่วน mapping ใช้แปลงกลับเป็น
                // index ข้อมูลเดิม
                cell.setActionCommand("" + i);
                cell.addActionListener(this);
                cells[i][j] = cell;
                row.add(cell);
            }
            rows.add(row);
        }
        rows.revalidate();
        rows.repaint();
    }

    /**
     * คืน index ของข้อมูลเดิมผ่าน mapping ไม่ใช่ลำดับแถวที่มองเห็นหลังกรอง
     * เช่น แถวแรกหลังกรองอาจชี้ไป records[5]; คืน -1 เมื่อยังไม่เลือก
     */
    public int getSelectedRow() {
        return selectedRow < 0 ? -1 : mapping[selectedRow];
    }

    /**
     * รับ index ของแถวที่แสดงจากปุ่ม cell แล้วเน้นสีแถวนั้น
     * เมื่อผู้เรียกขอ getSelectedRow() จะค่อยแปลงกลับเป็น index ข้อมูลเดิม
     */
    public void actionPerformed(ActionEvent event) {
        selectedRow = Integer.parseInt(event.getActionCommand());
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                cells[i][j].setBackground(i == selectedRow ? Ui.SOFT : Color.WHITE);
            }
        }
    }
}

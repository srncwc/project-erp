package erp.ui;

import erp.model.UserAccount;
import erp.service.AuthService;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.border.EmptyBorder;

/**
 * รับ username และ password แล้วส่งให้ AuthService ตรวจบัญชี
 * ตรวจช่องกรอกเมื่อข้อความเปลี่ยน และส่งผู้ใช้ที่ Login สำเร็จให้ AppFrame
 * เลือกหน้าถัดไป
 */
public class LoginPanel extends JPanel implements ActionListener, DocumentListener, FocusListener {

    private AppFrame frame;
    private AuthService auth;
    private JTextField username = new JTextField(22);
    private JPasswordField password = new JPasswordField(22);
    private JButton login = Ui.button("Login", true);
    private JLabel message = new JLabel(" ");

    /**
     * สร้างช่องบัญชีและรหัสผ่าน พร้อมผูกเหตุการณ์ปุ่มและการเปลี่ยนข้อความ
     * เรียก checkBoundary(false) เพื่อกำหนดสถานะเริ่มต้นของปุ่ม Login
     */
    public LoginPanel(AppFrame frame, AuthService auth) {
        this.frame = frame;
        this.auth = auth;

        setLayout(new GridBagLayout());
        setBackground(Ui.BACKGROUND);

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Ui.LINE), new EmptyBorder(34, 36, 30, 36)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        addLine(card, Ui.label("ERP", 11, true, Ui.DARK_ORANGE), 12);
        addLine(card, Ui.label("Login", 29, true, Ui.INK), 8);
        addLine(card, Ui.label("Sign in to your account", 13, false, Ui.MUTED), 26);

        Ui.input(username);
        Ui.input(password);
        addLine(card, Ui.label("Username", 12, false, Ui.MUTED), 6);
        addLine(card, username, 17);
        addLine(card, Ui.label("Password", 12, false, Ui.MUTED), 6);
        addLine(card, password, 22);
        addLine(card, login, 14);

        message.setForeground(new Color(184, 58, 45));
        addLine(card, message, 16);
        add(card);

        username.getAccessibleContext().setAccessibleName("Username");
        password.getAccessibleContext().setAccessibleName("Password");
        login.addActionListener(this);
        username.getDocument().addDocumentListener(this);
        password.getDocument().addDocumentListener(this);
        username.addFocusListener(this);
        password.addFocusListener(this);
        username.setToolTipText("1-30 characters");
        password.setToolTipText("1-64 characters");
        checkBoundary(false);
    }

    /**
     * ตรวจช่องกรอกแล้วส่งบัญชีให้ AuthService.login()
     * สำเร็จ: ล้างฟอร์มแล้วให้ AppFrame เลือก Overview หรือ Main Menu
     * ตามประเภทบัญชี
     * ใน finally ล้างเฉพาะ char[] entered; ไม่ได้ล้าง String
     * ที่สร้างจากรหัสผ่านทั้งหมด
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == login) {
            if (!checkBoundary(true)) {
                return;
            }
            char[] entered = password.getPassword();
            try {
                UserAccount user = auth.login(username.getText(), new String(entered));
                if (user == null) {
                    message.setText("Incorrect username or password.");
                    password.selectAll();
                    password.requestFocusInWindow();
                } else {
                    clear();
                    frame.signedIn(user);
                }
            } catch (Exception error) {
                message.setText("Cannot read users.csv. Check the data folder.");
            } finally {
                for (int i = 0; i < entered.length; i++) {
                    entered[i] = '\0';
                }
            }
        }
    }

    /**
     * ตรวจความยาว username/password และเปิดปุ่มเฉพาะเมื่อกรอกครบ
     * required เป็น false: ไม่แสดงคำเตือนช่องว่าง แต่ยังคืน false
     * และปิดปุ่มเมื่อกรอกไม่ครบ
     * required เป็น true: แสดงคำเตือนช่องว่างด้วย;
     * ไม่ได้ตรวจว่าบัญชีตรงกับไฟล์หรือไม่
     */
    private boolean checkBoundary(boolean required) {
        int size = password.getPassword().length;
        String error = username.getText().length() > 30 ? "Username: maximum 30 characters."
                : size > 64 ? "Password: maximum 64 characters."
                        : required && (username.getText().trim().length() == 0 || size == 0)
                                ? "Enter username and password."
                                : " ";
        message.setText(error);
        boolean valid = error.equals(" ") && username.getText().trim().length() > 0 && size > 0;
        login.setEnabled(valid);
        return valid;
    }

    /**
     * เมื่อพิมพ์หรือวางข้อความ ให้ตรวจฟอร์มและปรับสถานะปุ่ม Login
     */
    public void insertUpdate(DocumentEvent e) {
        checkBoundary(false);
    }

    /**
     * เมื่อลบข้อความ ให้ตรวจฟอร์มและปรับสถานะปุ่ม Login
     */
    public void removeUpdate(DocumentEvent e) {
        checkBoundary(false);
    }

    
    public void changedUpdate(DocumentEvent e) {

    }

    
    public void focusLost(FocusEvent e) {

    }

    
    public void focusGained(FocusEvent e) {
    }

    /**
     * เพิ่ม component ลง panel และเว้นระยะด้านล่าง
     * ช่องข้อความและปุ่มจะจำกัดความสูงไว้ 38 พิกเซล
     */
    private static void addLine(JPanel panel, JComponent component, int gap) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (component instanceof JTextField || component instanceof JButton) {
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        }
        panel.add(component);
        panel.add(Box.createVerticalStrut(gap));
    }

    /**
     * คืนปุ่ม Login ตัวเดิมให้ผู้เรียกนำไปใช้งาน เช่น ตั้งเป็น default button
     */
    public JButton loginButton() {
        return login;
    }

    /**
     * ล้าง username, password และข้อความแจ้งเตือนบนฟอร์ม
     * ไม่ล้าง signedInId และไม่ทำให้ Logout
     */
    public void clear() {
        username.setText("");
        password.setText("");
        message.setText(" ");
    }
}

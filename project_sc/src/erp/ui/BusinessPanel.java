package erp.ui;

import erp.service.ErpService;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * รวมแท็บรายการซื้อขายและทะเบียนคู่ค้าไว้ในโมดูลเดียว
 * sales เป็น true ใช้ Sales/Customers; false ใช้ Procurement/Suppliers
 */
public class BusinessPanel extends JPanel implements ChangeListener {
    private JTabbedPane tabs = new JTabbedPane();

    /**
     * สร้างแท็บ Transactions และ Customers เมื่อ sales เป็น true
     * ถ้า false ใช้รายการซื้อและ Suppliers แล้วผูก listener ตอนสลับแท็บ
     */
    public BusinessPanel(AppFrame frame, ErpService service, boolean sales) {
        setLayout(new BorderLayout());
        tabs.addTab("Transactions", sales ? new SalesPanel(frame, service) : new ProcurementPanel(frame, service));
        tabs.addTab(sales ? "Customers" : "Suppliers", new PartnerPanel(frame, service, sales));
        tabs.addChangeListener(this);
        add(tabs);
    }

    /**
     * อ่านข้อมูลใหม่เฉพาะ SalesPanel, ProcurementPanel หรือ PartnerPanel
     * ที่ถูกเลือก
     */
    public void stateChanged(ChangeEvent event) {
        Component page = tabs.getSelectedComponent();
        if (page instanceof SalesPanel) {
            ((SalesPanel) page).reload();
        } else if (page instanceof ProcurementPanel) {
            ((ProcurementPanel) page).reload();
        } else if (page instanceof PartnerPanel) {
            ((PartnerPanel) page).reload();
        }
    }
}

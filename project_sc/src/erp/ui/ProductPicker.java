package erp.ui;

import erp.model.StockItem;
import javax.swing.*;

/**
 * ตัวเลือกสินค้าที่เก็บ StockItem[] ไว้จับคู่กับตำแหน่งที่เลือก
 * แสดงรหัส ชื่อ และจำนวนจากข้อมูลที่ได้รับ; Service จะอ่านตรวจอีกครั้งตอนบันทึก
 */
public class ProductPicker extends JComboBox<String> {
    private StockItem[] products;

    /**
     * เก็บสินค้าและสร้างรายการให้เลือก โดยแสดงรหัส ชื่อ และยอดคงเหลือที่รับมา
     */
    public ProductPicker(StockItem[] products) {
        this.products = products;
        for (int i = 0; i < products.length; i++) {
            addItem(products[i].getCode() + " - " + products[i].getName()
                    + " (" + products[i].getQuantity() + " " + products[i].getUnit() + ")");
        }
    }

    /**
     * ค้นหาสินค้าจาก ID แล้วเลือกตำแหน่งนั้น ถ้าไม่พบให้ยกเลิกการเลือก
     */
    public void selectProduct(String id) {
        for (int i = 0; i < products.length; i++) {
            if (products[i].getId().equals(id)) {
                setSelectedIndex(i);
                return;
            }
        }
        setSelectedIndex(-1);
    }

    /**
     * คืน StockItem ที่ตำแหน่งเลือก หากไม่เลือกให้โยนข้อผิดพลาด
     */
    private StockItem selectedProduct() {
        if (getSelectedIndex() < 0) {
            throw new IllegalArgumentException("Select a product. Create products in Warehouse first.");
        }
        return products[getSelectedIndex()];
    }

    /**
     * คืนจำนวนจากสินค้าที่เลือกในชุดข้อมูลของตัวเลือก ไม่ได้อ่าน CSV ใหม่
     */
    public int getQuantity() {
        return selectedProduct().getQuantity();
    }

    /**
     * คืน ID ของสินค้าที่เลือกเพื่อเชื่อมกับทะเบียน Warehouse
     */
    public String getProductId() {
        return selectedProduct().getId();
    }

    /**
     * คืนชื่อสินค้าที่เลือกจากข้อมูลที่เก็บในตัวเลือก
     */
    public String getProductName() {
        return selectedProduct().getName();
    }
}

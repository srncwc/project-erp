package erp.service;

import erp.model.*;
import erp.repository.*;
import java.io.*;

/**
 * ตรวจบัญชีตอน Login และจำผู้ใช้ปัจจุบันด้วย signedInId
 * ตรวจสิทธิ์จากบัญชีและ Role ล่าสุด รวมถึงจัดการบัญชีและ Role สำหรับ Admin
 */
public class AuthService {
    private UserRepository users;
    private RoleRepository roles;
    private String signedInId = "";

    /**
     * รับโฟลเดอร์ข้อมูลจากผู้เรียก แล้วสร้าง UserRepository และ RoleRepository
     * ใน flow เปิดโปรแกรม AppFrame เป็นผู้ส่ง data เข้ามา
     */
    public AuthService(File data) {
        users = new UserRepository(data);
        roles = new RoleRepository(data);
    }

    /**
     * ล้างสถานะเดิม อ่านบัญชี แล้วเทียบ username แบบไม่แยกตัวพิมพ์กับ password
     * ที่ต้องตรงทุกตัว
     * ถ้าพบให้จำ signedInId และคืน UserAccount; ถ้าไม่พบคืน null
     */
    public UserAccount login(String username, String password) throws IOException {
        signedInId = "";
        UserAccount[] all = users.findAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getUsername().equalsIgnoreCase(username.trim()) && all[i].getPassword().equals(password)) {
                signedInId = all[i].getId();
                return all[i];
            }
        }
        return null;
    }

    /**
     * ล้าง ID ผู้ใช้ที่จำไว้ในหน่วยความจำ ไม่ลบบัญชีออกจาก users.csv
     */
    public void logout() {
        signedInId = "";
    }

    /**
     * อ่าน users.csv ใหม่แล้วคืนบัญชีที่ ID ตรงกับ signedInId
     * ถ้าไม่ได้ Login, Logout แล้ว หรือไม่พบบัญชี ให้โยนข้อผิดพลาด;
     * ไม่มีระบบหมดอายุตามเวลา
     */
    public UserAccount currentUser() throws IOException {
        UserAccount[] all = users.findAll();
        for (int i = 0; i < all.length; i++) {
            if (all[i].getId().equals(signedInId)) {
                return all[i];
            }
        }
        throw new IllegalArgumentException("Please log in again.");
    }

    /**
     * ตรวจสิทธิ์โมดูลที่ระบบรู้จักจากบัญชีปัจจุบัน
     * ADMIN เข้าได้ทุกโมดูลที่รองรับ; USER ใช้ Role และเข้า Overview/Administration
     * ไม่ได้
     */
    public boolean canAccess(String module) throws IOException {
        UserAccount user = currentUser();
        if ("Main Menu".equals(module)) {
            return true;
        }
        if (!"Overview".equals(module) && !"Administration".equals(module)
                && !Role.contains("Sales|Procurement|Warehouse|Finance", module)) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        if ("Overview".equals(module) || "Administration".equals(module)) {
            return false;
        }
        Role[] all = roles.findAll();
        for (int i = 0; i < all.length; i++) {
            if (Role.contains(user.getRoleIds(), all[i].getId()) && Role.contains(all[i].getModules(), module)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ตรวจสิทธิ์ด้วย canAccess(); หากไม่ผ่านให้โยน IllegalArgumentException
     * เพื่อหยุดงานของผู้เรียก
     */
    public void require(String module) throws IOException {
        if (!canAccess(module)) {
            throw new IllegalArgumentException("Access denied: " + module);
        }
    }

    /**
     * ตรวจสิทธิ์ Administration แล้วคืนบัญชีทั้งหมดเป็น UserAccount[]
     */
    public UserAccount[] users() throws IOException {
        require("Administration");
        return users.findAll();
    }

    /**
     * ตรวจสิทธิ์ Administration แล้วคืน Role ทั้งหมดเป็น Role[]
     */
    public Role[] roles() throws IOException {
        require("Administration");
        return roles.findAll();
    }

    /**
     * ตรวจสิทธิ์ ข้อมูลบัญชี และ Role ก่อนเพิ่มหรือแก้ผู้ใช้
     * รหัสผ่านใหม่ทั้งตอนเพิ่มและตอนเปลี่ยนต้องมี 8–64 ตัว;
     * รหัสเดิมที่ไม่เปลี่ยนใช้ต่อได้
     * ห้ามลดสิทธิ์ Admin ของตนเอง และต้องมี Admin เหลืออย่างน้อยหนึ่งบัญชี
     */
    public void saveUser(UserAccount value) throws IOException {
        require("Administration");
        Rules.text(value.getUsername(), "Username", 30, false);
        Rules.text(value.getDisplayName(), "Display name", 100, false);
        boolean oldPassword = false;
        UserAccount[] existing = users.findAll();
        for (int i = 0; i < existing.length; i++) {
            if (existing[i].getId().equals(value.getId()) && existing[i].getPassword().equals(value.getPassword())) {
                oldPassword = true;
            }
        }
        if (!oldPassword) {
            Rules.text(value.getPassword(), "Password", 64, false);
            if (value.getPassword().length() < 8) {
                throw new IllegalArgumentException("New password must have 8..64 characters.");
            }
        }
        if (value.getId().equals(signedInId) && !value.isAdmin()) {
            throw new IllegalArgumentException("You cannot remove your own ADMIN access.");
        }
        if (!value.isAdmin() && value.getRoleIds().length() == 0) {
            throw new IllegalArgumentException("Select at least one role for a USER.");
        }
        String[] chosen = value.getRoleIds().split("\\|", -1);
        Role[] allRoles = roles.findAll();
        for (int i = 0; i < chosen.length; i++) {
            if (chosen[i].length() == 0 && value.isAdmin()) {
                continue;
            }
            boolean found = false;
            for (int j = 0; j < allRoles.length; j++) {
                if (chosen[i].equals(allRoles[j].getId())) {
                    found = true;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Unknown role. Reopen the form.");
            }
        }
        if (!value.isAdmin()) {
            keepAdmin(value.getId());
        }
        if (value.getId().length() == 0) {
            users.add(value);
        } else {
            users.update(value);
        }
    }

    /**
     * ตรวจสิทธิ์และห้ามลบบัญชีที่กำลัง Login
     * ตรวจว่ามี Admin อื่นเหลือก่อนส่ง ID ไปให้ Repository ลบ
     */
    public void deleteUser(String id) throws IOException {
        require("Administration");
        if (signedInId.equals(id)) {
            throw new IllegalArgumentException("You cannot delete your signed-in account.");
        }
        keepAdmin(id);
        users.delete(id);
    }

    /**
     * ค้นหา Admin ที่มี ID ต่างจากบัญชีที่จะลบหรือลดสิทธิ์
     * ถ้าไม่พบ ให้หยุดด้วยข้อผิดพลาด
     */
    private void keepAdmin(String excludedId) throws IOException {
        UserAccount[] all = users.findAll();
        for (int i = 0; i < all.length; i++) {
            if (!all[i].getId().equals(excludedId) && all[i].isAdmin()) {
                return;
            }
        }
        throw new IllegalArgumentException("Keep at least one ADMIN account.");
    }

    /**
     * ตรวจสิทธิ์ Administration และความยาวชื่อ แล้วให้ Repository เพิ่มหรือแก้ Role
     * การตรวจสิทธิ์ครั้งถัดไปจะอ่าน Role ที่บันทึกใหม่นี้
     */
    public void saveRole(Role role) throws IOException {
        require("Administration");
        Rules.text(role.getName(), "Role name", 60, false);
        roles.save(role);
    }

    /**
     * ตรวจสิทธิ์และอ่านบัญชีทั้งหมดก่อนลบ Role
     * หากยังมีบัญชีอ้างอิง Role นี้ ให้ผู้ใช้แก้บัญชีก่อน
     */
    public void deleteRole(String id) throws IOException {
        require("Administration");
        UserAccount[] all = users.findAll();
        for (int i = 0; i < all.length; i++) {
            if (Role.contains(all[i].getRoleIds(), id)) {
                throw new IllegalArgumentException(
                        "Role is assigned to " + all[i].getUsername() + ". Edit that account first.");
            }
        }
        roles.delete(id);
    }
}

package lk.nsbm.university.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lk.nsbm.university.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private static final String RECOVERY_CODE_REGEX = "^[0-9]{6}$";

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/login/guardian")
    public String guardianLoginPage() {
        return "auth/login-guardian";
    }

    @GetMapping("/login/shuttle")
    public String shuttleLoginPage() {
        return "auth/login-shuttle";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(form.getStudentId())) {
            bindingResult.rejectValue("studentId", "", "Student ID is required");
        }
        validateRecoveryCodeField(form.getRecoveryCode(), bindingResult);
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerUser(form.getStudentId(), form.getPassword(), form.getRecoveryCode());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration submitted. Await moderator approval before login.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            if (!applyRecoveryCodeError(bindingResult, ex.getMessage())) {
                bindingResult.rejectValue("studentId", "", ex.getMessage());
            }
            return "auth/register";
        }
    }

    @GetMapping("/register/guardian")
    public String showGuardianRegistrationForm(Model model) {
        model.addAttribute("guardianRegistrationForm", new RegistrationForm());
        return "auth/register-guardian";
    }

    @PostMapping("/register/guardian")
    public String registerGuardian(@Valid @ModelAttribute("guardianRegistrationForm") RegistrationForm form,
                                   BindingResult bindingResult,
                                   RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(form.getFullName())) {
            bindingResult.rejectValue("fullName", "", "Full name is required");
        }
        if (!StringUtils.hasText(form.getContactNumber())) {
            bindingResult.rejectValue("contactNumber", "", "Contact number is required");
        }
        validateRecoveryCodeField(form.getRecoveryCode(), bindingResult);
        if (bindingResult.hasErrors()) {
            return "auth/register-guardian";
        }
        try {
            userService.registerGuardian(null, form.getPassword(), form.getFullName(), form.getContactNumber(), form.getRecoveryCode());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Request submitted. Once approved, sign in with your phone number and password.");
            return "redirect:/login/guardian";
        } catch (IllegalArgumentException ex) {
            if (!applyRecoveryCodeError(bindingResult, ex.getMessage())) {
                bindingResult.rejectValue("contactNumber", "", ex.getMessage());
            }
            return "auth/register-guardian";
        }
    }

    @GetMapping("/register/shuttle")
    public String showShuttleRegistrationForm(Model model) {
        model.addAttribute("shuttleRegistrationForm", new RegistrationForm());
        return "auth/register-shuttle";
    }

    @PostMapping("/register/shuttle")
    public String registerShuttleDriver(@Valid @ModelAttribute("shuttleRegistrationForm") RegistrationForm form,
                                        BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(form.getStudentId())) {
            bindingResult.rejectValue("studentId", "", "Staff ID is required");
        }
        if (!StringUtils.hasText(form.getFullName())) {
            bindingResult.rejectValue("fullName", "", "Full name is required");
        }
        if (!StringUtils.hasText(form.getContactNumber())) {
            bindingResult.rejectValue("contactNumber", "", "Contact number is required");
        }
        validateRecoveryCodeField(form.getRecoveryCode(), bindingResult);
        if (bindingResult.hasErrors()) {
            return "auth/register-shuttle";
        }
        try {
            userService.registerShuttleDriver(
                    form.getStudentId(),
                    form.getPassword(),
                    form.getFullName(),
                    form.getContactNumber(),
                    form.getRecoveryCode());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Request submitted. Logistics approval required before access.");
            return "redirect:/login/shuttle";
        } catch (IllegalArgumentException ex) {
            if (!applyRecoveryCodeError(bindingResult, ex.getMessage())) {
                bindingResult.rejectValue("studentId", "", ex.getMessage());
            }
            return "auth/register-shuttle";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword(Model model) {
        if (!model.containsAttribute("forgotPasswordForm")) {
            model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (!StringUtils.hasText(form.getIdentifier())) {
            bindingResult.rejectValue("identifier", "", "Enter your student ID or phone number");
        }
        validateRecoveryCodeField(form.getRecoveryCode(), bindingResult);
        if (!StringUtils.hasText(form.getNewPassword()) || form.getNewPassword().length() < 8) {
            bindingResult.rejectValue("newPassword", "", "Password must be at least 8 characters long");
        } else if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "", "Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("forgotPasswordForm", form);
            return "auth/forgot-password";
        }
        try {
            userService.resetPasswordWithRecovery(form.getIdentifier(), form.getRecoveryCode(), form.getNewPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Password updated. You can sign in now.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            if (!applyRecoveryCodeError(bindingResult, ex.getMessage())) {
                bindingResult.rejectValue("identifier", "", ex.getMessage());
            }
            model.addAttribute("forgotPasswordForm", form);
            return "auth/forgot-password";
        }
    }

    private void validateRecoveryCodeField(String code, BindingResult bindingResult) {
        if (!StringUtils.hasText(code)) {
            bindingResult.rejectValue("recoveryCode", "", "Recovery code is required");
            return;
        }
        String trimmed = code.trim();
        if (!trimmed.matches(RECOVERY_CODE_REGEX)) {
            bindingResult.rejectValue("recoveryCode", "", "Use the 6-digit recovery code format");
        }
    }

    private boolean applyRecoveryCodeError(BindingResult bindingResult, String message) {
        if (message != null && message.toLowerCase().contains("recovery code")) {
            bindingResult.rejectValue("recoveryCode", "", message);
            return true;
        }
        return false;
    }

    public static class RegistrationForm {
        private String studentId;

        @NotBlank(message = "Password is required")
        private String password;

        private String fullName;

        private String contactNumber;

        private String recoveryCode;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getContactNumber() {
            return contactNumber;
        }

        public void setContactNumber(String contactNumber) {
            this.contactNumber = contactNumber;
        }

        public String getRecoveryCode() {
            return recoveryCode;
        }

        public void setRecoveryCode(String recoveryCode) {
            this.recoveryCode = recoveryCode;
        }
    }

    public static class ForgotPasswordForm {
        private String identifier;
        private String recoveryCode;
        private String newPassword;
        private String confirmPassword;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getRecoveryCode() {
            return recoveryCode;
        }

        public void setRecoveryCode(String recoveryCode) {
            this.recoveryCode = recoveryCode;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}

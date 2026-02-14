package lk.nsbm.university.controller.advice;

import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.UserService;
import lk.nsbm.university.util.AvatarGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@Component
public class NavProfileAdvice {

    private final UserService userService;

    public NavProfileAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("navProfile")
    public NavProfile populateNavProfile(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.getUserByStudentId(principal.getName())
                .map(this::toNavProfile)
                .orElse(null);
    }

    private NavProfile toNavProfile(User user) {
        String displayName = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getStudentId();
        String avatarUrl = StringUtils.hasText(user.getAvatarPath()) ? "/media/" + user.getAvatarPath() : null;
        return new NavProfile(
                displayName,
                avatarUrl,
                AvatarGenerator.initials(user),
                AvatarGenerator.gradient(user),
                prettifyRole(user.getRole())
        );
    }

    private String prettifyRole(Role role) {
        if (role == null) {
            return "";
        }
        return role.getDisplayName();
    }

    public record NavProfile(
            String displayName,
            String avatarUrl,
            String initials,
            String gradient,
            String roleLabel
    ) {
    }
}

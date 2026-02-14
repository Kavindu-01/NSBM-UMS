package lk.nsbm.university.controller;

import lk.nsbm.university.entity.AccountStatus;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.LogisticsAnnouncementService;
import lk.nsbm.university.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/services")
public class StudentLogisticsController {

    private final UserService userService;
    private final LogisticsAnnouncementService logisticsAnnouncementService;

    public StudentLogisticsController(UserService userService,
                                      LogisticsAnnouncementService logisticsAnnouncementService) {
        this.userService = userService;
        this.logisticsAnnouncementService = logisticsAnnouncementService;
    }

    @GetMapping("/shuttles")
    public String viewShuttles(Model model) {
        List<User> drivers = getActiveUsers(Role.SHUTTLE_DRIVER);
        model.addAttribute("profiles", drivers);
        model.addAttribute("headline", "University Shuttle Services");
        model.addAttribute("subhead", "Track pickup hubs, driver updates, and WhatsApp routes in one place.");
        model.addAttribute("profileLabel", "Driver");
        model.addAttribute("shuttleNotices", logisticsAnnouncementService.getRecentShuttleNotices());
        return "services/shuttle-hub";
    }

    @GetMapping("/boarding")
    public String viewBoarding(Model model) {
        List<User> guardians = getActiveUsers(Role.BOARDING_GUARDIAN);
        model.addAttribute("profiles", guardians);
        model.addAttribute("headline", "Boarding & Welfare Network");
        model.addAttribute("subhead", "Browse approved boarding houses, welfare bulletins, and guardian contacts.");
        model.addAttribute("profileLabel", "Guardian");
        model.addAttribute("boardingListings", logisticsAnnouncementService.getRecentBoardingListings());
        return "services/boarding-network";
    }

    @PostMapping("/shuttles/notices")
    public String publishShuttleNotice(@RequestParam(value = "message", required = false) String message,
                                       @RequestParam(value = "runningTimes", required = false) String runningTimes,
                                       @RequestParam(value = "routeDescription", required = false) String routeDescription,
                                       @RequestParam(value = "contactInfo", required = false) String contactInfo,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        try {
            User driver = requireUser(principal);
            if (driver.getRole() != Role.SHUTTLE_DRIVER) {
                throw new AccessDeniedException("Not authorized to post shuttle updates");
            }
            logisticsAnnouncementService.publishShuttleNotice(driver, message, runningTimes, routeDescription, contactInfo);
            redirectAttributes.addFlashAttribute("successMessage", "Shuttle update shared");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/services/shuttles";
    }

    @PostMapping("/boarding/listings")
    public String publishBoardingListing(@RequestParam(value = "headline", required = false) String headline,
                                         @RequestParam(value = "description", required = false) String description,
                                         @RequestParam(value = "location", required = false) String location,
                                         @RequestParam(value = "rentRange", required = false) String rentRange,
                                         @RequestParam(value = "availableSlots", required = false) String availableSlotsValue,
                                         @RequestParam(value = "amenities", required = false) String amenities,
                                         @RequestParam(value = "contactInfo", required = false) String contactInfo,
                                         @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                                         Principal principal,
                                         RedirectAttributes redirectAttributes) {
        try {
            User guardian = requireUser(principal);
            if (guardian.getRole() != Role.BOARDING_GUARDIAN) {
                throw new AccessDeniedException("Not authorized to post boarding listings");
            }
            Integer availableSlots = null;
            if (StringUtils.hasText(availableSlotsValue)) {
                try {
                    availableSlots = Integer.parseInt(availableSlotsValue.trim());
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Slots must be a number");
                }
            }
            logisticsAnnouncementService.publishBoardingListing(guardian, headline, description, location, rentRange, availableSlots, amenities, contactInfo, photos);
            redirectAttributes.addFlashAttribute("successMessage", "Boarding listing published");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/services/boarding";
    }

    private List<User> getActiveUsers(Role role) {
        return userService.getUsersByRole(role).stream()
                .filter(user -> user.getAccountStatus() == AccountStatus.APPROVED)
                .sorted(Comparator.comparing(user -> {
                    String display = user.getFullName();
                    if (display != null && !display.isBlank()) {
                        return display;
                    }
                    return user.getStudentId();
                }, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private User requireUser(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("You must be signed in");
        }
        return userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new AccessDeniedException("Active account not found"));
    }
}

package lk.nsbm.university.controller;

import lk.nsbm.university.dto.SocialReactionSummary;
import lk.nsbm.university.entity.SocialPost;
import lk.nsbm.university.entity.SocialPostComment;
import lk.nsbm.university.entity.SocialReactionType;
import lk.nsbm.university.entity.SocialVisibility;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.service.SocialPostService;
import lk.nsbm.university.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/social")
public class SocialFeedController {

    private final SocialPostService socialPostService;
    private final UserService userService;

    public SocialFeedController(SocialPostService socialPostService,
                                UserService userService) {
        this.socialPostService = socialPostService;
        this.userService = userService;
    }

    @GetMapping({"", "/feed"})
    public String feed(@RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "6") int size,
                       Principal principal,
                       Model model,
                       HttpServletRequest request) {
        User currentUser = requireUser(principal);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 12));
        Page<SocialPost> feedPage = socialPostService.getFeed(currentUser, pageable);
        hydrateFeedModel(model, currentUser, feedPage);
        model.addAttribute("currentFeedPath", resolveCurrentPathOrDefault(request, "/social/feed"));
        model.addAttribute("selectedProfile", currentUser);
        model.addAttribute("pageTitle", "Campus Stream");
        return "social/feed";
    }

    @GetMapping("/profile/{studentId}")
    public String profileFeed(@PathVariable String studentId,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "6") int size,
                               Principal principal,
                               Model model,
                               HttpServletRequest request) {
        User currentUser = requireUser(principal);
        User profileOwner = userService.getUserByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 12));
        Page<SocialPost> feedPage = socialPostService.getProfilePosts(profileOwner, pageable);
        hydrateFeedModel(model, currentUser, feedPage);
        model.addAttribute("currentFeedPath", resolveCurrentPathOrDefault(request, "/social/profile/" + profileOwner.getStudentId()));
        model.addAttribute("selectedProfile", profileOwner);
        model.addAttribute("pageTitle", profileOwner.getStudentId());
        model.addAttribute("isProfileOwner", profileOwner.getId().equals(currentUser.getId()));
        return "social/profile";
    }

    @PostMapping("/posts")
    public String publishPost(@RequestParam("body") String body,
                              @RequestParam(value = "visibility", required = false) String visibilityParam,
                              @RequestParam(value = "semesterId", required = false) Long semesterId,
                              @RequestParam(value = "facultyId", required = false) Long facultyId,
                              @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User currentUser = requireUser(principal);
        try {
            SocialVisibility visibility = parseVisibility(visibilityParam);
            socialPostService.createPost(currentUser, body, visibility, semesterId, facultyId, mediaFiles);
            redirectAttributes.addFlashAttribute("successMessage", "Post shared successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/social/feed";
    }

    @PostMapping("/posts/{postId}/react")
    public String reactToPost(@PathVariable Long postId,
                              @RequestParam(value = "reactionType", required = false) String reactionTypeParam,
                              Principal principal,
                              RedirectAttributes redirectAttributes,
                              @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        User currentUser = requireUser(principal);
        try {
            socialPostService.reactToPost(postId, currentUser, parseReactionType(reactionTypeParam));
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/social/feed");
    }

    @PostMapping("/posts/{postId}/comments")
    public String commentOnPost(@PathVariable Long postId,
                                @RequestParam("body") String body,
                                @RequestParam(value = "parentId", required = false) Long parentId,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        User currentUser = requireUser(principal);
        try {
            socialPostService.addComment(postId, currentUser, body, parentId);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/social/feed");
    }

    @PostMapping("/posts/{postId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long postId,
                                 @PathVariable Long commentId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes,
                                 @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        User currentUser = requireUser(principal);
        try {
            socialPostService.removeComment(commentId, currentUser);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/social/feed");
    }

    @PostMapping("/posts/{postId}/delete")
    public String deletePost(@PathVariable Long postId,
                             Principal principal,
                             RedirectAttributes redirectAttributes,
                             @RequestParam(value = "redirectTo", required = false) String redirectTo) {
        User currentUser = requireUser(principal);
        try {
            socialPostService.deletePost(postId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Post removed");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + (StringUtils.hasText(redirectTo) ? redirectTo : "/social/feed");
    }

    private void hydrateFeedModel(Model model, User currentUser, Page<SocialPost> feedPage) {
        List<Long> postIds = feedPage.getContent().stream().map(SocialPost::getId).toList();
        Map<Long, List<SocialPostComment>> commentMap = socialPostService.getCommentsGrouped(postIds);
        Map<Long, SocialReactionSummary> reactionSummaries = socialPostService.getReactionSummaries(postIds);
        Map<Long, String> userReactions = socialPostService.getUserReactions(postIds, currentUser);
        Set<Long> reactedPostIds = userReactions == null ? Set.of() : Set.copyOf(userReactions.keySet());
        Set<Long> deletablePostIds = feedPage.getContent().stream()
            .filter(post -> socialPostService.canDeletePost(post, currentUser))
            .map(SocialPost::getId)
            .collect(Collectors.toSet());
        model.addAttribute("feedPage", feedPage);
        model.addAttribute("commentMap", commentMap);
        model.addAttribute("reactionSummaries", reactionSummaries);
        model.addAttribute("userReactions", userReactions);
        model.addAttribute("reactedPostIds", reactedPostIds);
        model.addAttribute("deletablePostIds", deletablePostIds);
        model.addAttribute("availableVisibilities", socialPostService.getAvailableVisibilities(currentUser));
    }

    private User requireUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Login required");
        }
        return userService.getUserByStudentId(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private SocialVisibility parseVisibility(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return SocialVisibility.PUBLIC;
        }
        try {
            return SocialVisibility.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SocialVisibility.PUBLIC;
        }
    }

    private SocialReactionType parseReactionType(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return SocialReactionType.LIKE;
        }
        try {
            return SocialReactionType.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SocialReactionType.LIKE;
        }
    }

    private String resolveCurrentPath(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            return uri + "?" + query;
        }
        return uri;
    }

    private String resolveCurrentPathOrDefault(HttpServletRequest request, String defaultValue) {
        String resolved = resolveCurrentPath(request);
        return StringUtils.hasText(resolved) ? resolved : defaultValue;
    }
}

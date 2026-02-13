package lk.nsbm.university.service;

import lk.nsbm.university.entity.Content;
import lk.nsbm.university.entity.ContentCategory;
import lk.nsbm.university.entity.Role;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.ContentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

@Service
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentService(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @Transactional(readOnly = true)
    public Page<Content> getContent(Pageable pageable) {
        Page<Content> page = contentRepository.findAll(pageable);
        hydrateCreators(page);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<Content> getContentByCategory(ContentCategory category, Pageable pageable) {
        Page<Content> page = contentRepository.findByCategory(category, pageable);
        hydrateCreators(page);
        return page;
    }

    @Transactional(readOnly = true)
    public Page<Content> getContentByCategories(Set<ContentCategory> categories, Pageable pageable) {
        if (categories == null || categories.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Content> page = contentRepository.findByCategoryIn(categories, pageable);
        hydrateCreators(page);
        return page;
    }

    @Transactional(readOnly = true)
    public Content getContentById(Long contentId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found"));
        if (content.getCreatedBy() != null) {
            content.getCreatedBy().getStudentId();
        }
        return content;
    }

    @Transactional
    public Content createContent(Content content, User createdBy) {
        enforceCategoryAccess(createdBy, content.getCategory());
        sanitizeContentFields(content);
        content.setCreatedBy(createdBy);
        return contentRepository.save(content);
    }

    @Transactional
    public Content updateContent(Long contentId, Content updatedContent, User performedBy) {
        Content existing = getContentById(contentId);
        enforceCategoryAccess(performedBy, existing.getCategory());
        enforceCategoryAccess(performedBy, updatedContent.getCategory());
        sanitizeContentFields(updatedContent);
        existing.setTitle(updatedContent.getTitle());
        existing.setDescription(updatedContent.getDescription());
        existing.setCategory(updatedContent.getCategory());
        existing.setAdditionalInfo(updatedContent.getAdditionalInfo());
        existing.setImageUrl(updatedContent.getImageUrl());
        existing.setCtaLink(updatedContent.getCtaLink());
        return contentRepository.save(existing);
    }

    @Transactional
    public void deleteContent(Long contentId, User performedBy) {
        Content existing = getContentById(contentId);
        enforceCategoryAccess(performedBy, existing.getCategory());
        contentRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public Page<Content> getContentForManager(User user, Pageable pageable) {
        if (hasFullContentAccess(user)) {
            return getContent(pageable);
        }
        Set<ContentCategory> allowed = getAllowedCategories(user);
        if (allowed.isEmpty()) {
            return Page.empty(pageable);
        }
        return getContentByCategories(allowed, pageable);
    }

    @Transactional(readOnly = true)
    public Set<ContentCategory> getAllowedCategories(User user) {
        if (user == null || user.getRole() == null) {
            return EnumSet.noneOf(ContentCategory.class);
        }
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN, MODERATOR, CONTENT_MANAGER, EDITOR -> EnumSet.allOf(ContentCategory.class);
            case BOARDING_GUARDIAN -> EnumSet.of(ContentCategory.BOARDING);
            case SHUTTLE_DRIVER -> EnumSet.of(ContentCategory.SHUTTLE);
            case CLUB_PRESIDENT -> EnumSet.noneOf(ContentCategory.class);
            default -> EnumSet.noneOf(ContentCategory.class);
        };
    }

    private boolean hasFullContentAccess(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == Role.SUPER_ADMIN
            || user.getRole() == Role.ADMIN
                || user.getRole() == Role.MODERATOR
                || user.getRole() == Role.CONTENT_MANAGER
                || user.getRole() == Role.EDITOR;
    }

    private void enforceCategoryAccess(User user, ContentCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Select a category before publishing");
        }
        Set<ContentCategory> allowed = getAllowedCategories(user);
        if (!allowed.contains(category)) {
            throw new IllegalArgumentException("You are not allowed to publish under the " + category + " category");
        }
    }

    private void sanitizeContentFields(Content content) {
        if (content == null) {
            return;
        }
        if (StringUtils.hasText(content.getTitle())) {
            content.setTitle(content.getTitle().trim());
        }
        if (StringUtils.hasText(content.getDescription())) {
            content.setDescription(content.getDescription().trim());
        }
        content.setAdditionalInfo(cleanOptional(content.getAdditionalInfo()));
        content.setImageUrl(cleanOptional(content.getImageUrl()));
        content.setCtaLink(cleanOptional(content.getCtaLink()));
    }

    private String cleanOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void hydrateCreators(Page<Content> page) {
        if (page == null) {
            return;
        }
        page.forEach(item -> {
            if (item.getCreatedBy() != null) {
                item.getCreatedBy().getStudentId();
            }
        });
    }
}

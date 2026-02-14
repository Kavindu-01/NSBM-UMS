package lk.nsbm.university.service;

import lk.nsbm.university.dto.ContentReactionSummary;
import lk.nsbm.university.entity.Content;
import lk.nsbm.university.entity.ContentReaction;
import lk.nsbm.university.entity.ContentReactionType;
import lk.nsbm.university.entity.User;
import lk.nsbm.university.repository.ContentReactionRepository;
import lk.nsbm.university.repository.ContentRepository;
import lk.nsbm.university.repository.projection.ContentReactionCountProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ContentReactionService {

    private final ContentReactionRepository reactionRepository;
    private final ContentRepository contentRepository;

    public ContentReactionService(ContentReactionRepository reactionRepository,
                                  ContentRepository contentRepository) {
        this.reactionRepository = reactionRepository;
        this.contentRepository = contentRepository;
    }

    @Transactional
    public void react(Long contentId, User user, ContentReactionType desiredReaction) {
        if (user == null) {
            throw new IllegalStateException("Login required before reacting to content");
        }
        if (desiredReaction == null) {
            throw new IllegalArgumentException("Select a reaction before submitting");
        }
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException("Content not found"));
        Optional<ContentReaction> existing = reactionRepository.findByContentIdAndUserId(contentId, user.getId());
        if (existing.isPresent()) {
            ContentReaction reaction = existing.get();
            if (reaction.getReactionType() == desiredReaction) {
                reactionRepository.delete(reaction);
            } else {
                reaction.setReactionType(desiredReaction);
                reactionRepository.save(reaction);
            }
            return;
        }
        ContentReaction newReaction = new ContentReaction();
        newReaction.setContent(content);
        newReaction.setUser(user);
        newReaction.setReactionType(desiredReaction);
        reactionRepository.save(newReaction);
    }

    @Transactional(readOnly = true)
    public Map<Long, ContentReactionSummary> getSummaries(Collection<Long> contentIds, Long userId) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, EnumMap<ContentReactionType, Long>> aggregate = new HashMap<>();
        List<ContentReactionCountProjection> counts = reactionRepository.countByContentIds(contentIds);
        for (ContentReactionCountProjection projection : counts) {
            EnumMap<ContentReactionType, Long> map = aggregate.computeIfAbsent(
                    projection.getContentId(),
                    id -> new EnumMap<>(ContentReactionType.class));
            map.put(projection.getReactionType(), projection.getTotal());
        }
        Map<Long, ContentReactionType> userReactions = Collections.emptyMap();
        if (userId != null) {
            userReactions = new HashMap<>();
            List<ContentReaction> reactions = reactionRepository.findByContentIdInAndUserId(contentIds, userId);
            for (ContentReaction reaction : reactions) {
                userReactions.put(reaction.getContent().getId(), reaction.getReactionType());
            }
        }
        Map<Long, ContentReactionSummary> summaries = new HashMap<>();
        for (Long contentId : contentIds) {
            EnumMap<ContentReactionType, Long> map = aggregate.getOrDefault(contentId, new EnumMap<>(ContentReactionType.class));
            long likes = map.getOrDefault(ContentReactionType.LIKE, 0L);
            long dislikes = map.getOrDefault(ContentReactionType.DISLIKE, 0L);
            ContentReactionType userReaction = userReactions.get(contentId);
            summaries.put(contentId, new ContentReactionSummary(likes, dislikes, userReaction));
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public ContentReactionSummary getSummary(Long contentId, Long userId) {
        Map<Long, ContentReactionSummary> map = getSummaries(Collections.singleton(contentId), userId);
        return map.getOrDefault(contentId, new ContentReactionSummary(0, 0, null));
    }
}

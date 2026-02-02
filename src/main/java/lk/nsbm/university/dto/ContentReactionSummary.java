package lk.nsbm.university.dto;

import lk.nsbm.university.entity.ContentReactionType;

public record ContentReactionSummary(long likes, long dislikes, ContentReactionType userReaction) {

    public ContentReactionSummary {
        if (likes < 0 || dislikes < 0) {
            throw new IllegalArgumentException("Reaction counts cannot be negative");
        }
    }

    public ContentReactionSummary withUserReaction(ContentReactionType reactionType) {
        return new ContentReactionSummary(likes, dislikes, reactionType);
    }
}

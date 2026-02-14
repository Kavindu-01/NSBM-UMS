package lk.nsbm.university.repository.projection;

import lk.nsbm.university.entity.ContentReactionType;

public interface ContentReactionCountProjection {
    Long getContentId();
    ContentReactionType getReactionType();
    long getTotal();
}

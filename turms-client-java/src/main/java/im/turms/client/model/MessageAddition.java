package im.turms.client.model;

import java.util.Set;

public final class MessageAddition {
    private final boolean isMentioned;
    private final Set<Long> mentionedUserIds;

    public MessageAddition(boolean isMentioned, Set<Long> mentionedUserIds) {
        this.isMentioned = isMentioned;
        this.mentionedUserIds = mentionedUserIds;
    }

    public boolean isMentioned() {
        return isMentioned;
    }

    public Set<Long> getMentionedUserIds() {
        return mentionedUserIds;
    }
}

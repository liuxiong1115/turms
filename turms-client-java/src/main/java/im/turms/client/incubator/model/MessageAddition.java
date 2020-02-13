package im.turms.client.incubator.model;

import java.util.Set;

public class MessageAddition {
    public boolean isMentioned;
    public Set<Long> mentionedUserIds;

    public MessageAddition(boolean isMentioned, Set<Long> mentionedUserIds) {
        this.isMentioned = isMentioned;
        this.mentionedUserIds = mentionedUserIds;
    }
}

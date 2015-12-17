package org.briarproject.android.forum;

import org.briarproject.api.forum.Forum;
import org.briarproject.api.sync.MessageHeader;

import java.util.Collection;

class ForumListItem {

	private final Forum forum;
	private final boolean empty;
	private final long timestamp;
	private final int unread;

	ForumListItem(Forum forum, Collection<MessageHeader> headers) {
		this.forum = forum;
		empty = headers.isEmpty();
		if (empty) {
			timestamp = 0;
			unread = 0;
		} else {
			MessageHeader newest = null;
			long timestamp = -1;
			int unread = 0;
			for (MessageHeader h : headers) {
				if (h.getTimestamp() > timestamp) {
					timestamp = h.getTimestamp();
					newest = h;
				}
				if (!h.isRead()) unread++;
			}
			this.timestamp = newest.getTimestamp();
			this.unread = unread;
		}
	}

	Forum getForum() {
		return forum;
	}

	boolean isEmpty() {
		return empty;
	}

	long getTimestamp() {
		return timestamp;
	}

	int getUnreadCount() {
		return unread;
	}
}
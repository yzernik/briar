package net.sf.briar.api.db.event;

import net.sf.briar.api.ContactId;
import net.sf.briar.api.messaging.Group;

/** An event that is broadcast when a message is added to the database. */
public class MessageAddedEvent extends DatabaseEvent {

	private final Group group;
	private final ContactId contactId;

	public MessageAddedEvent(Group group, ContactId contactId) {
		this.group = group;
		this.contactId = contactId;
	}

	/** Returns the group to which the message belongs. */
	public Group getGroup() {
		return group;
	}

	/**
	 * Returns the ID of the contact from which the message was received, or
	 * null if the message was locally generated.
	 */
	public ContactId getContactId() {
		return contactId;
	}
}

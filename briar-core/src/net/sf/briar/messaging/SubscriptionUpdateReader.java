package net.sf.briar.messaging;

import static net.sf.briar.api.messaging.MessagingConstants.MAX_PACKET_LENGTH;
import static net.sf.briar.api.messaging.MessagingConstants.MAX_SUBSCRIPTIONS;
import static net.sf.briar.api.messaging.Types.SUBSCRIPTION_UPDATE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.briar.api.FormatException;
import net.sf.briar.api.messaging.Group;
import net.sf.briar.api.messaging.SubscriptionUpdate;
import net.sf.briar.api.serial.Consumer;
import net.sf.briar.api.serial.CountingConsumer;
import net.sf.briar.api.serial.Reader;
import net.sf.briar.api.serial.StructReader;

class SubscriptionUpdateReader implements StructReader<SubscriptionUpdate> {

	private final StructReader<Group> groupReader;

	SubscriptionUpdateReader(StructReader<Group> groupReader) {
		this.groupReader = groupReader;
	}

	public SubscriptionUpdate readStruct(Reader r) throws IOException {
		// Set up the reader
		Consumer counting = new CountingConsumer(MAX_PACKET_LENGTH);
		r.addConsumer(counting);
		// Read the start of the struct
		r.readStructStart(SUBSCRIPTION_UPDATE);
		// Read the subscriptions
		List<Group> groups = new ArrayList<Group>();
		r.readListStart();
		for(int i = 0; i < MAX_SUBSCRIPTIONS && !r.hasListEnd(); i++)
			groups.add(groupReader.readStruct(r));
		r.readListEnd();
		// Read the version number
		long version = r.readIntAny();
		if(version < 0) throw new FormatException();
		// Read the end of the struct
		r.readStructEnd();
		// Reset the reader
		r.removeConsumer(counting);
		// Build and return the subscription update
		groups = Collections.unmodifiableList(groups);
		return new SubscriptionUpdate(groups, version);
	}
}

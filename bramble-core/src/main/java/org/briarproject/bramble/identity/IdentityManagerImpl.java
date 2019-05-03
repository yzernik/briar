package org.briarproject.bramble.identity;

import org.briarproject.bramble.api.crypto.CryptoComponent;
import org.briarproject.bramble.api.crypto.KeyPair;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.identity.AuthorFactory;
import org.briarproject.bramble.api.identity.Identity;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.identity.LocalAuthor;
import org.briarproject.bramble.api.lifecycle.LifecycleManager.OpenDatabaseHook;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.system.Clock;

import java.util.Collection;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.api.nullsafety.NullSafety.requireNonNull;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.now;

@ThreadSafe
@NotNullByDefault
class IdentityManagerImpl implements IdentityManager, OpenDatabaseHook {

	private static final Logger LOG =
			getLogger(IdentityManagerImpl.class.getName());

	private final DatabaseComponent db;
	private final CryptoComponent crypto;
	private final AuthorFactory authorFactory;
	private final Clock clock;

	/**
	 * The user's identity, or null if no identity has been registered or
	 * loaded. If non-null, this identity always has handshake keys.
	 */
	@Nullable
	private volatile Identity cachedIdentity = null;

	/**
	 * True if {@code cachedIdentity} was registered via
	 * {@link #registerIdentity(Identity)} and should be stored when
	 * {@link #onDatabaseOpened(Transaction)} is called.
	 */

	private volatile boolean shouldStoreIdentity = false;

	/**
	 * True if the handshake keys in {@code cachedIdentity} were generated
	 * when the identity was loaded and should be stored when
	 * {@link #onDatabaseOpened(Transaction)} is called.
	 */
	private volatile boolean shouldStoreKeys = false;

	@Inject
	IdentityManagerImpl(DatabaseComponent db, CryptoComponent crypto,
			AuthorFactory authorFactory, Clock clock) {
		this.db = db;
		this.crypto = crypto;
		this.authorFactory = authorFactory;
		this.clock = clock;
	}

	@Override
	public Identity createIdentity(String name) {
		long start = now();
		LocalAuthor localAuthor = authorFactory.createLocalAuthor(name);
		KeyPair handshakeKeyPair = crypto.generateAgreementKeyPair();
		byte[] handshakePub = handshakeKeyPair.getPublic().getEncoded();
		byte[] handshakePriv = handshakeKeyPair.getPrivate().getEncoded();
		logDuration(LOG, "Creating identity", start);
		return new Identity(localAuthor, handshakePub, handshakePriv,
				clock.currentTimeMillis());
	}

	@Override
	public void registerIdentity(Identity i) {
		if (!i.hasHandshakeKeyPair()) throw new IllegalArgumentException();
		cachedIdentity = i;
		shouldStoreIdentity = true;
		LOG.info("Identity registered");
	}

	@Override
	public void onDatabaseOpened(Transaction txn) throws DbException {
		Identity cached = getCachedIdentity(txn);
		if (shouldStoreIdentity) {
			db.addIdentity(txn, cached);
			LOG.info("Identity stored");
		} else if (shouldStoreKeys) {
			byte[] publicKey = requireNonNull(cached.getHandshakePublicKey());
			byte[] privateKey = requireNonNull(cached.getHandshakePrivateKey());
			db.setHandshakeKeyPair(txn, cached.getId(), publicKey, privateKey);
			LOG.info("Handshake key pair stored");
		}
	}

	@Override
	public LocalAuthor getLocalAuthor() throws DbException {
		Identity cached = cachedIdentity;
		if (cached == null)
			cached = db.transactionWithResult(true, this::getCachedIdentity);
		return cached.getLocalAuthor();
	}

	@Override
	public LocalAuthor getLocalAuthor(Transaction txn) throws DbException {
		return getCachedIdentity(txn).getLocalAuthor();
	}

	@Override
	public byte[][] getHandshakeKeys(Transaction txn) throws DbException {
		Identity cached = getCachedIdentity(txn);
		return new byte[][] {
				cached.getHandshakePublicKey(),
				cached.getHandshakePrivateKey()
		};
	}

	private Identity getCachedIdentity(Transaction txn) throws DbException {
		Identity cached = cachedIdentity;
		if (cached == null)
			cachedIdentity = cached = loadIdentityWithKeyPair(txn);
		return cached;
	}

	private Identity loadIdentityWithKeyPair(Transaction txn)
			throws DbException {
		Identity i = loadIdentity(txn);
		LOG.info("Identity loaded");
		if (i.hasHandshakeKeyPair()) return i;
		KeyPair keyPair = crypto.generateAgreementKeyPair();
		byte[] publicKey = keyPair.getPublic().getEncoded();
		byte[] privateKey = keyPair.getPrivate().getEncoded();
		LOG.info("Handshake key pair generated");
		shouldStoreKeys = true;
		return new Identity(i.getLocalAuthor(), publicKey, privateKey,
				i.getTimeCreated());
	}

	private Identity loadIdentity(Transaction txn) throws DbException {
		Collection<Identity> identities = db.getIdentities(txn);
		if (identities.size() != 1) throw new DbException();
		return identities.iterator().next();
	}
}

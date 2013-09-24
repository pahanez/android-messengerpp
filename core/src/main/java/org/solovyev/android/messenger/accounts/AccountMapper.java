package org.solovyev.android.messenger.accounts;

import android.database.Cursor;
import android.util.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import org.solovyev.android.messenger.App;
import org.solovyev.android.messenger.entities.EntityImpl;
import org.solovyev.android.messenger.realms.Realm;
import org.solovyev.android.messenger.realms.UnsupportedRealmException;
import org.solovyev.android.messenger.users.User;
import org.solovyev.common.Converter;
import org.solovyev.common.security.Cipherer;
import org.solovyev.common.security.CiphererException;

import com.google.gson.Gson;

public class AccountMapper<C extends AccountConfiguration> implements Converter<Cursor, Account<C>> {

	@Nullable
	private final SecretKey secret;

	public AccountMapper(@Nullable SecretKey secret) {
		this.secret = secret;
	}

	@Nonnull
	@Override
	public Account<C> convert(@Nonnull Cursor cursor) {
		final String accountId = cursor.getString(0);
		final String realmId = cursor.getString(1);
		final String userId = cursor.getString(2);
		final String configuration = cursor.getString(3);
		final String state = cursor.getString(4);

		try {
			final Realm<C> realm = (Realm<C>) App.getRealmService().getRealmById(realmId);
			// realm is not loaded => no way we can find user in realm services
			final User user = App.getUserService().getUserById(EntityImpl.fromEntityId(userId), false);

			final C encryptedConfiguration = new Gson().fromJson(configuration, realm.getConfigurationClass());

			final C decryptedConfiguration = decryptConfiguration(realm, encryptedConfiguration);

			return realm.newAccount(accountId, user, decryptedConfiguration, AccountState.valueOf(state));
		} catch (UnsupportedRealmException e) {
			throw new AccountRuntimeException(accountId, e);
		}
	}

	@Nonnull
	private C decryptConfiguration(@Nonnull Realm<C> realm, @Nonnull C encryptedConfiguration) {
		try {
			final C decryptedConfiguration;
			final Cipherer<C, C> cipherer = realm.getCipherer();
			if (secret != null && cipherer != null) {
				decryptedConfiguration = cipherer.decrypt(secret, encryptedConfiguration);
			} else {
				decryptedConfiguration = encryptedConfiguration;
			}
			return decryptedConfiguration;
		} catch (CiphererException e) {
			Log.e("Realm", e.getMessage(), e);
			// user will see an error notification later when realm will try to connect to remote server
			return encryptedConfiguration;
		}
	}
}
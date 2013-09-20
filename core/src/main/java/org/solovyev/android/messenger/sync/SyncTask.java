package org.solovyev.android.messenger.sync;

import org.joda.time.DateTime;
import org.solovyev.android.messenger.MessengerApplication;
import org.solovyev.android.messenger.realms.RealmException;
import org.solovyev.android.messenger.realms.AccountService;
import org.solovyev.android.messenger.realms.UnsupportedRealmException;
import org.solovyev.android.messenger.users.User;

import javax.annotation.Nonnull;

/**
 * User: serso
 * Date: 5/30/12
 * Time: 11:18 PM
 */
public enum SyncTask {

	user {
		@Override
		public boolean isTime(@Nonnull SyncData syncData) {
			boolean result = false;

			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				final DateTime lastPropertiesSyncDate = user.getUserSyncData().getLastPropertiesSyncDate();
				if (lastPropertiesSyncDate == null || lastPropertiesSyncDate.plusHours(1).isBefore(DateTime.now())) {
					result = true;
				}
			} catch (UnsupportedRealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}

			return result;
		}

		@Override
		public void doTask(@Nonnull SyncData syncData) {
			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				MessengerApplication.getServiceLocator().getUserService().syncUser(user.getEntity());
			} catch (RealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}
		}
	},

	user_contacts {
		@Override
		public boolean isTime(@Nonnull SyncData syncData) {
			boolean result = false;

			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				final DateTime lastContactsSyncDate = user.getUserSyncData().getLastContactsSyncDate();
				if (lastContactsSyncDate == null || lastContactsSyncDate.plusHours(1).isBefore(DateTime.now())) {
					result = true;
				}
			} catch (UnsupportedRealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}

			return result;
		}

		@Override
		public void doTask(@Nonnull SyncData syncData) {
			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				MessengerApplication.getServiceLocator().getUserService().syncUserContacts(user.getEntity());
			} catch (RealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}
		}
	},

	user_icons {
		@Override
		public boolean isTime(@Nonnull SyncData syncData) {
			boolean result = false;

			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				final DateTime lastUserIconsSyncDate = user.getUserSyncData().getLastUserIconsSyncData();
				if (lastUserIconsSyncDate == null || lastUserIconsSyncDate.plusDays(1).isBefore(DateTime.now())) {
					result = true;
				}
			} catch (UnsupportedRealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}

			return result;
		}

		@Override
		public void doTask(@Nonnull SyncData syncData) {
			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();

				MessengerApplication.getServiceLocator().getUserService().fetchUserAndContactsIcons(user);

			} catch (UnsupportedRealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}
		}
	},

	user_contacts_statuses {
		@Override
		public boolean isTime(@Nonnull SyncData syncData) {
			return true;
		}

		@Override
		public void doTask(@Nonnull SyncData syncData) {
			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				MessengerApplication.getServiceLocator().getUserService().syncUserContactsStatuses(user.getEntity());
			} catch (RealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}
		}
	},

	user_chats {
		@Override
		public boolean isTime(@Nonnull SyncData syncData) {
			boolean result = false;

			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				final DateTime lastChatsSyncDate = user.getUserSyncData().getLastChatsSyncDate();
				if (lastChatsSyncDate == null || lastChatsSyncDate.plusHours(24).isBefore(DateTime.now())) {
					result = true;
				}
			} catch (UnsupportedRealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}

			return result;
		}

		@Override
		public void doTask(@Nonnull SyncData syncData) {
			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				MessengerApplication.getServiceLocator().getUserService().syncUserChats(user.getEntity());
			} catch (RealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}
		}
	},

	chat_messages {
		@Override
		public boolean isTime(@Nonnull SyncData syncData) {
			return true;
		}

		@Override
		public void doTask(@Nonnull SyncData syncData) {
			try {
				final User user = getRealmService().getAccountById(syncData.getRealmId()).getUser();
				MessengerApplication.getServiceLocator().getChatService().syncChatMessages(user.getEntity());
			} catch (RealmException e) {
				// ok, user is not logged in
				MessengerApplication.getServiceLocator().getExceptionHandler().handleException(e);
			}
		}
	};

	@Nonnull
	private static AccountService getRealmService() {
		return MessengerApplication.getServiceLocator().getAccountService();
	}

	public abstract boolean isTime(@Nonnull SyncData syncData);

	public abstract void doTask(@Nonnull SyncData syncData);
}

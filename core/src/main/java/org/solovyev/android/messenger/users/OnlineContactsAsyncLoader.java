/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.solovyev.android.messenger.users;

import android.content.Context;
import org.solovyev.android.list.ListAdapter;
import org.solovyev.android.messenger.BaseAsyncLoader;
import org.solovyev.android.messenger.App;
import org.solovyev.android.messenger.accounts.Account;
import org.solovyev.android.messenger.accounts.AccountService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.solovyev.android.messenger.users.ContactListItem.newContactListItem;
import static org.solovyev.android.messenger.users.UiContact.loadUiContact;

/**
 * User: serso
 * Date: 6/2/12
 * Time: 5:24 PM
 */
public class OnlineContactsAsyncLoader extends BaseAsyncLoader<UiContact, ContactListItem> {

	@Nonnull
	private final AccountService accountService;

	OnlineContactsAsyncLoader(@Nonnull Context context,
							  @Nonnull ListAdapter<ContactListItem> adapter,
							  @Nullable Runnable onPostExecute,
							  @Nonnull AccountService accountService) {
		super(context, adapter, onPostExecute);
		this.accountService = accountService;
	}

	@Nonnull
	protected List<UiContact> getElements(@Nonnull Context context) {
		final List<UiContact> result = new ArrayList<UiContact>();

		final UserService userService = App.getUserService();

		for (Account account : accountService.getEnabledAccounts()) {
			final User user = account.getUser();
			for (User contact : userService.getOnlineUserContacts(user.getEntity())) {
				result.add(loadUiContact(contact, account));
			}
		}

		return result;
	}

	@Nonnull
	@Override
	protected ContactListItem createListItem(@Nonnull UiContact contact) {
		return newContactListItem(contact);
	}
}

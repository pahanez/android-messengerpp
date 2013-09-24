package org.solovyev.android.messenger.realms.sms;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.solovyev.android.messenger.accounts.AccountConfiguration;
import org.solovyev.android.messenger.accounts.BaseAccountConfigurationFragment;
import org.solovyev.android.messenger.realms.Realm;

import com.google.inject.Inject;

/**
 * User: serso
 * Date: 5/27/13
 * Time: 9:08 PM
 */
public final class SmsAccountConfigurationFragment extends BaseAccountConfigurationFragment<SmsAccount> {

    /*
	**********************************************************************
    *
    *                           AUTO INJECTED FIELDS
    *
    **********************************************************************
    */

	@Inject
	@Nonnull
	private SmsRealm realmDef;

	public SmsAccountConfigurationFragment() {
		super(R.layout.mpp_realm_sms_conf);
	}

	@Nullable
	@Override
	protected AccountConfiguration validateData() {
		return new SmsAccountConfiguration();
	}

	@Nonnull
	@Override
	public Realm getRealm() {
		return realmDef;
	}
}
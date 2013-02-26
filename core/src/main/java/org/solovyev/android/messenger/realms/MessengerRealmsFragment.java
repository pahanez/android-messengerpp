package org.solovyev.android.messenger.realms;

import android.content.Context;
import android.os.Bundle;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.solovyev.android.menu.ActivityMenu;
import org.solovyev.android.menu.IdentifiableMenuItem;
import org.solovyev.android.menu.ListActivityMenu;
import org.solovyev.android.messenger.AbstractMessengerListFragment;
import org.solovyev.android.messenger.MessengerListItemAdapter;
import org.solovyev.android.messenger.R;
import org.solovyev.android.messenger.api.MessengerAsyncTask;
import org.solovyev.android.sherlock.menu.SherlockMenuHelper;
import org.solovyev.android.view.ListViewAwareOnRefreshListener;

import java.util.ArrayList;
import java.util.List;

public class MessengerRealmsFragment extends AbstractMessengerListFragment<Realm, RealmListItem> {

    @Inject
    @NotNull
    private RealmService realmService;

    private ActivityMenu<Menu, MenuItem> menu;

    public MessengerRealmsFragment() {
        super("Realms");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    protected boolean isFilterEnabled() {
        return false;
    }

    @Nullable
    @Override
    protected ListViewAwareOnRefreshListener getTopPullRefreshListener() {
        return null;
    }

    @Nullable
    @Override
    protected ListViewAwareOnRefreshListener getBottomPullRefreshListener() {
        return null;
    }

    @NotNull
    @Override
    protected MessengerListItemAdapter<RealmListItem> createAdapter() {
        final List<RealmListItem> listItems = new ArrayList<RealmListItem>();
        for (Realm realm : realmService.getRealms()) {
            listItems.add(new RealmListItem(realm));
        }
        return new MessengerListItemAdapter<RealmListItem>(getActivity(), listItems);
    }

    @Nullable
    @Override
    protected MessengerAsyncTask<Void, Void, List<Realm>> createAsyncLoader(@NotNull MessengerListItemAdapter<RealmListItem> adapter, @NotNull Runnable onPostExecute) {
        return null;
    }

    /*
    **********************************************************************
    *
    *                           MENU
    *
    **********************************************************************
    */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.menu == null) {
            this.menu = ListActivityMenu.fromResource(R.menu.mpp_realms_menu, RealmsMenu.class, SherlockMenuHelper.getInstance());
        }

        this.menu.onCreateOptionsMenu(this.getActivity(), menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.menu.onPrepareOptionsMenu(this.getActivity(), menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return this.menu.onOptionsItemSelected(this.getActivity(), item) || super.onOptionsItemSelected(item);
    }

    private static enum RealmsMenu implements IdentifiableMenuItem<MenuItem> {
        realm_add(R.id.mpp_realm_add) {
            @Override
            public void onClick(@NotNull MenuItem data, @NotNull Context context) {
                MessengerRealmDefsActivity.startActivity(context);
            }
        };

        private final int menuItemId;

        RealmsMenu(int menuItemId) {
            this.menuItemId = menuItemId;
        }

        @NotNull
        @Override
        public Integer getItemId() {
            return this.menuItemId;
        }
    }
}
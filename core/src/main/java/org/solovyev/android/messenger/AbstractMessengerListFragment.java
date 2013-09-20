package org.solovyev.android.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockListFragment;
import com.google.inject.Inject;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.internal.LoadingLayout;
import org.solovyev.android.Threads;
import org.solovyev.android.list.ListItem;
import org.solovyev.android.list.ListViewScroller;
import org.solovyev.android.list.ListViewScrollerListener;
import org.solovyev.android.messenger.api.MessengerAsyncTask;
import org.solovyev.android.messenger.chats.ChatService;
import org.solovyev.android.messenger.core.R;
import org.solovyev.android.messenger.fragments.FragmentGuiEventType;
import org.solovyev.android.messenger.messages.ChatMessageService;
import org.solovyev.android.messenger.realms.AccountService;
import org.solovyev.android.messenger.sync.SyncService;
import org.solovyev.android.messenger.users.UserEvent;
import org.solovyev.android.messenger.users.UserService;
import org.solovyev.android.messenger.view.MessengerListItem;
import org.solovyev.android.messenger.view.PublicPullToRefreshListView;
import org.solovyev.android.view.ListViewAwareOnRefreshListener;
import org.solovyev.android.view.OnRefreshListener2Adapter;
import org.solovyev.common.listeners.AbstractJEventListener;
import org.solovyev.common.listeners.JEventListener;
import roboguice.event.EventManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * User: serso
 * Date: 6/7/12
 * Time: 5:57 PM
 */
public abstract class AbstractMessengerListFragment<T, LI extends MessengerListItem>
		extends RoboSherlockListFragment
		implements ListViewScrollerListener, ListViewFilter.FilterableListView {

    /*
	**********************************************************************
    *
    *                           CONSTANTS
    *
    **********************************************************************
    */

	private static final int NOT_SELECTED_POSITION = -1;

	/**
	 * Constants are copied from list fragment, see {@link android.support.v4.app.ListFragment}
	 */
	private static final int INTERNAL_EMPTY_ID = 0x00ff0001;

	private static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;

	private static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    /*
    **********************************************************************
    *
    *                           AUTO INJECTED FIELDS
    *
    **********************************************************************
    */


	@Inject
	@Nonnull
	private AccountService accountService;

	@Inject
	@Nonnull
	private UserService userService;

	@Inject
	@Nonnull
	private ChatService chatService;

	@Inject
	@Nonnull
	private ChatMessageService chatMessageService;

	@Inject
	@Nonnull
	private SyncService syncService;

	@Inject
	@Nonnull
	private MessengerMultiPaneManager multiPaneManager;

	@Inject
	@Nonnull
	private EventManager eventManager;

    /*
    **********************************************************************
    *
    *                           OWN FIELDS
    *
    **********************************************************************
    */

	@Nullable
	private JEventListener<UserEvent> userEventListener;

	private MessengerListItemAdapter<LI> adapter;

	@Nullable
	private MessengerAsyncTask<Void, Void, List<T>> listLoader;

	/**
	 * Filter for list view, null if filter is disabled for current list fragment
	 */
	@Nullable
	private final ListViewFilter listViewFilter;

	@Nonnull
	private final String tag;

	@Nullable
	private PublicPullToRefreshListView pullToRefreshListView;

	/**
	 * Mode which is used for {@link PullToRefreshListView}.
	 * Note: null if simple {@link ListView} is used instead of {@link PullToRefreshListView}.
	 */
	@SuppressWarnings("FieldCanBeLocal")
	@Nullable
	private PullToRefreshBase.Mode pullToRefreshMode;


	/**
	 * If nothing selected - first list item will be selected
	 */
	private final boolean selectFirstItemByDefault;

	@Nonnull
	private final Handler uiHandler = Threads.newUiHandler();

    /*
    **********************************************************************
    *
    *                           CONSTRUCTORS
    *
    **********************************************************************
    */

	public AbstractMessengerListFragment(@Nonnull String tag, boolean filterEnabled, boolean selectFirstItemByDefault) {
		this.tag = tag;
		if (filterEnabled) {
			this.listViewFilter = new ListViewFilter(this, this);
		} else {
			this.listViewFilter = null;
		}
		this.selectFirstItemByDefault = selectFirstItemByDefault;
	}

    /*
    **********************************************************************
    *
    *                           GETTERS
    *
    **********************************************************************
    */

	@Nonnull
	protected UserService getUserService() {
		return userService;
	}

	@Nonnull
	protected SyncService getSyncService() {
		return syncService;
	}

	@Nonnull
	protected ChatService getChatService() {
		return chatService;
	}

	@Nonnull
	protected ChatMessageService getChatMessageService() {
		return chatMessageService;
	}

	@Nonnull
	protected AccountService getAccountService() {
		return accountService;
	}

	@Nonnull
	protected MessengerListItemAdapter getAdapter() {
		return adapter;
	}

	@Nonnull
	protected EventManager getEventManager() {
		return eventManager;
	}

	@Nonnull
	protected MessengerMultiPaneManager getMultiPaneManager() {
		return multiPaneManager;
	}

	@Nonnull
	protected ListView getListView(@Nonnull View root) {
		return (ListView) root.findViewById(android.R.id.list);
	}

	@Nullable
	public PublicPullToRefreshListView getPullToRefreshListView() {
		return pullToRefreshListView;
	}

    /*
    **********************************************************************
    *
    *                           LIFECYCLE
    *
    **********************************************************************
    */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		eventManager.fire(FragmentGuiEventType.created.newEvent(this));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		final LinearLayout root = new LinearLayout(this.getActivity());
		root.setOrientation(LinearLayout.VERTICAL);
		root.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		if (listViewFilter != null) {
			final View filterView = listViewFilter.createView(savedInstanceState);
			root.addView(filterView, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
		}

		final View listViewParent = createListView();

		final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
		params.gravity = CENTER_VERTICAL;
		root.addView(listViewParent, params);

		// some fragments may change the title and icon of action bar => we need to reset it every time new fragment is shown
		final ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setTitle(R.string.mpp_app_name);
		actionBar.setIcon(R.drawable.mpp_app_icon);

		multiPaneManager.onCreatePane(getActivity(), container, root);

		return root;
	}

	@Override
	public void onViewCreated(View root, Bundle savedInstanceState) {
		super.onViewCreated(root, savedInstanceState);

		setListShown(false);

		if (listViewFilter != null) {
			listViewFilter.onViewCreated();
		}

		eventManager.fire(FragmentGuiEventType.shown.newEvent(this));
	}

	public void toggleFilterBox() {
		if (listViewFilter != null) {
			listViewFilter.toggleView();
		}
	}

	@Nonnull
	private View createListView() {
		final Context context = getActivity();

		final FrameLayout root = new FrameLayout(context);

		// ------------------------------------------------------------------

		final LinearLayout progressContainer = new LinearLayout(context);
		progressContainer.setId(INTERNAL_PROGRESS_CONTAINER_ID);
		progressContainer.setOrientation(LinearLayout.VERTICAL);
		progressContainer.setVisibility(View.GONE);
		progressContainer.setGravity(Gravity.CENTER);

		final ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
		progressContainer.addView(progress, new FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

		root.addView(progressContainer, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		// ------------------------------------------------------------------

		final FrameLayout listViewContainer = new FrameLayout(context);
		listViewContainer.setId(INTERNAL_LIST_CONTAINER_ID);

		final TextView emptyListCaption = new TextView(context);
		emptyListCaption.setId(INTERNAL_EMPTY_ID);
		emptyListCaption.setGravity(Gravity.CENTER);
		listViewContainer.addView(emptyListCaption, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));


		final ListViewAwareOnRefreshListener topRefreshListener = getTopPullRefreshListener();
		final ListViewAwareOnRefreshListener bottomRefreshListener = getBottomPullRefreshListener();

		final View listView;

		final Resources resources = context.getResources();
		if (topRefreshListener == null && bottomRefreshListener == null) {
			pullToRefreshMode = null;
			listView = new ListView(context);
			fillListView((ListView) listView, context);
			listView.setId(android.R.id.list);
		} else if (topRefreshListener != null && bottomRefreshListener != null) {
			pullToRefreshMode = PullToRefreshBase.Mode.BOTH;
			pullToRefreshListView = new PublicPullToRefreshListView(context, pullToRefreshMode);

			topRefreshListener.setListView(pullToRefreshListView);
			bottomRefreshListener.setListView(pullToRefreshListView);

			fillListView(pullToRefreshListView.getRefreshableView(), context);
			pullToRefreshListView.setShowIndicator(false);
			prepareLoadingView(resources, pullToRefreshListView.getHeaderLoadingView());
			prepareLoadingView(resources, pullToRefreshListView.getFooterLoadingView());

			pullToRefreshListView.setOnRefreshListener(new OnRefreshListener2Adapter(topRefreshListener, bottomRefreshListener));
			listView = pullToRefreshListView;
		} else if (topRefreshListener != null) {
			pullToRefreshMode = PullToRefreshBase.Mode.PULL_DOWN_TO_REFRESH;
			pullToRefreshListView = new PublicPullToRefreshListView(context, pullToRefreshMode);

			topRefreshListener.setListView(pullToRefreshListView);

			fillListView(pullToRefreshListView.getRefreshableView(), context);

			pullToRefreshListView.setShowIndicator(false);
			prepareLoadingView(resources, pullToRefreshListView.getHeaderLoadingView());
			prepareLoadingView(resources, pullToRefreshListView.getFooterLoadingView());

			pullToRefreshListView.setOnRefreshListener(topRefreshListener);

			listView = pullToRefreshListView;
		} else {
			pullToRefreshMode = PullToRefreshBase.Mode.PULL_UP_TO_REFRESH;
			pullToRefreshListView = new PublicPullToRefreshListView(context, pullToRefreshMode);

			bottomRefreshListener.setListView(pullToRefreshListView);

			fillListView(pullToRefreshListView.getRefreshableView(), context);
			pullToRefreshListView.setShowIndicator(false);
			prepareLoadingView(resources, pullToRefreshListView.getHeaderLoadingView());
			prepareLoadingView(resources, pullToRefreshListView.getFooterLoadingView());

			pullToRefreshListView.setOnRefreshListener(bottomRefreshListener);

			listView = pullToRefreshListView;
		}

		listViewContainer.addView(listView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		root.addView(listViewContainer, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		// ------------------------------------------------------------------

		root.setLayoutParams(new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

		return root;
	}

	protected void fillListView(@Nonnull ListView lv, @Nonnull Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			lv.setScrollbarFadingEnabled(true);
		}
		lv.setCacheColorHint(Color.TRANSPARENT);
		ListViewScroller.createAndAttach(lv, this);
		lv.setFastScrollEnabled(true);

		// filter so done manually
		lv.setTextFilterEnabled(false);

		lv.setVerticalFadingEdgeEnabled(false);
		lv.setFocusable(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			lv.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
		}
		lv.setDividerHeight(1);
	}

	private void prepareLoadingView(@Nonnull Resources resources, @Nullable LoadingLayout loadingView) {
		if (loadingView != null) {
			multiPaneManager.fillLoadingLayout(this.getActivity(), resources, loadingView);
		}
	}

	@Nullable
	protected abstract ListViewAwareOnRefreshListener getTopPullRefreshListener();

	@Nullable
	protected abstract ListViewAwareOnRefreshListener getBottomPullRefreshListener();

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		userEventListener = new UiThreadUserEventListener();
		userService.addListener(userEventListener);

		final ListView lv = getListView();
		lv.setOnItemClickListener(new ListViewOnItemClickListener());
		lv.setOnItemLongClickListener(new ListViewOnItemLongClickListener());

		// as newly loaded list can differ from one used last time position may be not accurate
		// better approach is to use list item which was previously selected and reuse it
		Integer selectedPosition = null;
		ListItem selectedListItem = null;
		if (adapter != null) {
			// adapter not null => this fragment has been created earlier (and now it just goes to the shown state)
			selectedPosition = adapter.getSelectedItemPosition();
			if (selectedPosition == NOT_SELECTED_POSITION && selectFirstItemByDefault) {
				// there were no elements in adapter => position == NOT_SELECTED_POSITION
				// but we need to select first element if selectFirstItemByDefault == true => do it
				selectedPosition = 0;
			} else if (selectedPosition >= 0 && selectedPosition < adapter.getCount()) {
				// selected position exists => exists selected list item => can use it
				selectedListItem = adapter.getItem(selectedPosition);
			}
		}

		adapter = createAdapter();
		if (savedInstanceState != null) {
			adapter.restoreState(savedInstanceState);
		}

		setListAdapter(adapter);

		if (selectedPosition == null) {
			if (savedInstanceState != null) {
				selectedPosition = adapter.restoreSelectedPosition(savedInstanceState, selectFirstItemByDefault ? 0 : NOT_SELECTED_POSITION);
			} else {
				selectedPosition = selectFirstItemByDefault ? 0 : NOT_SELECTED_POSITION;
			}
		}

		final PostListLoadingRunnable onPostExecute = new PostListLoadingRunnable(selectedPosition, selectedListItem, lv);

		this.listLoader = createAsyncLoader(adapter, onPostExecute);
		if (this.listLoader != null) {
			this.listLoader.executeInParallel();
		} else {
			// we need to schedule onPostExecute in order to be after all pending transaction in fragment manager
			uiHandler.post(onPostExecute);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		eventManager.fire(FragmentGuiEventType.started.newEvent(this));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (adapter != null) {
			adapter.saveState(outState);
		}

		if (listViewFilter != null) {
			listViewFilter.saveState(outState);
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		if (listLoader != null) {
			listLoader.cancel(false);
			listLoader = null;
		}

		if (userEventListener != null) {
			this.userService.removeListener(userEventListener);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void filter(@Nullable CharSequence filterText) {
		filter(filterText, null);
	}

	public void filter(@Nullable  CharSequence filterText, @Nullable Filter.FilterListener filterListener) {
		if (this.adapter != null && this.adapter.isInitialized()) {
			this.adapter.filter(filterText == null ? null : filterText.toString(), filterListener);
		}
	}

	@Nonnull
	protected abstract MessengerListItemAdapter<LI> createAdapter();

	@Nullable
	protected abstract MessengerAsyncTask<Void, Void, List<T>> createAsyncLoader(@Nonnull MessengerListItemAdapter<LI> adapter, @Nonnull Runnable onPostExecute);

    /*
    **********************************************************************
    *
    *                           SCROLLING
    *
    **********************************************************************
    */


	public void onItemReachedFromTop(int position) {
	}

	public void onItemReachedFromBottom(int position) {
	}

	public void onBottomReached() {
	}

	public void onTopReached() {
	}

	public final void selectListItem(@Nonnull String listItemId) {
		if (adapter != null && adapter.isInitialized()) {
			final int size = adapter.getCount();
			for (int i = 0; i < size; i++) {
				final MessengerListItem listItem = adapter.getItem(i);
				if (listItem.getId().equals(listItemId)) {
					adapter.getSelectedItemListener().onItemClick(i);
					break;
				}
			}
		}
	}

    /*
    **********************************************************************
    *
    *                           LISTENERS, HELPERS, ETC
    *
    **********************************************************************
    */

	private class UiThreadUserEventListener extends AbstractJEventListener<UserEvent> {

		private UiThreadUserEventListener() {
			super(UserEvent.class);
		}

		@Override
		public void onEvent(@Nonnull final UserEvent event) {
			Threads2.tryRunOnUiThread(AbstractMessengerListFragment.this, new Runnable() {
				@Override
				public void run() {
					adapter.onEvent(event);
				}
			});
		}
	}

	private class PostListLoadingRunnable implements Runnable {

		private final int selectedPosition;

		@Nullable
		private final ListItem selectedListItem;

		@Nonnull
		private final ListView listView;

		public PostListLoadingRunnable(int selectedPosition, @Nullable ListItem selectedListItem, @Nonnull ListView lv) {
			this.selectedPosition = selectedPosition;
			this.selectedListItem = selectedListItem;
			this.listView = lv;
		}

		@Override
		public void run() {
			// change adapter state
			adapter.setInitialized(true);

			final Activity activity = getActivity();
			if (activity != null) {

				try {
					runPostFilling();
				} catch (IllegalStateException e) {
					// todo serso: investigate the root of the problem
					// oops, list view is not created yet
					Log.w(tag, e.getMessage(), e);
				}
			}
		}

		private void runPostFilling() {
			// apply filter if any
			if (listViewFilter != null) {
				filter(listViewFilter.getFilterText(), new PostListLoadingFilterListener());
			} else {
				filter(null, new PostListLoadingFilterListener());
			}
		}

		private final class PostListLoadingFilterListener implements Filter.FilterListener {

			@Override
			public void onFilterComplete(int count) {
				final Activity activity = getActivity();
				if (activity != null && !activity.isFinishing() && !isDetached()) {

					// change UI state
					setListShown(true);

					int position = -1;
					if (selectedListItem != null) {
						position = adapter.getSelectedItemListener().onItemClick(selectedListItem);
					} else {
						if (selectedPosition >= 0 && selectedPosition < adapter.getCount()) {
							adapter.getSelectedItemListener().onItemClick(selectedPosition);
						}
					}

					if (position < 0) {
						position = selectedPosition;
					}

					if (multiPaneManager.isDualPane(activity)) {
						if (position >= 0 && position < adapter.getCount()) {
							adapter.getSelectedItemListener().onItemClick(position);
							final ListItem.OnClickAction onClickAction = adapter.getItem(position).getOnClickAction();
							if (onClickAction != null) {
								onClickAction.onClick(activity, adapter, listView);
							}
						}
					}

				}
			}
		}
	}

	private class ListViewOnItemClickListener implements AdapterView.OnItemClickListener {

		public void onItemClick(final AdapterView<?> parent,
								final View view,
								final int position,
								final long id) {
			final Object itemAtPosition = parent.getItemAtPosition(position);

			if (itemAtPosition instanceof ListItem) {
				final ListItem listItem = (ListItem) itemAtPosition;
				// notify adapter

				adapter.getSelectedItemListener().onItemClick(listItem);

				final ListItem.OnClickAction onClickAction = listItem.getOnClickAction();
				if (onClickAction != null) {
					onClickAction.onClick(getActivity(), adapter, getListView());
				}
			}
		}
	}

	private class ListViewOnItemLongClickListener implements AdapterView.OnItemLongClickListener {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			final Object itemAtPosition = parent.getItemAtPosition(position);

			if (itemAtPosition instanceof ListItem) {
				final ListItem listItem = (ListItem) itemAtPosition;
				// notify adapter

				final ListItem.OnClickAction onClickAction = listItem.getOnLongClickAction();
				if (onClickAction != null) {
					onClickAction.onClick(getActivity(), adapter, getListView());
					return true;
				}
			}

			return false;
		}
	}
}

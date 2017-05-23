package com.pinetask.app.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.pinetask.app.R;
import com.pinetask.app.launch.StartupMessageDialogFragment;
import com.pinetask.app.launch.TutorialActivity;
import com.pinetask.app.chat.ChatFragment;
import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.ListSelectedEvent;
import com.pinetask.app.common.PineTaskActivity;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.common.UserMessage;
import com.pinetask.app.db.DbHelper;
import com.pinetask.app.db.ListLoader;
import com.pinetask.app.list_items.ListItemsFragment;
import com.pinetask.app.list_members.MembersFragment;
import com.pinetask.app.manage_lists.AddOrRenameListDialogFragment;
import com.pinetask.app.manage_lists.ManageListsActivity;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.pinetask.app.db.DbHelper.getUserNameObservable;
import static com.pinetask.app.db.DbHelper.getUserNameSingle;

public class MainActivity extends PineTaskActivity implements ViewPager.OnPageChangeListener
{
    FirebaseDatabase mDatabase;
    GoogleApiClient mGoogleApiClient;
    ListLoader mListLoader;
    String mUserId;
    ActionBarDrawerToggle mDrawerToggle;

    /** Identifies the active menu item from the bottom navigation drawer. **/
    int mActiveMenuItem=-1;

    /** The number of new chat messages received since the user last opened the Chat tab. **/
    int mNumUnreadChatMessages=0;

    @BindView(R.id.toolbar) Toolbar mToolBar;
    @BindView(R.id.listSpinner) Spinner mListSpinner;
    @BindView(R.id.viewPager) ViewPager mViewPager;
    @BindView(R.id.bottomNavigationView) BottomNavigationView mBottomNavigationView;
    @BindView(R.id.noListsFoundTextView) TextView mNoListsFoundTextView;
    @BindView(R.id.drawerLayout) DrawerLayout mDrawerLayout;
    @BindView(R.id.leftDrawerLayout) LinearLayout mLeftDrawerLayout;
    @BindView(R.id.userNameTextView) TextView mUserNameTextView;
    @BindView(R.id.settingsTextView) TextView mSettingsTextView;

    /** Adapter for the spinner that displays names of lists the user has access to. **/
    ArrayAdapter<PineTaskList> mListAdapter;

    /** InviteManager is in charge of sending invites to share a list, and processing invites received to grant access to someone else's list. **/
    InviteManager mInviteManager;
    public InviteManager getInviteManager() { return mInviteManager; }

    /** Activity request code for sending Firebase invite to share a list. **/
    public static int SEND_INVITE_REQUEST_CODE = 0;

    /** Activity request code for launching the Manage Lists Activity. **/
    public static int MANAGE_LISTS_REQUEST_CODE = 1;

    public static void launch(Context context)
    {
        Intent i = new Intent(context, MainActivity.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        // Register event bus
        PineTaskApplication application = (PineTaskApplication) getApplication();
        Bus bus = application.getEventBus();
        logMsg("Registering event bus");
        bus.register(this);

        // Create an auto-managed GoogleApiClient with access to App Invites.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, (@NonNull ConnectionResult connectionResult) -> logMsg("onConnectionFailed"))
                .build();

        // Create database connection
        logMsg("onCreate: creating database connection");
        mDatabase = FirebaseDatabase.getInstance();

        // Get current user's ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        mUserId = user.getUid();
        if (mUserId == null)
        {
            String msg = getString(R.string.user_not_signed_in);
            logMsg(msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            finish();
        }
        else
        {
            // Set user information for Crashlytics
            Crashlytics.setUserIdentifier(mUserId);

            // Check for and process any list invites that have been received.
            mInviteManager = new InviteManager(MainActivity.this, mGoogleApiClient, mDatabase, mUserId);
            mInviteManager.checkForInvites();

            // Set the Toolbar as the app bar for the activity.  Hide the default display of title text, since we show a custom spinner in the Toolbar.
            setSupportActionBar(mToolBar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // Initialize the ViewPager which hosts the "list items" and "chat messages" tabs.
            initViewPager();

            // Initialize the bottom navigation menu with choices for "List Items", "Chat", and "Members"
            initBottomNavigationMenu();

            // Populate spinner for choosing the list.  When done, it will start loading items in the currently selected list (or the 1st one if none was previously chosen).
            initListsSpinner();

            // Show username in side navigation drawer
            observe(getUserNameObservable(mUserId), (String name) ->  mUserNameTextView.setText(name));

            // Show startup message if it's a newer version than the user has previously seen.
            showStartupMessage();
        }

        // Initialize navigation drawer
        initNavigationDrawer();
    }

    private void initNavigationDrawer()
    {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolBar, R.string.drawer_open, R.string.drawer_close)
        {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view)
            {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView)
            {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerLayout.closeDrawer(mLeftDrawerLayout);
    }

    @Override
    public void onBackPressed()
    {
        if(mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // Close the navigation drawer if it's open and the activity is resuming
        if(mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        logMsg("onDestroy: shutting down event listeners");

        if (mListLoader != null) mListLoader.shutdown();

        mGoogleApiClient.stopAutoManage(this);
        mGoogleApiClient.disconnect();

        // Unregister event bus
        PineTaskApplication application = (PineTaskApplication)getApplication();
        Bus bus = application.getEventBus();
        logMsg("UnRegistering event bus");
        bus.unregister(this);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        logMsg("onNewIntent: %s", intent.toString());
        mInviteManager.checkForInvites();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        logMsg("onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == SEND_INVITE_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                mInviteManager.onInvitesSent(resultCode, data);
            }
            else if (resultCode != RESULT_CANCELED)
            {
                logError("onActivityResult: error sending invitation");
                showUserMessage(true, getString(R.string.error_sending_invite));
            }
        }
        else if (requestCode == MANAGE_LISTS_REQUEST_CODE)
        {
            if (resultCode == ManageListsActivity.NEW_LIST_SELECTED_RESULT_CODE)
            {
                PineTaskList list = (PineTaskList)data.getSerializableExtra(ManageListsActivity.LIST_KEY);
                logMsg("Switching to list %s selected by user on Manage Lists activity", list.getKey());
                displayListItems(list, false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }

        String currentListId = mPrefsManager.getCurrentListId();

        switch (item.getItemId())
        {
            case R.id.purgeCompletedItems:
                if (currentListId != null)
                {
                    PurgeCompletedItemsDialogFragment dialog = PurgeCompletedItemsDialogFragment.newInstance(currentListId);
                    getSupportFragmentManager().beginTransaction().add(dialog, PurgeCompletedItemsDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
                }
                else
                {
                    showUserMessage(true, getString(R.string.error_no_current_list));
                }
                return true;
            case R.id.exportList:
                if (currentListId != null)
                {
                    DbHelper.getListAsString(currentListId)
                            .subscribe(strList ->
                            {
                                Intent sendIntent = new Intent();
                                sendIntent.setAction(Intent.ACTION_SEND);
                                sendIntent.putExtra(Intent.EXTRA_TEXT, strList);
                                sendIntent.setType("text/plain");
                                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.export_list)));
                            }, ex ->
                            {
                                logException(ex);
                                showUserMessage(true, getString(R.string.error_exporting_list_x), ex.getMessage());
                            });
                }
                else
                {
                    showUserMessage(true, getString(R.string.error_no_current_list));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.helpTextView)
    public void helpOnClick(View view)
    {
        TutorialActivity.launch(this);
    }

    // User clicked "Manage Lists" from navigation drawer: Go to 'manage lists' activity.  It can return (via onActivityResult) a new list that was selected by the user.
    @OnClick(R.id.manageListsTextView)
    public void manageListsTextViewOnClick(View view)
    {
        Intent i = ManageListsActivity.buildLaunchIntent(this, mUserId);
        startActivityForResult(i, MANAGE_LISTS_REQUEST_CODE);
    }

    // User clicked "Settings" from navigation drawer: go to SettingsActivity
    @OnClick(R.id.settingsTextView)
    public void settingsTextViewOnClick(View view)
    {
        Intent i = SettingsActivity.getLaunchIntent(this, mUserId);
        startActivity(i);
    }

    @OnClick(R.id.aboutTextView)
    public void aboutOnClick(View view)
    {
        Intent i = new Intent(this, AboutActivity.class);
        startActivity(i);
    }

    /** Initialize the view pager, for switching between the three fragments:
     *  - List Items
     *  - Chat
     *  - Members
     **/
    private void initViewPager()
    {
        // Keep all three fragments active to avoid destroy/recreate overhead.
        mViewPager.setOffscreenPageLimit(2);

        mViewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager())
        {
            @Override
            public void destroyItem(ViewGroup container, int position, Object object)
            {
                super.destroyItem(container, position, object);
                logMsg("destroyItem pos=%d", position);
            }

            @Override
            public Fragment getItem(int position)
            {
                if (position==0)
                {
                    ListItemsFragment listItemsFragment = ListItemsFragment.newInstance(mUserId);
                    return listItemsFragment;
                }
                else if (position==1)
                {
                    ChatFragment chatFragment = ChatFragment.newInstance(mUserId);
                    return chatFragment;
                }
                else if (position==2)
                {
                    MembersFragment membersFragment = MembersFragment.newInstance(mUserId);
                    return membersFragment;
                }
                else
                {
                    throw new RuntimeException("initViewPager: getItem - invalid position " + position);
                }
            }

            @Override
            public int getCount()
            {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position)
            {
                if (position==0)
                {
                    return getString(R.string.list_items);
                }
                else if (position==1)
                {
                    return getString(R.string.chat_messages);
                }
                else if (position==2)
                {
                    return getString(R.string.members);
                }
                else
                {
                    throw new RuntimeException("initViewPager: getPageTitle - invalid position " + position);
                }
            }
        });

        mViewPager.addOnPageChangeListener(this);
    }

    /** Initialize the bottom navigation menu. **/
    private void initBottomNavigationMenu()
    {
        mBottomNavigationView.setOnNavigationItemSelectedListener((MenuItem item) -> {
            mActiveMenuItem = item.getItemId();
            int viewPosition = (mActiveMenuItem==R.id.listItemsMenuItem) ? 0 : (mActiveMenuItem==R.id.chatMenuItem) ? 1 : 2;
            mViewPager.setCurrentItem(viewPosition);
            return true;
        });
    }

    /** Hide soft keyboard **/
    private void hideSoftKeyboard()
    {
        View focusedView = getCurrentFocus();
        if (focusedView != null)
        {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }
    }

    /** Initialize spinner that displays all lists.  Create a ListLoader that will populate the spinner with the user's lists.  It will automatically refresh if any lists are added/deleted. **/
    private void initListsSpinner()
    {
        mListAdapter = new ArrayAdapter<>(this, R.layout.list_spinner, new ArrayList<PineTaskList>());
        mListSpinner.setAdapter(mListAdapter);
        mListAdapter.setDropDownViewResource(R.layout.lists_spinner_dropdown_item);

        mListLoader = new ListLoader(mUserId, new ListLoader.ListLoadCallback(){
            @Override
            public void onListsLoaded(List<PineTaskList> lists)
            {
                handleListsLoaded(lists);
            }

            @Override
            public void onLoadError()
            {
                showUserMessage(true, getString(R.string.error_loading_lists));
            }

            @Override
            public void onListDeleted(String listId)
            {
                handleListDeleted(listId);
            }

            @Override
            public void onListAdded(PineTaskList list)
            {
                handleListAdded(list);
            }
        });
    }

    /** Unless this is the first app launch, query the server for the startup message and its version.  If the user hasn't seen it yet, display the message now. **/
    private void showStartupMessage()
    {
        if (mPrefsManager.getIsFirstLaunch()) return;

        DbHelper.getStartupMessageIfUnread(mUserId)
                .subscribe(startupMessage ->
                {
                    if (mActivityActive)
                    {
                        StartupMessageDialogFragment dialog = StartupMessageDialogFragment.newInstance(startupMessage.text, startupMessage.version, mUserId);
                        getSupportFragmentManager().beginTransaction().add(dialog, StartupMessageDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
                    }
                }, ex ->
                {
                    logException(ex);
                    showUserMessage(true, getString(R.string.error_loading_startup_message));
                });
    }

    private void handleListsLoaded(List<PineTaskList> lists)
    {
        // Clear existing spinner contents.
        logMsg("initListsSpinner: loaded %d lists", lists.size());
        mListAdapter.clear();

        if (lists.size()==0)
        {
            if (mPrefsManager.getIsFirstLaunch())
            {
                // If this is the first app launch, show "Add List" dialog and pass the flag so that it prompts the user to create their first list.
                AddOrRenameListDialogFragment dialog = AddOrRenameListDialogFragment.newInstanceAddMode(mUserId, true);
                getSupportFragmentManager().beginTransaction().add(dialog, AddOrRenameListDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
                mPrefsManager.setIsFirstLaunch(false);
            }
        }
        else
        {
            // If the user was previously viewing a list, make sure it still exists.
            String previousListId = mPrefsManager.getCurrentListId();
            if ((previousListId != null) && (doesListIdExist(previousListId, lists)==false))
            {
                logMsg("Previous list %s no longer exists", previousListId);
                previousListId = null;
            }

            // Determine list to display:
            // - If an invite has just been accepted, switch to the list which was just added.
            // - If not, check SharedPreferences to find the ID of the list the user was previously viewing and use it.
            // - If none, then use the first list.
            String listIdToDisplay=null;
            if ((listIdToDisplay = mInviteManager.getAcceptedInviteListId()) != null)
            {
                logMsg("Will display list %s which was just added from invite", listIdToDisplay);
                mInviteManager.setAcceptedInviteListId(null);
            }
            else if ((listIdToDisplay = previousListId) != null)
            {
                logMsg("Will display previously displayed list %s", listIdToDisplay);
            }
            else
            {
                listIdToDisplay = lists.get(0).getKey();
                logMsg("User has no current list, using first owned list %s", listIdToDisplay);
            }

            // Add all lists to the spinner, setting the spinner selection to the list to be displayed.
            PineTaskList listToDisplay = null;
            for (int i=0;i<lists.size();i++)
            {
                PineTaskList l = lists.get(i);
                mListAdapter.add(l);
                if (l.getKey().equals(listIdToDisplay))
                {
                    mListSpinner.setSelection(i);
                    if (l.getKey().equals(listIdToDisplay)) listToDisplay = l;
                }
            }

            displayListItems(listToDisplay, true);
        }

        // Initialize callback for handling user selecting an item from the spinner (note: must be done after all items are already added to the spinner to prevent extra callback)
        initListSpinnerCallback();

        updateSpinnerAndTabVisibility();
    }

    /** Configure callback that runs when user selects a list from the lists spinner. **/
    private void initListSpinnerCallback()
    {
        logMsg("initListSpinnerCallback: setting onItemSelectedListener");
        mListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
            {
                // Switch to the list the user selected in the spinner.
                PineTaskList selectedList = mListAdapter.getItem(i);
                logMsg("User selected list '%s' [%s]", selectedList.getName(), selectedList.getKey());
                displayListItems(selectedList, false);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
    }

    private void handleListDeleted(String listId)
    {
        // Remove the list from the spinner mListAdapter
        for (int i=0;i<mListAdapter.getCount();i++)
        {
            PineTaskList list = mListAdapter.getItem(i);
            if (list.getKey().equals(listId))
            {
                logMsg("Removing list %s from spinner mListAdapter", listId);
                mListAdapter.remove(list);
                mListAdapter.notifyDataSetChanged();
                break;
            }
        }

        // If the current list has just been deleted, display the first available list (or no list if there aren't any)
        String currentListId = mPrefsManager.getCurrentListId();
        if (listId.equals(currentListId))
        {
            logMsg("Current list has been deleted");

            if (mListAdapter.getCount()>0)
            {
                mListSpinner.setSelection(0);
                PineTaskList firstList = mListAdapter.getItem(0);
                logMsg("Changing to display first available list %s (%s)", firstList.getKey(), firstList.getName());
                showUserMessage(false, getString(R.string.current_list_deleted_will_display_x), firstList.getName());
                displayListItems(firstList, false);
            }
            else
            {
                showUserMessage(false, getString(R.string.current_list_deleted));
                displayListItems(null, false);
            }
        }

        updateSpinnerAndTabVisibility();
    }

    private void handleListAdded(PineTaskList list)
    {
        // Add the list to the spinner mListAdapter.  Remove it first if it already exists (after a rename operation, list with the same key will already exist so must be removed first).
        logMsg("Adding list %s to spinner", list.getKey());
        mListAdapter.remove(list);
        mListAdapter.add(list);
        mListAdapter.sort(PineTaskList.NAME_COMPARATOR);

        if (list.getKey().equals(mInviteManager.getAcceptedInviteListId()))
        {
            // If a list invite was just accepted for this new list being added, automatically switch to it.
            logMsg("handleListAdded: Will display list %s which was just added from invite", list.getKey());
            mInviteManager.setAcceptedInviteListId(null);
            displayListItems(list, false);
        }
        else if (mListAdapter.getCount()==1)
        {
            // If there were no lists and we just added the first one, switch to it.
            logMsg("First list %s has been added, switching to it", list.getKey());
            mListSpinner.setSelection(0);
        }
        else
        {
            // Update selected spinner position to keep the previously active list, since the new item might have been inserted above it.
            String currentListId = mPrefsManager.getCurrentListId();
            int selectedPos = findSpinnerPositionForList(currentListId);
            logMsg("Resetting spinner selected position to %d for current list %s", selectedPos, currentListId);
            mListSpinner.setSelection(selectedPos);
        }

        updateSpinnerAndTabVisibility();
    }

    /** Searches the list of lists for one with the ID specified and returns value indicating if found or not. **/
    private boolean doesListIdExist(String listId, List<PineTaskList> lists)
    {
        for (PineTaskList l : lists)
        {
            if (l.getKey().equals(listId)) return true;
        }
        return false;
    }

    /** Set current list and store to prefs.  Then notify eventbus that new list has been selected. **/
    private void displayListItems(PineTaskList list, boolean isInitialDisplay)
    {
        String currentListId = mPrefsManager.getCurrentListId();
        String newListId = (list==null) ? null : list.getKey();
        boolean isCurrentlyDisplayedList = (currentListId==null) ? (newListId==null) : (currentListId.equals(newListId));
        if (!isInitialDisplay && isCurrentlyDisplayedList)
        {
            logMsg("displayItems: already displaying list %s, returning", newListId);
            return;
        }

        logMsg("displayListItems: Loading items in list %s", list==null ? null : list.getKey());

        // Set current list, and store it to shared prefs will be opened by default next time the app is run.
        mPrefsManager.setCurrentListId(newListId);

        // Set spinner selection to match displayed list, if not already.
        if (list != null)
        {
            int pos = mListAdapter.getPosition(list);
            logMsg("displayListItems: setting spinner to position %d to match displayed list", pos);
            mListSpinner.setSelection(pos);
        }

        // Notify eventbus of selected list (ListItemsFragment will display the new list)
        PineTaskApplication application = (PineTaskApplication) getApplication();
        Bus bus = application.getEventBus();
        bus.post(new ListSelectedEvent(newListId));
    }

    /** Called by the event bus when a UserMessage has been posted. **/
    @Subscribe
    public void userMessageRaised(UserMessage message)
    {
        showUserMessage(message.IsError, message.Message);
    }

    /** If the user has no lists, hide the list selection spinner and the bottom navigation menu. Otherwise, show them.
     *  If the user has no lists, show a message asking them to fromRef one. **/
    private void updateSpinnerAndTabVisibility()
    {
        int visibility = (mListAdapter.getCount()==0) ? View.GONE : View.VISIBLE;
        mListSpinner.setVisibility(visibility);
        mBottomNavigationView.setVisibility(visibility);
        mNoListsFoundTextView.setVisibility((mListAdapter != null && mListAdapter.getCount()==0) ? View.VISIBLE : View.GONE);
    }

    /** Returns the position of the specified list in the Lists spinner, or -1 if not found. **/
    private int findSpinnerPositionForList(String listId)
    {
        for (int i=0;i<mListAdapter.getCount();i++)
        {
            PineTaskList l = mListAdapter.getItem(i);
            if (l.getKey().equals(listId)) return i;
        }
        return -1;
    }

    /** Called by the event bus when a chat message is received.  If the chat tab is not currently active, look up username of the message sender.
     *  Then, show name and message in a Snackbar message, and update the number of unread messages on the Chat tab header. **/
    @Subscribe
    public void onChatMessageReceived(ChatMessage chatMessage)
    {
        if (mActiveMenuItem != R.id.chatMenuItem)
        {
            final String msg = chatMessage.getMessage();
            getUserNameSingle(chatMessage.getSenderId()).subscribe(singleObserver((String name) -> Snackbar.make(getWindow().getDecorView(), name + ": " + msg, Snackbar.LENGTH_LONG).show()));
            mNumUnreadChatMessages++;
            updateUnreadChatMessageCount();
        }
    }

    private void updateUnreadChatMessageCount()
    {
        MenuItem menuItem = mBottomNavigationView.getMenu().getItem(1);
        if (mNumUnreadChatMessages==0)
        {
            menuItem.setTitle(getString(R.string.chat));
        }
        else
        {
            menuItem.setTitle(String.format("%s (%d)", getString(R.string.chat), mNumUnreadChatMessages));
        }
    }


    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
    }

    @Override
    public void onPageSelected(int position)
    {
        // Set the checked state of the BottomNavigationView so that it shows the correct highlight for the now-active page.
        mBottomNavigationView.getMenu().getItem(position).setChecked(true);

        // When switching to the Chat fragment, clear the number of unread messages.
        if (position==1)
        {
            mNumUnreadChatMessages=0;
            updateUnreadChatMessageCount();
        }

        // Hide soft keyboard if it's open
        hideSoftKeyboard();
    }

    @Override
    public void onPageScrollStateChanged(int state)
    {
    }
}


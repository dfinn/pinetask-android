package com.pinetask.app.main;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.pinetask.app.R;
import com.pinetask.app.active_list_manager.ActiveListManager;
import com.pinetask.app.common.PineTaskInviteAlreadyUsedException;
import com.pinetask.app.launch.StartupMessageDialogFragment;
import com.pinetask.app.launch.TutorialActivity;
import com.pinetask.app.chat.ChatFragment;
import com.pinetask.app.chat.ChatMessage;
import com.pinetask.app.common.PineTaskActivity;
import com.pinetask.app.common.PineTaskApplication;
import com.pinetask.app.common.PineTaskList;
import com.pinetask.app.list_items.ListItemsFragment;
import com.pinetask.app.list_members.MembersFragment;
import com.pinetask.app.manage_lists.AddOrRenameListDialogFragment;
import com.pinetask.app.manage_lists.ManageListsActivity;
import com.pinetask.app.manage_lists.StartupMessage;
import com.squareup.otto.Subscribe;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends PineTaskActivity implements ViewPager.OnPageChangeListener, MainActivityView
{
    GoogleApiClient mGoogleApiClient;
    ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsActivityActive;
    ChatFragment mChatFragment;

    /** Identifies the active menu item from the bottom navigation drawer. **/
    int mActiveMenuItem=-1;

    /** The number of new chat messages received since the user last opened the Chat tab. **/
    int mNumUnreadChatMessages=0;

    @BindView(R.id.toolbar) Toolbar mToolBar;
    @BindView(R.id.listNameTextView) TextView mListNameTextView;
    @BindView(R.id.viewPager) ViewPager mViewPager;
    @BindView(R.id.bottomNavigationView) BottomNavigationView mBottomNavigationView;
    @BindView(R.id.noListsFoundTextView) TextView mNoListsFoundTextView;
    @BindView(R.id.drawerLayout) DrawerLayout mDrawerLayout;
    @BindView(R.id.leftDrawerLayout) LinearLayout mLeftDrawerLayout;
    @BindView(R.id.userNameTextView) TextView mUserNameTextView;
    @BindView(R.id.settingsTextView) TextView mSettingsTextView;

    /** Indices of the list items, chat, and members fragments within the viewpager. **/
    final int LIST_ITEMS_INDEX = 0;
    final int CHAT_INDEX = 1;
    final int MEMBERS_INDEX = 2;

    /** InviteManager is in charge of sending invites to share a list, and processing invites received to grant access to someone else's list. **/
    InviteManager mInviteManager;
    public InviteManager getInviteManager() { return mInviteManager; }

    /** Activity request code for sending Firebase invite to share a list. **/
    public static int SEND_INVITE_REQUEST_CODE = 0;

    /** Activity request code for launching the Manage Lists Activity. **/
    public static int MANAGE_LISTS_REQUEST_CODE = 1;

    /** Name of string extra specifying the user ID **/
    public static String USER_ID_KEY = "UserId";

    /** Name of a string extra specifying the ID of a chat message to highlight.  Used when launched from PendingIntent when user clicks item in notification drawer. **/
    public static String CHAT_MESSAGE_ID_KEY = "ChatMessageId";

    @Inject @Named("user_id") String mUserId;
    @Inject MainActivityPresenter mPresenter;
    @Inject ActiveListManager mActiveListManager;

    /** Launch the main activity, and create the Dagger components in the UserScope. **/
    public static Intent buildLaunchIntent(Context context, String userId, String chatMessageId)
    {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra(USER_ID_KEY, userId);
        i.putExtra(CHAT_MESSAGE_ID_KEY, chatMessageId);
        return i;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        ButterKnife.bind(this);

        // Create user scope dependencies if not done yet.
        if (PineTaskApplication.getInstance().getUserComponent() == null)
        {
            String userId = getIntent().getStringExtra(USER_ID_KEY);
            logMsg("Creating user component for user %s", userId);
            PineTaskApplication.getInstance().createUserComponent(userId);
        }

        // Inject UserScope dependencies
        PineTaskApplication.getInstance().getUserComponent().inject(this);

        // Register event bus
        logMsg("Registering event bus");
        mBus.register(this);

        // Create an auto-managed GoogleApiClient with access to App Invites.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(AppInvite.API)
                .enableAutoManage(this, (@NonNull ConnectionResult connectionResult) -> logMsg("onConnectionFailed"))
                .build();

        // Check for and process any list invites that have been received.
        mInviteManager = new InviteManager(MainActivity.this, mGoogleApiClient, mUserId);
        checkForInvites();

        // Set the Toolbar as the app bar for the activity.  Hide the default display of title text, since we show a custom spinner in the Toolbar.
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize the ViewPager which hosts the "list items" and "chat messages" tabs.
        initViewPager();

        // Initialize the bottom navigation menu with choices for "List Items", "Chat", and "Members"
        initBottomNavigationMenu();

        // Initialize navigation drawer
        initNavigationDrawer();

        // Attach presenter
        mPresenter.attach(this);
    }

    private void checkForInvites()
    {
        logMsg("Checking for pending invites");
        mInviteManager.checkForInvites().subscribe(list ->
        {
            showUserMessage(false, getString(R.string.access_granted_to_list_x), list.getName());
            mPresenter.onListSelected(list);
        }, ex ->
        {
            logError("Error accepting list invite");
            logException(ex);
            if (ex instanceof PineTaskInviteAlreadyUsedException) showError(getString(R.string.invite_already_used));
            else showError(getString(R.string.error_accepting_invite_see_log_for_details));
        });
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
    protected void onPause()
    {
        super.onPause();
        mIsActivityActive = false;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mIsActivityActive = true;
        // Close the navigation drawer if it's open and the activity is resuming
        if(mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START))
        {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
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

        logMsg("onDestroy: disconnecting googleApiClient");
        mGoogleApiClient.stopAutoManage(this);
        mGoogleApiClient.disconnect();

        logMsg("UnRegistering event bus");
        mBus.unregister(this);

        mPresenter.detach();

        // If finishing, destroy the Dagger components in the UserScope.
        if (isFinishing())
        {
            logMsg("onDestroy: isFinishing=true, destroying user scope");
            PineTaskApplication.getInstance().destroyUserComponent();
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        logMsg("onNewIntent: %s", intent.toString());
        checkForInvites();
        String chatMessageId = intent.getStringExtra(CHAT_MESSAGE_ID_KEY);
        if ((chatMessageId != null) && (mChatFragment != null))
        {
            mViewPager.setCurrentItem(CHAT_INDEX);
        }
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
                showError(getString(R.string.error_sending_invite));
            }
        }
        else if (requestCode == MANAGE_LISTS_REQUEST_CODE)
        {
            if (resultCode == ManageListsActivity.NEW_LIST_SELECTED_RESULT_CODE)
            {
                PineTaskList list = (PineTaskList)data.getSerializableExtra(ManageListsActivity.LIST_KEY);
                logMsg("Switching to list %s selected by user on Manage Lists activity", list.getKey());
                mPresenter.onListSelected(list);
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
                mPresenter.onPurgeCompletedItemsSelected();
                return true;
            case R.id.exportList:
                if (currentListId != null)
                {
                    mDbHelper.getListAsString(currentListId)
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
                                showError(getString(R.string.error_exporting_list_x), ex.getMessage());
                            });
                }
                else
                {
                    showError(getString(R.string.error_no_current_list));
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
                if (position == LIST_ITEMS_INDEX)
                {
                    ListItemsFragment listItemsFragment = ListItemsFragment.newInstance(mUserId);
                    return listItemsFragment;
                }
                else if (position == CHAT_INDEX)
                {
                    mChatFragment = ChatFragment.newInstance();
                    return mChatFragment;
                }
                else if (position == MEMBERS_INDEX)
                {
                    MembersFragment membersFragment = MembersFragment.newInstance();
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
                if (position == LIST_ITEMS_INDEX)
                {
                    return getString(R.string.list_items);
                }
                else if (position == CHAT_INDEX)
                {
                    return getString(R.string.chat_messages);
                }
                else if (position == MEMBERS_INDEX)
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
        mBottomNavigationView.setOnNavigationItemSelectedListener((MenuItem item) ->
        {
            mActiveMenuItem = item.getItemId();
            int viewPosition = (mActiveMenuItem==R.id.listItemsMenuItem) ? 0 : (mActiveMenuItem==R.id.chatMenuItem) ? 1 : 2;
            mViewPager.setCurrentItem(viewPosition);
            return true;
        });
    }

    @Override
    public void showAddListDialog()
    {
        AddOrRenameListDialogFragment dialog = AddOrRenameListDialogFragment.newInstanceAddMode(mUserId, true);
        getSupportFragmentManager().beginTransaction().add(dialog, AddOrRenameListDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
    }

    @Override
    public void showListChooser(List<PineTaskList> lists)
    {
        ArrayAdapter<PineTaskList> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lists);
        new AlertDialog.Builder(this).setAdapter(adapter, (dialogInterface, i) ->
        {
            PineTaskList selectedList = adapter.getItem(i);
            logMsg("User selected list %s (%s)", selectedList.getKey(), selectedList.getName());
            mPresenter.onListSelected(selectedList);
        }).create().show();
    }

    @Override
    public void showNoListsFoundMessage()
    {
        mNoListsFoundTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideNoListsFoundMessage()
    {
        mNoListsFoundTextView.setVisibility(View.GONE);
    }

    @Override
    public void showBottomMenuBar()
    {
        mBottomNavigationView.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideBottomMenuBar()
    {
        mBottomNavigationView.setVisibility(View.GONE);
    }

    /** Called by the event bus when a chat message is received.   **/
    @Subscribe
    public void onChatMessageReceived(ChatMessage chatMessage)
    {
        if ((! mIsActivityActive))
        {
            // Raise system notification if app is currently in the background.  Clicking the notification will open the app and go to the chat tab.
            logMsg("onChatMessageReceived: building pending intent for chat message %s", chatMessage.getId());
            Intent intent = MainActivity.buildLaunchIntent(this, mUserId, chatMessage.getId());
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.launcher_icon)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setVibrate(new long[] {0, 500, 500, 500})
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentTitle(String.format(getString(R.string.message_from_x), chatMessage.getSenderName()))
                    .setContentText(chatMessage.getMessage());
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }
        else if (mViewPager.getCurrentItem() != CHAT_INDEX)
        {
            // Show pop-up message and play sound
            Toast.makeText(this, chatMessage.getSenderName() + ": " + chatMessage.getMessage(), Toast.LENGTH_SHORT).show();
            mNumUnreadChatMessages++;
            updateUnreadChatMessageCount();
        }
    }

    @OnClick(R.id.listNameTextView)
    public void onListNameClicked(View view)
    {
        mPresenter.onListSelectorClicked();
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

    @Override
    public void showUserName(String userName)
    {
        mUserNameTextView.setText(userName);
    }

    @Override
    public void showStartupMessage(StartupMessage startupMessage)
    {
        StartupMessageDialogFragment dialog = StartupMessageDialogFragment.newInstance(startupMessage.text, startupMessage.version, mUserId);
        getSupportFragmentManager().beginTransaction().add(dialog, StartupMessageDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
    }

    @Override
    public void showError(String message, Object... args)
    {
        showUserMessage(false, message, args);
    }

    @Override
    public void showErrorAndExit(String message, Object... args)
    {
        showUserMessage(true, message, args);
    }

    @Override
    public void showCurrentListName(String listName)
    {
        mListNameTextView.setText(listName);
    }

    @Override
    public void showPurgeCompletedItemsDialog(String listId, String listName)
    {
        PurgeCompletedItemsDialogFragment dialog = PurgeCompletedItemsDialogFragment.newInstance(listId, listName);
        getSupportFragmentManager().beginTransaction().add(dialog, PurgeCompletedItemsDialogFragment.class.getSimpleName()).commitAllowingStateLoss();
    }
}


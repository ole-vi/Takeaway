package org.ole.planet.myplanet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayout;

import org.ole.planet.myplanet.Data.realm_UserModel;
import org.ole.planet.myplanet.Data.realm_meetups;
import org.ole.planet.myplanet.Data.realm_myCourses;
import org.ole.planet.myplanet.Data.realm_myLibrary;
import org.ole.planet.myplanet.Data.realm_myTeams;
import org.ole.planet.myplanet.Data.realm_submissions;
import org.ole.planet.myplanet.base.BaseContainerFragment;
import org.ole.planet.myplanet.courses.TakeCourseFragment;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.library.LibraryDetailFragment;
import org.ole.planet.myplanet.mymeetup.MyMeetupDetailFragment;
import org.ole.planet.myplanet.survey.SurveyFragment;
import org.ole.planet.myplanet.teams.MyTeamsDetailFragment;
import org.ole.planet.myplanet.userprofile.UserProfileDbHandler;
import org.ole.planet.myplanet.userprofile.UserProfileFragment;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

/**
 * A placeholder fragment containing a simple view.
 */
public class DashboardFragment extends BaseContainerFragment {

    public static final String PREFS_NAME = "OLE_PLANET";
    TextView txtFullName, txtVisits, tv_surveys;
    String fullName;
    Realm mRealm;
    DatabaseService dbService;
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            250,
            100
    );
    private UserProfileDbHandler profileDbHandler;


    public DashboardFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        profileDbHandler = new UserProfileDbHandler(getActivity());
        declareElements(view);
        fullName = profileDbHandler.getUserModel().getName();
        txtFullName.setText(fullName);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(Utilities.currentDate());
        realm_UserModel model = mRealm.copyToRealmOrUpdate(profileDbHandler.getUserModel());
        ImageView imageView = view.findViewById(R.id.imageView);
        Utilities.loadImage(model.getUserImage(), imageView);
        txtVisits.setText(profileDbHandler.getOfflineVisits() + " visits");
        int noOfSurvey = realm_submissions.getNoOfSurveySubmissionByUser(settings.getString("userId", "--"), mRealm);
        (view.findViewById(R.id.img_survey_warn)).setVisibility(noOfSurvey == 0 ? View.VISIBLE : View.GONE);
        return view;
    }

    private void declareElements(View view) {
        txtFullName = view.findViewById(R.id.txtFullName);
        txtVisits = view.findViewById(R.id.txtVisits);
        tv_surveys = view.findViewById(R.id.tv_surveys);
        tv_surveys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                homeItemClickListener.openCallFragment(new SurveyFragment());
            }
        });
        view.findViewById(R.id.ll_user).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                homeItemClickListener.openCallFragment(new UserProfileFragment());
            }
        });
        dbService = new DatabaseService(getActivity());
        mRealm = dbService.getRealmInstance();
        myLibraryDiv(view);
        initializeFlexBoxView(view, R.id.flexboxLayoutCourse, realm_myCourses.class);
        initializeFlexBoxView(view, R.id.flexboxLayoutTeams, realm_myTeams.class);
        initializeFlexBoxView(view, R.id.flexboxLayoutMeetups, realm_meetups.class);
        showDownloadDialog(getLibraryList());
    }

    public void myLibraryDiv(View view) {
        TextView count = view.findViewById(R.id.count_library);
        FlexboxLayout flexboxLayout = view.findViewById(R.id.flexboxLayout);
        flexboxLayout.setFlexDirection(FlexDirection.ROW);
        RealmResults<realm_myLibrary> db_myLibrary = mRealm.where(realm_myLibrary.class).isNotEmpty("userId")
                .equalTo("userId", settings.getString("userId", "--"), Case.INSENSITIVE).findAll();
        count.setText(db_myLibrary.size() + "");
        TextView[] myLibraryTextViewArray = new TextView[db_myLibrary.size()];
        int itemCnt = 0;
        for (final realm_myLibrary items : db_myLibrary) {
            setTextViewProperties(myLibraryTextViewArray, itemCnt, items);
            myLibraryItemClickAction(myLibraryTextViewArray[itemCnt], items);
            if ((itemCnt % 2) == 0) {
                myLibraryTextViewArray[itemCnt].setBackgroundResource(R.drawable.light_rect);
            }
            flexboxLayout.addView(myLibraryTextViewArray[itemCnt], params);
            itemCnt++;
        }
    }

    public void initializeFlexBoxView(View v, int id, Class c) {
        FlexboxLayout flexboxLayout = v.findViewById(id);
        flexboxLayout.setFlexDirection(FlexDirection.ROW);
        setUpMyList(c, flexboxLayout, v);
    }

    public void setUpMyList(Class c, FlexboxLayout flexboxLayout, View view) {
        RealmResults<RealmObject> db_myCourses = mRealm.where(c).isNotEmpty("userId")
                .equalTo("userId", settings.getString("userId", "--"), Case.INSENSITIVE).findAll();
        setCountText(db_myCourses.size(), c, view);
        TextView[] myCoursesTextViewArray = new TextView[db_myCourses.size()];
        int itemCnt = 0;
        for (final RealmObject items : db_myCourses) {
            setTextViewProperties(myCoursesTextViewArray, itemCnt, items);
            if ((itemCnt % 2) == 0) {
                myCoursesTextViewArray[itemCnt].setBackgroundResource(R.drawable.light_rect);
            }
            flexboxLayout.addView(myCoursesTextViewArray[itemCnt], params);
            itemCnt++;
        }
    }


    public void setTextViewProperties(TextView[] textViewArray, int itemCnt, final RealmObject obj) {
        textViewArray[itemCnt] = new TextView(getContext());
        textViewArray[itemCnt].setPadding(20, 10, 20, 10);
        textViewArray[itemCnt].setBackgroundResource(R.drawable.dark_rect);
        textViewArray[itemCnt].setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textViewArray[itemCnt].setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        textViewArray[itemCnt].setTextColor(getResources().getColor(R.color.dialog_sync_labels));
        if (obj instanceof realm_myLibrary) {
            textViewArray[itemCnt].setText(((realm_myLibrary) obj).getTitle());
        } else if (obj instanceof realm_myCourses) {
            handleClick(((realm_myCourses) obj).getCourseId(), ((realm_myCourses) obj).getCourseTitle(), new TakeCourseFragment(), textViewArray[itemCnt]);
        } else if (obj instanceof realm_myTeams) {
            //    textViewArray[itemCnt].setText(((realm_myTeams) obj).getName());
            handleClick(((realm_myTeams) obj).getTeamId(), ((realm_myTeams) obj).getName(), new MyTeamsDetailFragment(), textViewArray[itemCnt]);
        } else if (obj instanceof realm_meetups) {
            handleClick(((realm_meetups) obj).getMeetupId(), ((realm_meetups) obj).getTitle(), new MyMeetupDetailFragment(), textViewArray[itemCnt]);
        }
    }

    private void handleClick(final String id, String title, final Fragment f, TextView v) {
        v.setText(title);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (homeItemClickListener != null) {
                    Bundle b = new Bundle();
                    b.putString("id", id);
                    f.setArguments(b);
                    homeItemClickListener.openCallFragment(f);
                }
            }
        });
    }

    private RealmResults<realm_myLibrary> getLibraryList() {
        return mRealm.where(realm_myLibrary.class)
                .equalTo("resourceOffline", false)
                .isNotEmpty("userId")
                .equalTo("userId", settings.getString("userId", "--"), Case.INSENSITIVE)
                .or()
                .equalTo("resourceOffline", false)
                .isNotEmpty("courseId")
                .equalTo("userId", settings.getString("userId", "--"), Case.INSENSITIVE)
                .findAll();
    }


    public void myLibraryItemClickAction(TextView textView, final realm_myLibrary items) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fragment f = new LibraryDetailFragment();
                Bundle b = new Bundle();
                b.putString("libraryId", items.getResource_id());
                f.setArguments(b);
                homeItemClickListener.openCallFragment(f);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        profileDbHandler.onDestory();
    }

    public void setCountText(int countText, Class c, View v) {
        if (c == realm_myCourses.class)
            ((TextView) v.findViewById(R.id.count_course)).setText(countText + "");
        else if (c == realm_meetups.class)
            ((TextView) v.findViewById(R.id.count_meetup)).setText(countText + "");
        else if (c == realm_myTeams.class)
            ((TextView) v.findViewById(R.id.count_team)).setText(countText + "");
    }
}

package org.ole.planet.myplanet.ui.library;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.base.BaseContainerFragment;
import org.ole.planet.myplanet.callback.OnRatingChangeListener;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmMyLibrary;
import org.ole.planet.myplanet.model.RealmRating;
import org.ole.planet.myplanet.model.RealmRemovedLog;
import org.ole.planet.myplanet.utilities.Constants;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.Arrays;

import io.realm.Realm;


public class LibraryDetailFragment extends BaseContainerFragment implements OnRatingChangeListener {
    TextView author, pubishedBy, title, media, subjects, license, language, resource, type;
    Button download, remove;
    String libraryId;
    DatabaseService dbService;
    Realm mRealm;
    RealmMyLibrary library;

    public LibraryDetailFragment() {
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            libraryId = getArguments().getString("libraryId");
        }
    }

    @Override
    public void onDownloadComplete() {
        download.setText("Open Resource");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_library_detail, container, false);
        dbService = new DatabaseService(getActivity());
        mRealm = dbService.getRealmInstance();
        Utilities.log("Library id " + libraryId);
        initView(v);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        library = mRealm.where(RealmMyLibrary.class).equalTo("resourceId", libraryId).findFirst();
        setLibraryData();
    }

    private void initView(View v) {
        TextView average, tv_rating;
        LinearLayout llRating;

        author = v.findViewById(R.id.tv_author);
        title = v.findViewById(R.id.tv_title);
        pubishedBy = v.findViewById(R.id.tv_published);
        media = v.findViewById(R.id.tv_media);
        subjects = v.findViewById(R.id.tv_subject);
        language = v.findViewById(R.id.tv_language);
        license = v.findViewById(R.id.tv_license);
        resource = v.findViewById(R.id.tv_resource);
        type = v.findViewById(R.id.tv_type);
        download = v.findViewById(R.id.btn_download);
        remove = v.findViewById(R.id.btn_remove);
        llRating = v.findViewById(R.id.ll_rating);
        llRating.setVisibility(Constants.showBetaFeature(Constants.KEY_RATING, getActivity()) ? View.VISIBLE : View.GONE);
        average = v.findViewById(R.id.average);
        average.setVisibility(Constants.showBetaFeature(Constants.KEY_RATING, getActivity()) ? View.VISIBLE : View.GONE);
        tv_rating = v.findViewById(R.id.tv_rating);
        tv_rating.setVisibility(Constants.showBetaFeature(Constants.KEY_RATING, getActivity()) ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.ll_rating).setOnClickListener(view -> homeItemClickListener.showRatingDialog("resource", library.getResource_id(), library.getTitle(), LibraryDetailFragment.this));
        initRatingView(v);
    }

    private void setLibraryData() {
        title.setText(library.getTitle());
        author.setText(library.getAuthor());
        pubishedBy.setText(library.getPublisher());
        media.setText(library.getMediaType());
        subjects.setText(library.getSubjectsAsString());
        language.setText(library.getLanguage());
        license.setText(library.getLinkToLicense());
        resource.setText(RealmMyLibrary.listToString(library.getResourceFor()));
        profileDbHandler.setResourceOpenCount(library);
        try {
            onRatingChanged();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        download.setVisibility(TextUtils.isEmpty(library.getResourceLocalAddress()) ? View.GONE : View.VISIBLE);
        download.setText(library.getResourceOffline() == null || library.getResourceOffline() ? "Open Resource " : "Download Resource");
        if (FileUtils.getFileExtension(library.getResourceLocalAddress()).equals("mp4")) {
            download.setText("Open Video");
        }
        setClickListeners();
    }


    public void setClickListeners() {

        download.setOnClickListener(view -> {
            if (TextUtils.isEmpty(library.getResourceLocalAddress())) {
                Toast.makeText(getActivity(), "Link not available", Toast.LENGTH_LONG).show();
                return;
            }
            openResource(library);
        });
        boolean isAdd = !library.getUserId().contains(profileDbHandler.getUserModel().getId());
        remove.setText(isAdd ? "Add To My Library" : "Remove from myLibrary");
        remove.setOnClickListener(view -> {
            if (!mRealm.isInTransaction())
                mRealm.beginTransaction();
            if (isAdd) {
                library.setUserId(profileDbHandler.getUserModel().getId());
                RealmRemovedLog.onAdd(mRealm, "resources", profileDbHandler.getUserModel().getId(), libraryId);

            } else {
                library.removeUserId(profileDbHandler.getUserModel().getId());
                RealmRemovedLog.onRemove(mRealm, "resources", profileDbHandler.getUserModel().getId(), libraryId);
            }
            Utilities.toast(getActivity(), "Resource " + (isAdd ? " added to" : " removed from ") + " my library");
            setLibraryData();
        });
    }

    @Override
    public void onRatingChanged() {
        JsonObject object = RealmRating.getRatingsById(mRealm, "resource", library.getResource_id());
        setRatings(object);
    }
}

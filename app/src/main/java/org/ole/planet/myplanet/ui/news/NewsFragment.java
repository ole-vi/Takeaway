package org.ole.planet.myplanet.ui.news;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.base.BaseNewsFragment;
import org.ole.planet.myplanet.callback.SuccessListener;
import org.ole.planet.myplanet.datamanager.DatabaseService;
import org.ole.planet.myplanet.model.RealmNews;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UploadManager;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.library.AddResourceActivity;
import org.ole.planet.myplanet.utilities.Constants;
import org.ole.planet.myplanet.utilities.FileUtils;
import org.ole.planet.myplanet.utilities.KeyboardUtils;
import org.ole.planet.myplanet.utilities.NetworkUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Case;
import io.realm.Sort;

import static android.app.Activity.RESULT_OK;

public class NewsFragment extends BaseNewsFragment {


    RecyclerView rvNews;
    EditText etMessage;
    TextInputLayout tlMessage;
    Button btnSubmit, btnAddImage;
    RealmUserModel user;
    TextView tvMessage;
    AdapterNews adapterNews;
    String imageName = "", imageUrl = "";
    ImageView thumb;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news, container, false);
        rvNews = v.findViewById(R.id.rv_news);
        etMessage = v.findViewById(R.id.et_message);
        tlMessage = v.findViewById(R.id.tl_message);
        btnSubmit = v.findViewById(R.id.btn_submit);
        tvMessage = v.findViewById(R.id.tv_message);
        thumb = v.findViewById(R.id.thumb);
        btnAddImage = v.findViewById(R.id.add_news_image);
//        btnShowMain = v.findViewById(R.id.btn_main);
        mRealm = new DatabaseService(getActivity()).getRealmInstance();
        user = new UserProfileDbHandler(getActivity()).getUserModel();
        KeyboardUtils.setupUI(v.findViewById(R.id.news_fragment_parent_layout), getActivity());
        return v;
    }



    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<RealmNews> list = mRealm.where(RealmNews.class).sort("time", Sort.DESCENDING)
                .equalTo("docType", "message", Case.INSENSITIVE)
                .equalTo("viewableBy", "community", Case.INSENSITIVE)
                .equalTo("replyTo", "", Case.INSENSITIVE)
                .findAll();

        setData(list);
        btnSubmit.setOnClickListener(view -> {
            String message = etMessage.getText().toString();
            if (message.isEmpty()) {
                tlMessage.setError("Please enter message");
                return;
            }
            etMessage.setText("");
            HashMap<String, String> map = new HashMap<>();
            map.put("message", message);
            map.put("viewableBy", "community");
            map.put("viewableId", "");
            map.put("imageUrl", imageUrl);
            map.put("imageName", imageName);
//            if (!TextUtils.isEmpty(imageUrl)) {
//                createImage(map);
//            }else{
                RealmNews.createNews(map, mRealm, user);
//            }
            rvNews.getAdapter().notifyDataSetChanged();
        });
        btnAddImage.setOnClickListener(v -> FileUtils.openOleFolder(this));
        btnAddImage.setVisibility(Constants.showBetaFeature(Constants.KEY_NEWSADDIMAGE, getActivity()) ? View.VISIBLE : View.GONE);
    }

    public void setData(List<RealmNews> list) {
        rvNews.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapterNews = new AdapterNews(getActivity(), list, user, null);
        adapterNews.setmRealm(mRealm);
        adapterNews.setListener(this);
        adapterNews.registerAdapterDataObserver(observer);
        rvNews.setAdapter(adapterNews);
        showNoData(tvMessage, adapterNews.getItemCount());
    }

    final private RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            showNoData(tvMessage, adapterNews.getItemCount());
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            showNoData(tvMessage, adapterNews.getItemCount());
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            showNoData(tvMessage, adapterNews.getItemCount());
        }
    };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri url = null;
            String path = "";
            url = data.getData();
            path = FileUtils.getRealPathFromURI(getActivity(), url);
            if (TextUtils.isEmpty(path)) {
                path = getImagePath(url);
            }
            imageUrl = path;
            imageName = FileUtils.getFileNameFromUrl(path);
            try {
                thumb.setVisibility(View.VISIBLE);
                Glide.with(getActivity())
                        .load(new File(imageUrl))
                        .into(thumb);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public String getImagePath(Uri uri) {
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContext().getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }


}
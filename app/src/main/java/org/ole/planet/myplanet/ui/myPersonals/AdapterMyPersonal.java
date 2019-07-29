package org.ole.planet.myplanet.ui.myPersonals;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.model.RealmMyPersonal;
import org.ole.planet.myplanet.model.RealmNews;
import org.ole.planet.myplanet.model.RealmUserModel;
import org.ole.planet.myplanet.service.UserProfileDbHandler;
import org.ole.planet.myplanet.ui.news.AdapterNews;
import org.ole.planet.myplanet.ui.userprofile.AdapterOtherInfo;
import org.ole.planet.myplanet.utilities.TimeUtils;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.List;

import io.realm.Realm;

public class AdapterMyPersonal extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<RealmMyPersonal> list;
    private Realm realm;

    public AdapterMyPersonal(Context context, List<RealmMyPersonal> list) {
        this.context = context;
        this.list = list;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.row_my_personal, parent, false);
        return new ViewHolderMyPersonal(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderMyPersonal) {
            ((ViewHolderMyPersonal) holder).title.setText(list.get(position).getTitle());
            ((ViewHolderMyPersonal) holder).description.setText(list.get(position).getDescription());
            ((ViewHolderMyPersonal) holder).date.setText(TimeUtils.getFormatedDate(list.get(position).getDate()));
            ((ViewHolderMyPersonal) holder).ivDelete.setOnClickListener(view -> new AlertDialog.Builder(context).setMessage(R.string.delete_record)
                    .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                        if (!realm.isInTransaction())
                            realm.beginTransaction();
                        RealmMyPersonal personal = realm.where(RealmMyPersonal.class).equalTo("_id", list.get(position).get_id()).findFirst();
                        personal.deleteFromRealm();
                        realm.commitTransaction();
                        notifyDataSetChanged();
                    }).setNegativeButton(R.string.cancel, null).show());

            ((ViewHolderMyPersonal) holder).ivEdit.setOnClickListener(view -> {
                editPersonal(list.get(position));
            });
        }
    }

    private void editPersonal(RealmMyPersonal personal) {
        View v = LayoutInflater.from(context).inflate(R.layout.alert_my_personal, null);
        EditText etTitle = v.findViewById(R.id.et_title);
        EditText etDesc = v.findViewById(R.id.et_description);
        etDesc.setText(personal.getDescription());
        etTitle.setText(personal.getTitle());
        new AlertDialog.Builder(context).setTitle("Edit Personal").setIcon(R.drawable.ic_edit)
                .setView(v)
                .setPositiveButton(R.string.button_submit, (dialogInterface, i) -> {
                    String title = etTitle.getText().toString();
                    String desc = etDesc.getText().toString();
                    if (title.isEmpty()) {
                        Utilities.toast(context, "Please enter title");
                        return;
                    }
                    if (!realm.isInTransaction())
                        realm.beginTransaction();
                    personal.setDescription(desc);
                    personal.setTitle(title);
                    realm.commitTransaction();
                    notifyDataSetChanged();
                }).setNegativeButton(R.string.cancel, null).show();


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolderMyPersonal extends RecyclerView.ViewHolder {
        TextView title, description, date;
        ImageView ivEdit, ivDelete;

        public ViewHolderMyPersonal(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            date = itemView.findViewById(R.id.date);
            ivDelete = itemView.findViewById(R.id.img_delete);
            ivEdit = itemView.findViewById(R.id.img_edit);
        }
    }
}

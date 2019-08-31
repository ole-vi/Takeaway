package org.ole.planet.myplanet.ui.team.teamTask;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.model.RealmTeamTask;
import org.ole.planet.myplanet.model.RealmUserModel;

import java.util.List;

import io.realm.Realm;

public class AdapterTask extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<RealmTeamTask> list;
    private OnCompletedListener listener;
    private Realm realm;

    interface OnCompletedListener {
        void onCheckChange(RealmTeamTask realmTeamTask, boolean b);

        void onClickMore(RealmTeamTask realmTeamTask);
    }

    public AdapterTask(Context context, Realm mRealm, List<RealmTeamTask> list) {
        this.context = context;
        this.list = list;
        this.realm = mRealm;

    }

    public void setListener(OnCompletedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.row_task, parent, false);
        return new ViewHolderTask(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderTask) {
            ((ViewHolderTask) holder).completed.setText(list.get(position).getTitle());
            ((ViewHolderTask) holder).completed.setChecked(list.get(position).isCompleted());
            ((ViewHolderTask) holder).deadline.setText("Deadline : " + list.get(position).getDeadline());
            if (!TextUtils.isEmpty(list.get(position).getAssignee())) {
                RealmUserModel model = realm.where(RealmUserModel.class).equalTo("_id", list.get(position).getAssignee()).findFirst();
                if (model != null) {
                    ((ViewHolderTask) holder).assignee.setText("Assigned to : " + model.getName());
                }
            }else{
                ((ViewHolderTask) holder).assignee.setText("No assignee");

            }
            ((ViewHolderTask) holder).completed.setOnCheckedChangeListener((compoundButton, b) -> {
                if (listener != null)
                    listener.onCheckChange(list.get(position), b);
            });
            ((ViewHolderTask) holder).icMore.setOnClickListener(view -> {
                if (listener != null)
                    listener.onClickMore(list.get(position));
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolderTask extends RecyclerView.ViewHolder {
        CheckBox completed;
        TextView deadline, assignee;
        ImageView icMore;

        public ViewHolderTask(View itemView) {
            super(itemView);
            completed = itemView.findViewById(R.id.checkbox);
            deadline = itemView.findViewById(R.id.deadline);
            assignee = itemView.findViewById(R.id.assignee);
            icMore = itemView.findViewById(R.id.ic_more);
        }
    }
}

package org.ole.planet.myplanet.courses;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import org.ole.planet.myplanet.Data.realm_myCourses;
import org.ole.planet.myplanet.Data.realm_myLibrary;
import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.callback.OnCourseItemSelected;
import org.ole.planet.myplanet.callback.OnHomeItemClickListener;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

public class AdapterCourses extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private List<realm_myCourses> courseList;
    private List<realm_myCourses> selectedItems;
    private OnCourseItemSelected listener;
    private OnHomeItemClickListener homeItemClickListener;

    public AdapterCourses(Context context, List<realm_myCourses> courseList) {
        this.context = context;
        this.courseList = courseList;
        this.selectedItems = new ArrayList<>();
        if (context instanceof OnHomeItemClickListener) {
            homeItemClickListener = (OnHomeItemClickListener) context;
        }
    }

    public void setCourseList(List<realm_myCourses> courseList) {
        this.courseList = courseList;
        notifyDataSetChanged();
    }


    public void setListener(OnCourseItemSelected listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.row_course, parent, false);
        return new ViewHoldercourse(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof ViewHoldercourse) {
            ((ViewHoldercourse) holder).title.setText((position + 1) + ". " + courseList.get(position).getCourseTitle());
            ((ViewHoldercourse) holder).desc.setText(courseList.get(position).getDescription());
            ((ViewHoldercourse) holder).grad_level.setText("Grad Level  : " + courseList.get(position).getGradeLevel());
            ((ViewHoldercourse) holder).subject_level.setText("Subject Level : " + courseList.get(position).getSubjectLevel());
            ((ViewHoldercourse) holder).checkBox.setChecked(selectedItems.contains(courseList.get(position)));
            if (courseList.get(position) != null) {
                ((ViewHoldercourse) holder).progressBar.setMax(courseList.get(position).getnumberOfSteps());
            }
            ((ViewHoldercourse) holder).checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (listener != null) {
                        Utilities.handleCheck(b, position, (ArrayList) selectedItems, courseList);
                        listener.onSelectedListChange(selectedItems);
                    }

                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    if (homeItemClickListener != null) {
                        Fragment f = new TakeCourseFragment();
                        Bundle b = new Bundle();
                        b.putString("id", courseList.get(position).getCourseId());
                        f.setArguments(b);
                        homeItemClickListener.openCallFragment(f);
                    }
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    class ViewHoldercourse extends RecyclerView.ViewHolder {
        TextView title, desc, grad_level, subject_level;
        CheckBox checkBox;
        ContentLoadingProgressBar progressBar;

        public ViewHoldercourse(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.description);
            grad_level = itemView.findViewById(R.id.grad_level);
            subject_level = itemView.findViewById(R.id.subject_level);
            checkBox = itemView.findViewById(R.id.checkbox);
            progressBar = itemView.findViewById(R.id.progress);
        }
    }
}
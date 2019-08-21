package org.ole.planet.myplanet.ui.team;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.utilities.TimeUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlanFragment extends BaseTeamFragment {

    TextView description, date;
    public PlanFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_plan, container, false);
        description = v.findViewById(R.id.tv_description);
        date = v.findViewById(R.id.tv_date);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (team!=null){
            description.setText(team.getDescription());
            date.setText(TimeUtils.formatDate(team.getCreatedDate()));
        }
    }
}

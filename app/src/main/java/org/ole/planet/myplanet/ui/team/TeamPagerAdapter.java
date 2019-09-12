package org.ole.planet.myplanet.ui.team;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import org.ole.planet.myplanet.MainApplication;
import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.model.RealmMyTeam;
import org.ole.planet.myplanet.ui.enterprises.FinanceFragment;
import org.ole.planet.myplanet.ui.team.teamCourse.TeamCourseFragment;
import org.ole.planet.myplanet.ui.team.teamDiscussion.DiscussionListFragment;
import org.ole.planet.myplanet.ui.team.teamMember.JoinedMemberFragment;
import org.ole.planet.myplanet.ui.team.teamMember.MembersFragment;
import org.ole.planet.myplanet.ui.team.teamResource.TeamResourceFragment;
import org.ole.planet.myplanet.ui.team.teamTask.TeamTaskFragment;

import java.util.ArrayList;
import java.util.List;

public class TeamPagerAdapter extends FragmentStatePagerAdapter {

    private String teamId;
    private List<String> list;
    private boolean isEnterprise;
    private boolean ismyLife;

    public TeamPagerAdapter(FragmentManager fm, RealmMyTeam team, boolean isMyTeam) {
        super(fm);
        this.teamId = team.getId();
        isEnterprise = team.getType().equals("enterprise");
        list = new ArrayList<>();
        ismyLife = isMyTeam;
        list.add(MainApplication.context.getString(isEnterprise ? R.string.mission : R.string.plan));
        list.add(MainApplication.context.getString(isEnterprise ? R.string.team : R.string.joined_members));
        if (isMyTeam) {
            list.add(MainApplication.context.getString(isEnterprise ? R.string.messages : R.string.discussion));
            list.add(MainApplication.context.getString(R.string.task));
            list.add(MainApplication.context.getString(isEnterprise ? R.string.applicants : R.string.requested_member));
            list.add(MainApplication.context.getString(isEnterprise ? R.string.finances : R.string.courses));
            list.add(MainApplication.context.getString(isEnterprise ? R.string.documents : R.string.resources));
            list.remove(0);
            list.remove(0);
            list.add(1, MainApplication.context.getString(isEnterprise ? R.string.mission : R.string.plan) );
            list.add(2,MainApplication.context.getString(isEnterprise ? R.string.team : R.string.joined_members));
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return list.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment f = null;
        if (!ismyLife) {
            if (position == 0)
                f = new PlanFragment();
            else {
                f = new JoinedMemberFragment();
            }
        }else{
            if (position == 0)
                f = new DiscussionListFragment();
            else if (position == 1)
                f = new PlanFragment();
            else {
                f = checkCondition(position);
            }
        }
        Bundle b = new Bundle();
        b.putString("id", teamId);
        f.setArguments(b);
        return f;
    }

    private Fragment checkCondition(int position) {
        Fragment f = null;
        if (position == 2)
            f = new JoinedMemberFragment();
        else if (position == 3)
            f = new TeamTaskFragment();
        else if (position == 4)
            f = new MembersFragment();
        else if (position == 5)
            f = getFragment();
        else if (position == 6)
            f = new TeamResourceFragment();
        return f;
    }

    private Fragment getFragment() {
       return isEnterprise ? new FinanceFragment() : new TeamCourseFragment();
    }

    @Override
    public int getCount() {
        return list.size();
    }


}

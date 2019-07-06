package org.ole.planet.myplanet.ui.references;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.ole.planet.myplanet.R;
import org.ole.planet.myplanet.model.Reference;
import org.ole.planet.myplanet.ui.map.OfflineMapActivity;
import org.ole.planet.myplanet.utilities.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ReferenceFragment extends Fragment {

    RecyclerView rvReference;

    public ReferenceFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_reference, container, false);
        List<Reference> list = new ArrayList<>();
        list.add(new Reference(getString(R.string.maps), android.R.drawable.ic_dialog_map));
        list.add(new Reference(getString(R.string.dictionaries), R.drawable.ic_dictionary));
        rvReference = v.findViewById(R.id.rv_references);
        rvReference.setLayoutManager(new GridLayoutManager(getActivity(), 3));
        setRecyclerAdapter(list);
        return v;
    }

    private void setRecyclerAdapter(List<Reference> list) {
        rvReference.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolderReference(LayoutInflater.from(getActivity()).inflate(R.layout.row_reference, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder instanceof ViewHolderReference) {
                    ((ViewHolderReference) holder).title.setText(list.get(position).getTitle());
                    ((ViewHolderReference) holder).icon.setImageResource(list.get(position).getIcon());
                    holder.itemView.setOnClickListener(view -> {
                        if (position == 0) startActivity(new Intent(getActivity(), OfflineMapActivity.class));
                        else Utilities.toast(getActivity(), getActivity().getString(R.string.feature_not));
                    });
                }
            }

            @Override
            public int getItemCount() {
                return list.size();
            }
        });
    }


    class ViewHolderReference extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;

        public ViewHolderReference(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            icon = itemView.findViewById(R.id.icon);
        }
    }
}

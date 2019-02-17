package org.ole.planet.myplanet.utilities;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class CheckboxListView extends ListView implements AdapterView.OnItemClickListener {
    ArrayList<Integer> selectedItemsList = new ArrayList<>();

    public CheckboxListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemClickListener(this);

    }

    public CheckboxListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public CheckboxListView(Context context) {
        super(context);
        setOnItemClickListener(this);
    }


    public ArrayList<Integer> getSelectedItemsList() {
        return selectedItemsList;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String itemSelected = ((TextView) view).getText().toString();
        if (selectedItemsList.contains(itemSelected)) {
            selectedItemsList.remove(itemSelected);
        } else {
            selectedItemsList.add(i);
        }
    }
}

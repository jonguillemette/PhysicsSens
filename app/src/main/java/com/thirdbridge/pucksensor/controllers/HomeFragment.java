package com.thirdbridge.pucksensor.controllers;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.Spinner;

import com.thirdbridge.pucksensor.R;
import com.thirdbridge.pucksensor.database.DataManager;
import com.thirdbridge.pucksensor.models.User;
import com.thirdbridge.pucksensor.utils.BaseFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Christophe on 2015-10-14.
 * Modified by Jayson Dalpe since 2016-01-26
 */
public class HomeFragment extends BaseFragment {

    private Button mShotTextButton;
    private ImageButton mNewUserImageButton;
    private Spinner mUserSpinner;
    private Button mSaveUserButton;
    private Button mCancelUserButton;
    private EditText mFirstNameEditText;
    private EditText mLastNameEditText;

    private List<User> mUsers;
    private List<String> mUserNames;
    private ArrayAdapter<User> mSpinnerAdapter;

    public static HomeFragment newInstance(){
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        mShotTextButton = (Button) v.findViewById(R.id.shot_test_button);
        mNewUserImageButton = (ImageButton) v.findViewById(R.id.new_user_image_button);
        mUserSpinner = (Spinner) v.findViewById(R.id.user_spinner);

        populateSpinner();

        mUserSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (mUsers.get(position) != null) {
                    getController().setCurrentUsername(mUsers.get(position).getFirstName() + " " + mUsers.get(position).getLastName());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                getController().setCurrentUsername("");
            }

        });

        final PopupWindow popupWindow = inflateNewUserPopup(container);

        mNewUserImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText) popupWindow.getContentView().findViewById(R.id.first_name)).setText("");
                ((EditText) popupWindow.getContentView().findViewById(R.id.last_name)).setText("");
                popupWindow.setFocusable(true);
                popupWindow.update();
                popupWindow.showAtLocation(container, Gravity.TOP, 0, 200);
            }
        });

        mShotTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reenable link
                getController().gotoShotStats((User) mUserSpinner.getSelectedItem());
                /*if(mUserSpinner.getSelectedItemPosition() != -1) {
                    if (getController().isBleDeviceConnected()) {
                        getController().gotoShotStats((User)mUserSpinner.getSelectedItem());
                    }
                }*/
            }
        });

        return v;
    }

    private void populateSpinner(){
        mUsers = new ArrayList<>();

        for(User user: DataManager.get().getUsers()){
            mUsers.add(user);
        }

        mSpinnerAdapter = new UserArrayAdapter(getController(), R.layout.spinner_dropdown_item, mUsers);
        mUserSpinner.setAdapter(mSpinnerAdapter);
    }

    private PopupWindow inflateNewUserPopup(ViewGroup container){

        LayoutInflater layoutInflater
                = (LayoutInflater)getController().getBaseContext()
                .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.new_user_popup, container, false);

        mSaveUserButton = (Button) popupView.findViewById(R.id.save_user_button);
        mCancelUserButton = (Button) popupView.findViewById(R.id.cancel_button);
        mFirstNameEditText = (EditText) popupView.findViewById(R.id.first_name);
        mLastNameEditText = (EditText) popupView.findViewById(R.id.last_name);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                1000,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mSaveUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mFirstNameEditText.getText().toString().matches("") || !mLastNameEditText.getText().toString().matches("")) {
                    User newUser = new User(UUID.randomUUID().toString(), mFirstNameEditText.getText().toString(), mLastNameEditText.getText().toString());
                    DataManager.get().addUser(newUser);
                    mUsers.add(newUser);
                    mSpinnerAdapter.notifyDataSetChanged();
                    mUserSpinner.setSelection(mSpinnerAdapter.getCount() - 1);
                    popupWindow.dismiss();
                    getController().setCurrentUsername(mFirstNameEditText.getText().toString() + " " + mLastNameEditText.getText().toString());
                }
            }
        });

        mCancelUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        return popupWindow;
    }

    private class UserArrayAdapter extends ArrayAdapter<User> {

        List<User> userList = new ArrayList<>();

        public UserArrayAdapter(Context context, int textViewResourceId, List<User> objects) {
            super(context, textViewResourceId, objects);
            userList = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position,convertView,parent);
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position,convertView,parent);
        }

        public View getCustomView(int position, View convertView, ViewGroup parent) {

            LayoutInflater inflater = (LayoutInflater)getController().getBaseContext()
                    .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

            View spinnerView = inflater.inflate(R.layout.spinner_dropdown_item, parent, false);

            CheckedTextView checkedTextView = (CheckedTextView) spinnerView.findViewById(R.id.spinner_dropdown_item);

            checkedTextView.setText(userList.get(position).getFirstName() + " " + userList.get(position).getLastName());

            return spinnerView;
        }
    }

}

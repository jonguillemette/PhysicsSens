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
 */
public class HomeFragment extends BaseFragment {

    private Button shotTestButton;
    private ImageButton newUserImageButton;
    private Spinner userSpinner;
    private Button saveUserButton;
    private Button cancelUserButton;
    private EditText firstNameEditText;
    private EditText lastNameEditText;

    private List<User> users;
    private List<String> userNames;
    private ArrayAdapter<User> spinnerAdapter;

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

        shotTestButton = (Button) v.findViewById(R.id.shot_test_button);
        newUserImageButton = (ImageButton) v.findViewById(R.id.new_user_image_button);
        userSpinner = (Spinner) v.findViewById(R.id.user_spinner);

        populateSpinner();

        userSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(users.get(position) != null) {
                    getController().setCurrentUsername(users.get(position).getFirstName() + " " + users.get(position).getLastName());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                getController().setCurrentUsername("");
            }

        });

        final PopupWindow popupWindow = inflateNewUserPopup(container);

        newUserImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText)popupWindow.getContentView().findViewById(R.id.first_name)).setText("");
                ((EditText)popupWindow.getContentView().findViewById(R.id.last_name)).setText("");
                popupWindow.setFocusable(true);
                popupWindow.update();
                popupWindow.showAtLocation(container, Gravity.TOP, 0, 200);
            }
        });

        shotTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reenable link
                getController().gotoShotStats((User)userSpinner.getSelectedItem());
                /*if(userSpinner.getSelectedItemPosition() != -1) {
                    if (getController().isBleDeviceConnected()) {
                        getController().gotoShotStats((User)userSpinner.getSelectedItem());
                    }
                }*/
            }
        });

        return v;
    }

    private void populateSpinner(){
        users = new ArrayList<>();

        for(User user: DataManager.get().getUsers()){
            users.add(user);
        }

        spinnerAdapter = new UserArrayAdapter(getController(), R.layout.spinner_dropdown_item, users);
        userSpinner.setAdapter(spinnerAdapter);
    }

    private PopupWindow inflateNewUserPopup(ViewGroup container){

        LayoutInflater layoutInflater
                = (LayoutInflater)getController().getBaseContext()
                .getSystemService(getController().LAYOUT_INFLATER_SERVICE);

        final View popupView = layoutInflater.inflate(R.layout.new_user_popup, container, false);

        saveUserButton = (Button) popupView.findViewById(R.id.save_user_button);
        cancelUserButton = (Button) popupView.findViewById(R.id.cancel_button);
        firstNameEditText = (EditText) popupView.findViewById(R.id.first_name);
        lastNameEditText = (EditText) popupView.findViewById(R.id.last_name);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                1000,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        saveUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!firstNameEditText.getText().toString().matches("") || !lastNameEditText.getText().toString().matches("")){
                    User newUser = new User(UUID.randomUUID().toString(),firstNameEditText.getText().toString(),lastNameEditText.getText().toString());
                    DataManager.get().addUser(newUser);
                    users.add(newUser);
                    spinnerAdapter.notifyDataSetChanged();
                    userSpinner.setSelection(spinnerAdapter.getCount() - 1);
                    popupWindow.dismiss();
                    getController().setCurrentUsername(firstNameEditText.getText().toString() + " " + lastNameEditText.getText().toString());
                }
            }
        });

        cancelUserButton.setOnClickListener(new View.OnClickListener() {
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

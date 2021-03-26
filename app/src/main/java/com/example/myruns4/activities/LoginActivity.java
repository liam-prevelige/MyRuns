package com.example.myruns4.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myruns4.R;
import com.example.myruns4.models.Login;

/**
 * Create initial Login page for the app that can navigate user to create an account
 * or enter the activity tracking page
 */
public class LoginActivity extends AppCompatActivity {
    private Login currentLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActionBar()!=null) getActionBar().setTitle("Sign in");
        setContentView(R.layout.activity_login);
        currentLogin = new Login(getApplicationContext());
        currentLogin.setLoadingMapFromHistory(false);
        currentLogin.setServiceStarted(false);
        currentLogin.setMapActivityStarted(false);
        currentLogin.setMapRotated(false);
        currentLogin.setHasNotified(false);
        currentLogin.setShouldStop(false);
        currentLogin.setSavePressed(false);

        loadLogin();
        changeActionBar();
        signIn();
        register();
    }

    private void changeActionBar(){
        setTitle("Sign in");
    }

    private void signIn(){
        Button signInButton = findViewById(R.id.sign_in);
        signInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText emailEntry = findViewById(R.id.email);
                EditText passwordEntry = findViewById(R.id.password);
                if(!(currentLogin.getEmail() == null || currentLogin.getEmail().equals("empty") || currentLogin.getPassword().equals("empty"))) {
                    if(noLoginInputErrors(emailEntry, passwordEntry)){
                        if (emailEntry.getText().toString().equals(currentLogin.getEmail()) && passwordEntry.getText().toString().equals(currentLogin.getPassword())) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        } else {
                            Toast.makeText(getApplicationContext(), "Email or password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "No profile has been created", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void loadLogin(){
        assert currentLogin.getProfile() != null;
        if(!(currentLogin.getEmail() != null || currentLogin.getPassword() != null || currentLogin.getEmail().equals("empty") || currentLogin.getPassword().equals("empty"))){
            EditText emailBox = findViewById(R.id.email);
            EditText passwordBox = findViewById(R.id.password);
            emailBox.setText(currentLogin.getEmail());
            passwordBox.setText(currentLogin.getPassword());
        }
    }

    /**
     * @param emailEntry value user has put into the email text box
     * @param passwordEntry value user has put into the password text box
     * @return whether or not the user's inputs for email/password have formatting issues
     */
    private boolean noLoginInputErrors(EditText emailEntry, EditText passwordEntry){
        boolean incorrectLoginInput = true;
        if(emailEntry.getText().toString().equals("")){
            emailEntry.setError("This field is required");
            incorrectLoginInput = false;
        }
        // Use provided Android functionality to check whether the Email Address is properly formatted
        else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(emailEntry.getText().toString()).matches()){
            emailEntry.setError("This email address is invalid");
            incorrectLoginInput = false;
        }
        if(passwordEntry.getText().toString().equals("")){
            passwordEntry.setError("This field is required");
            incorrectLoginInput = false;
        }
        if(passwordEntry.getText().toString().length() < 7){
            passwordEntry.setError("Password must be more than six characters");
            incorrectLoginInput = false;
        }
        return incorrectLoginInput;
    }

    private void register(){
        Button registerButton = findViewById(R.id.register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                currentLogin.setNeedAutofill(false);    // The user is creating a new account, so don't ask to load old one
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }
}

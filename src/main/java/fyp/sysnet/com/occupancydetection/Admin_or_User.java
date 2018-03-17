package fyp.sysnet.com.occupancydetection;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;


public class Admin_or_User extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_or__user);
    }

    public void admin (View view){

        Intent new_screen = new Intent(Admin_or_User.this,adminlogin.class);
        startActivity(new_screen);
    }


    public void user (View view){
        Intent new_screen = new Intent(Admin_or_User.this,userlogin.class);
        startActivity(new_screen);
    }
}

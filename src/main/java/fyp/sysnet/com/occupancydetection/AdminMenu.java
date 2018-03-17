package fyp.sysnet.com.occupancydetection;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

public class AdminMenu extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);
    }

    public void OccupancyView (View view){

        Intent new_screen = new Intent(AdminMenu.this,occupancy_view.class);
        startActivity(new_screen);
    }


    public void RegisterUser (View view){

        Intent new_screen = new Intent(AdminMenu.this,UserRegisteration.class);
        startActivity(new_screen);

    }
}

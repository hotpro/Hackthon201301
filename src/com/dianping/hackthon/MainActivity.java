package com.dianping.hackthon;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	Button btn;
	Button btnVerify;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btn = (Button)findViewById(R.id.btn_get);
		btn.setOnClickListener(this);
		
		btnVerify = (Button) findViewById(R.id.btn_verify);
		btnVerify.setOnClickListener(this);
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.activity_main, menu);
//		return true;
//	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_get:
			ShowFragment.newInstance(this);
			break;
			
		case R.id.btn_verify:
			VerifyFragment.newInstance(this);
			break;

		default:
			break;
		}
	}
	

	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menu items default to never show in the action bar. On most devices this means
        // they will show in the standard options menu panel when the menu button is pressed.
        // On xlarge-screen devices a "More" button will appear in the far right of the
        // Action Bar that will display remaining items in a cascading menu.
        menu.add("Normal item");

        MenuItem actionItem = menu.add("Action Button");

        // Items that show as actions should favor the "if room" setting, which will
        // prevent too many buttons from crowding the bar. Extra items will show in the
        // overflow area.
        actionItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        // Items that show as actions are strongly encouraged to use an icon.
        // These icons are shown without a text description, and therefore should
        // be sufficiently descriptive on their own.
        actionItem.setIcon(android.R.drawable.ic_menu_share);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Fragment f = getFragmentManager().findFragmentByTag(ShowFragment.TAG);
        if (f instanceof ShowFragment) {
        	((ShowFragment)f).getImage();
        }
        
        return true;
    }
	
	

}

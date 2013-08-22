package com.xabber.android.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.xabber.android.data.account.AccountManager;
import com.xabber.android.data.intent.EntityIntentBuilder;
import com.xabber.android.data.jingle.JingleManager;
import com.xabber.android.ui.helper.ManagedActivity;
import com.xabber.androiddev.R;

/**
 * Activity for jingle, currently uses droingle implementation
 *
 * @author Robert Taglang
 */
public class JingleViewer extends ManagedActivity implements Button.OnClickListener {

    private JingleManager manager;
    private String account;
    private String user;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing())
            return;

        Intent intent = getIntent();
        account = getAccount(intent);
        user = getUser(intent);

        setContentView(R.layout.jingle_viewer);
        ((TextView)findViewById(R.id.jingle_account_name)).setText(account);
        ((TextView)findViewById(R.id.jingle_contact_name)).setText(JingleManager.toBareJid(user));

        Button call = (Button)findViewById(R.id.jingle_end);
        call.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);

        call.setOnClickListener(this);

        manager = AccountManager.getInstance().getAccount(account).getJingleManager();
        manager.setActivity(this);
        if(!manager.isActive()) {
            manager.sendJingleRequest(account, user);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        manager.setActivity(null);
    }

    public static Intent createIntent(Context context, String account,
                                      String user) {
        return new EntityIntentBuilder(context, JingleViewer.class)
                .setAccount(account).setUser(user).build();
    }

    @Override
    public void onClick(View view) {
        close();
    }

    public void close() {
        manager.close(user);
        finish();
    }

    private static String getAccount(Intent intent) {
        String value = EntityIntentBuilder.getAccount(intent);
        if (value != null)
            return value;
        // Backward compatibility.
        return intent.getStringExtra("com.xabber.android.data.account");
    }

    private static String getUser(Intent intent) {
        String value = EntityIntentBuilder.getUser(intent);
        if (value != null)
            return value;
        // Backward compatibility.
        return intent.getStringExtra("com.xabber.android.data.user");
    }
}

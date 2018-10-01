package group26.cs307.allinwallet;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddPurchase extends AppCompatActivity {
    private Button add;
    private EditText inputName, inputPrice;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private static final String TAG = "AllinWallet";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_purchase);
        add = (Button) findViewById(R.id.add_item_button);
        inputName = (EditText) findViewById(R.id.item_name);
        inputPrice = (EditText) findViewById(R.id.item_price);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = inputName.getText().toString();
                String price = inputPrice.getText().toString();
                Log.d(TAG, "Item Name: " + name);
                Log.d(TAG, "Item Price: " + price);
                addPurchase(name, price);

            }
        });

    }

    public void addPurchase(String name, String price) {
        String uid = auth.getUid();
        Map<String, Object> purchaselist = new HashMap<>();
        purchaselist.put("uid", uid);
        purchaselist.put("name", name);
        purchaselist.put("price", price);
        CollectionReference purchase = db.collection("purchase");
        purchase.document(uid).set(purchaselist);
        Log.d(TAG, uid+" send purchase data");

    }


}

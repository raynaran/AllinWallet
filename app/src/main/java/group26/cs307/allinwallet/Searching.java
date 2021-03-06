package group26.cs307.allinwallet;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Searching extends AppCompatActivity {
    private Spinner categoryPicker;
    private EditText inputName, inputStart, inputEnd;
    private Button clear, btn_search;
    private RecyclerView purchaseList;
    private RecyclerView.Adapter purchaseListAdapter;
    private RecyclerView.LayoutManager purchaseListLayoutManager;
    public static List<PurchaseItem> purchases;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "AllinWallet";
    private FirebaseAuth auth;

    private DatePickerDialog.OnDateSetListener startDateSetListener, endDateSetListener;
    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat formatter;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        final GlobalClass globalVariable = (GlobalClass) getApplicationContext();
        final String color = globalVariable.getThemeSelection();
        if (color != null && color.equals("dark")) {
            LinearLayout li = (LinearLayout) findViewById(R.id.searchingLY);
            li.setBackgroundResource(R.color.cardview_dark_background);
            ActionBar ac;
            ac = getSupportActionBar();
            ac.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
            TextView tv,start,end;
            Spinner ctpicker;
            tv = findViewById(R.id.filterCatTV);
            tv.setTextColor(Color.parseColor("#ffffff"));
            start = findViewById(R.id.start_date);
            start.setTextColor(Color.parseColor("#ffffff"));
            start.setHintTextColor(Color.parseColor("#ffffff"));
            end = findViewById(R.id.end_date);
            end.setHintTextColor(Color.parseColor("#ffffff"));
            end.setTextColor(Color.parseColor("#ffffff"));
        }
        auth = FirebaseAuth.getInstance();

        clear = (Button) findViewById(R.id.clear_button);
        btn_search = (Button) findViewById(R.id.btn_search);
        inputName = (EditText) findViewById(R.id.item_name2);
        inputStart = (EditText) findViewById(R.id.start_date);
        inputEnd = (EditText) findViewById(R.id.end_date);

        purchaseList = (RecyclerView) findViewById(R.id.rst_search);
        purchaseList.setHasFixedSize(true);
        purchaseListLayoutManager = new LinearLayoutManager(Searching.this);
        purchaseList.setLayoutManager(purchaseListLayoutManager);
        purchases = new ArrayList<>();

        purchaseListAdapter = new PurchaseAdapter(purchases, new PurchaseClickListener() {
            @Override
            public void purchaseListClicked(View v, int position) {
            }
        });

        purchaseList.setAdapter(purchaseListAdapter);

        initializeDateSearch();

        categoryPicker = (Spinner) findViewById(R.id.category_picker);
        ArrayAdapter<CharSequence> categoryAA = ArrayAdapter.createFromResource(Searching.this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAA.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryPicker.setAdapter(categoryAA);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputName.setText(null);
                inputStart.setText(null);
                inputEnd.setText(null);
                categoryPicker.setSelection(0);
                purchases.clear();
                purchaseListAdapter.notifyDataSetChanged();
            }
        });

        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = inputName.getText().toString();
                String category = categoryPicker.getSelectedItem().toString();
                String startString = inputStart.getText().toString();
                String endString = inputEnd.getText().toString();
                Log.d(TAG, "Item Category" + category);

                searchAll(category, name, startString, endString);
            }
        });
    }

    public void searchAll(String category, String name, String startString, String endString) {
        String uid = auth.getUid();
        CollectionReference col_purchase = db.collection("users").document(uid).collection("purchase");
        Query result = col_purchase;

        if (!TextUtils.isEmpty(startString)) {
            Date startDate = startCalendar.getTime();
            result = result.whereGreaterThanOrEqualTo("date", startDate);
        }

        if (!TextUtils.isEmpty(endString)) {
            Date endDate = endCalendar.getTime();
            result = result.whereLessThanOrEqualTo("date", endDate);
        }

        if (!TextUtils.equals(category, "Any")) {
            result = result.whereEqualTo("category", category);
        }

        if (!TextUtils.isEmpty(name)) {
            result = result.whereEqualTo("name", name);
        }

        result.orderBy("date", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        purchases.clear();

                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Log.d(TAG, document.getId() + "-->" + document.getData());
                            purchases.add(new PurchaseItem(document.getString("category"),
                                    document.getString("name"), document.getDouble("price"),
                                    document.getDate("date"), document.getString("location"), document.getId()));
                        }

                        purchaseListAdapter.notifyDataSetChanged();
                    }
                });
    }

    public void initializeDateSearch() {
        formatter = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
        startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        startDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                startCalendar.set(Calendar.YEAR, year);
                startCalendar.set(Calendar.MONTH, month);
                startCalendar.set(Calendar.DAY_OF_MONTH, day);

                String date = formatter.format(startCalendar.getTime());
                inputStart.setText(date);
                Log.d(TAG, "onDateSet: mm/dd/yyy:" + date);
            }
        };

        inputStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = startCalendar.get(Calendar.YEAR);
                int month = startCalendar.get(Calendar.MONTH);
                int day = startCalendar.get(Calendar.DAY_OF_MONTH);

                new DatePickerDialog(Searching.this,
                        startDateSetListener, year, month, day).show();
            }
        });

        endCalendar = Calendar.getInstance();
        endCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endCalendar.set(Calendar.MINUTE, 59);
        endCalendar.set(Calendar.SECOND, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);

        endDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                endCalendar.set(Calendar.YEAR, year);
                endCalendar.set(Calendar.MONTH, month);
                endCalendar.set(Calendar.DAY_OF_MONTH, day);

                String date = formatter.format(endCalendar.getTime());
                inputEnd.setText(date);
                Log.d(TAG, "onDateSet: mm/dd/yyy:" + date);
            }
        };

        inputEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int year = endCalendar.get(Calendar.YEAR);
                int month = endCalendar.get(Calendar.MONTH);
                int day = endCalendar.get(Calendar.DAY_OF_MONTH);

                new DatePickerDialog(Searching.this,
                        endDateSetListener, year, month, day).show();
            }
        });
    }
}


package group26.cs307.allinwallet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddPurchase extends AppCompatActivity implements View.OnClickListener {
    private Button save, cancel;
    private ImageView img_reci;
    private Spinner categoryPicker;
    private EditText inputName, inputPrice, inputDate;
    private String locationString;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth auth;
    private static final String TAG = "Add Purchase";

    private DatePickerDialog.OnDateSetListener dateSetListener;
    private Calendar calendar;
    private SimpleDateFormat formatter;
    private int passedPurchaseIndex;
    FirebaseStorage storage;
    StorageReference storageReference;
    private PurchaseItem item;
    private static List<String> categories = new ArrayList<>(Arrays.asList("Grocery",
            "Clothes", "Housing", "Personal", "General", "Transport", "Fun"));
    static final int REQUEST_IMAGE_CAPTURE = 1;

    LinearLayout li;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_purchase);

        final GlobalClass globalVariable = (GlobalClass) getApplicationContext();
        final String color = globalVariable.getThemeSelection();
        if (color != null) {
            if (color.equals("dark")) {
                LinearLayout li = (LinearLayout) findViewById(R.id.addPurchaseLY);
                View bot = findViewById(R.id.img_reci);
                bot.setBackgroundResource(R.color.cardview_dark_background);
                li.setBackgroundResource(R.color.cardview_dark_background);
            }
        }

        passedPurchaseIndex = getIntent().getIntExtra("item_key", -1);
        if (passedPurchaseIndex != -1) {
            item = MainPage.purchases.get(passedPurchaseIndex);
        }

        save = (Button) findViewById(R.id.save_button);
        cancel = (Button) findViewById(R.id.cancel_button);
        inputName = (EditText) findViewById(R.id.item_name);
        inputPrice = (EditText) findViewById(R.id.item_price);
        inputDate = (EditText) findViewById(R.id.item_date);
        img_reci = (ImageView) findViewById(R.id.img_reci);

        categoryPicker = (Spinner) findViewById(R.id.category_picker);
        ArrayAdapter categoryAA = new ArrayAdapter(AddPurchase.this,
                android.R.layout.simple_spinner_dropdown_item, categories);
        categoryPicker.setAdapter(categoryAA);

        formatter = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
        calendar = Calendar.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        save.setOnClickListener(this);
        cancel.setOnClickListener(this);
        inputDate.setOnClickListener(this);

        dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);

                String date = formatter.format(calendar.getTime());
                inputDate.setText(date);
                Log.d(TAG, "onDateSet: mm/dd/yyy:" + date);
            }
        };

        if (passedPurchaseIndex != -1) {
            setTitle(R.string.title_activity_edit_purchase);
            inputName.setText(item.getTitle());
            inputPrice.setText(item.getAmountString());
            inputDate.setText(item.getDateString());
            calendar.setTime(item.getDate());
            locationString = item.getLocation();
            updateReci(item.getDocumentUID());
            categoryPicker.setSelection(categories.indexOf(item.getCategory()));
        } else {
            inputDate.setText(formatter.format(calendar.getTime()));
            locationString = "No Location";
        }
    }

    public void addPurchase(String name, double price, String category, Date date, String location) {
        String time = Calendar.getInstance().getTime().toString();

        Log.d(TAG, "purchase sending time is: " + time);
        String uid = auth.getUid();

        Map<String, Object> purchaselist = new HashMap<>();
        purchaselist.put("name", name);
        purchaselist.put("price", price);
        purchaselist.put("category", category);
        purchaselist.put("date", date);
        purchaselist.put("location", location);

        db.collection("users").document(uid)
                .collection("purchase").document(time).set(purchaselist);
        Log.d(TAG, uid + " send purchase data");

        if (img_reci.getDrawable() != null) {
            uploadrecipe(time);
        }

        if (date.compareTo(MainPage.startOfMonth.getTime()) >= 0) {
            if (Double.compare(price, 0.0) > 0) {
                MainPage.spendingNum += price;
                MainPage.isSpendingUpdated = true;
                updateSummary(date, price);
            }

            MainPage.purchases.add(0, new PurchaseItem(category,
                    name, price, date, location, time));
            MainPage.purchaseListAdapter.notifyItemInserted(0);
        } else if (Double.compare(price, 0.0) > 0) {
            updateSummary(date, price);
        }
    }

    public void updateSummary(Date date, final double amount) {
        String uid = auth.getUid();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        String result = String.format(Locale.getDefault(), "%d-%d", year, month);
        Log.d(TAG, "summary date: " + result);

        final DocumentReference dRef = db.collection("users").document(uid)
                .collection("summary").document(result);

        dRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();

                    if (document.exists()) {
                        double sum = document.getDouble("amount");
                        sum += amount;
                        Map<String, Object> amount = new HashMap<>();
                        amount.put("amount", sum);
                        dRef.update(amount);
                    } else {
                        Log.d(TAG, "Creating new document");
                        Map<String, Object> amountMap = new HashMap<>();
                        amountMap.put("amount", amount);
                        dRef.set(amountMap);
                    }
                } else {
                    Log.e(TAG, "Error getting documents: ", task.getException());
                }
            }
        });
    }

    public void uploadrecipe(String time) {
        //upload picture
        String uid = auth.getUid();
        StorageReference ref = storageReference.child("images/" + uid + "/" + "purchase" + "/" + time + "/" + "recipe");
        img_reci.setDrawingCacheEnabled(true);
        img_reci.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) img_reci.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = ref.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.d(TAG, "upload recipe failed");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "upload recipe succeeded");
            }
        });
    }

    public void updatePurchase(String name, double price, String category, Date date,
                               String location, String documentUID) {
        String uid = auth.getUid();

        Map<String, Object> purchaselist = new HashMap<>();
        purchaselist.put("name", name);
        purchaselist.put("price", price);
        purchaselist.put("category", category);
        purchaselist.put("date", date);
        purchaselist.put("location", location);

        db.collection("users").document(uid)
                .collection("purchase").document(documentUID).update(purchaselist);
        Log.d(TAG, uid + " update purchase data");

        if (img_reci.getDrawable() != null) {
            uploadrecipe(documentUID);
        }

        if (date.compareTo(MainPage.startOfMonth.getTime()) >= 0) {
            if (Double.compare(item.getAmount(), price) != 0) {
                double diff = price - item.getAmount();
                MainPage.spendingNum += diff;
                MainPage.isSpendingUpdated = true;
                updateSummary(date, diff);
            }

            MainPage.purchases.set(passedPurchaseIndex, new PurchaseItem(category,
                    name, price, date, location, documentUID));
            MainPage.purchaseListAdapter.notifyItemChanged(passedPurchaseIndex);
        } else {
            if (Double.compare(item.getAmount(), 0.0) > 0) {
                MainPage.spendingNum -= item.getAmount();
                MainPage.isSpendingUpdated = true;
                updateSummary(item.getDate(), -item.getAmount());
            }

            MainPage.purchases.remove(passedPurchaseIndex);
            MainPage.purchaseListAdapter.notifyItemRemoved(passedPurchaseIndex);

            if (Double.compare(price, 0.0) > 0) {
                updateSummary(date, price);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1000: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    @SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    try {
                        String city = getCityName(location.getLatitude(), location.getLongitude());
                        Toast.makeText(AddPurchase.this, "Located: " + city, Toast.LENGTH_SHORT)
                                .show();
                        locationString = city;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(AddPurchase.this, "Not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permission not granted!", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private String getCityName(double latitude, double lontitude) {
        Log.d(TAG, "latitude:" + latitude + " lontitude" + lontitude);
        String cityName = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latitude, lontitude, 10);
            if (addresses.size() > 0) {
                for (Address address : addresses) {
                    if (address.getLocality() != null && address.getLocality().length() > 0) {
                        cityName = address.getLocality();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void updateReci(String time) {
        String uid = auth.getUid();
        StorageReference ref = storageReference.child("images/" + uid + "/" + "purchase" + "/" + time + "/" + "recipe");
        Log.d(TAG, "update recipe: " + "images/" + uid + "/" + "purchase" + "/" + time + "/" + "recipe");
        File localFile = null;

        try {
            localFile = File.createTempFile("images", "jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        final File local2 = localFile;
        ref.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                Bitmap bitmap = BitmapFactory.decodeFile(local2.getAbsolutePath());
                img_reci.setImageBitmap(bitmap);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

    public void exportToCalendar() {
        String title = inputName.getText().toString();
        String amount = inputPrice.getText().toString();
        String category = categoryPicker.getSelectedItem().toString();
        String location = locationString;

        if (TextUtils.isEmpty(title)) {
            inputName.setError("Title cannot be empty");
            return;
        }

        if (TextUtils.isEmpty(amount)) {
            inputPrice.setError("Amount cannot be empty");
            return;
        }

        String description = String.format(Locale.getDefault(),
                "AllinWallet: %s expense of %s%s.",
                category, MainPage.currencySign, amount);
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.item/event");

        long startTime = calendar.getTimeInMillis();
        long endTime = calendar.getTimeInMillis() + 30 * 60 * 1000;

        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime);
        intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
        intent.putExtra(CalendarContract.Events.TITLE, title);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, description);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);

        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            img_reci.setImageBitmap(imageBitmap);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_purchase_menu, menu);

        if (passedPurchaseIndex == -1) {
            MenuItem check = menu.findItem(R.id.action_check_location);
            check.setVisible(false);
        } else {
            MenuItem get = menu.findItem(R.id.action_get_location);
            get.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_get_location:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
                } else {
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    try {
                        String city = getCityName(location.getLatitude(), location.getLongitude());
                        Toast.makeText(AddPurchase.this, "Located: " + city, Toast.LENGTH_SHORT)
                                .show();
                        locationString = city;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(AddPurchase.this, "Not found", Toast.LENGTH_SHORT).show();
                    }
                }

                return true;
            case R.id.action_check_location:
                if (TextUtils.equals(item.getLocation(), "No Location")) {
                    Toast.makeText(AddPurchase.this, "No Location Found", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(AddPurchase.this, "Place of Purchase: " + locationString, Toast.LENGTH_SHORT)
                            .show();
                }

                return true;
            case R.id.menu_export_to_calendar:
                exportToCalendar();
                return true;
            case R.id.menu_add_receipt:
                dispatchTakePictureIntent();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_button:
                String name = inputName.getText().toString();
                String price = inputPrice.getText().toString();
                String category = categoryPicker.getSelectedItem().toString();
                String location = locationString;
                Date date = calendar.getTime();

                if (TextUtils.isEmpty(name)) {
                    inputName.setError("Title cannot be empty");
                    return;
                }
                if (TextUtils.isEmpty(price)) {
                    inputPrice.setError("Amount cannot be empty");
                    return;
                }

                Log.d(TAG, "Item Title: " + name);
                Log.d(TAG, "Item Amount: " + price);
                Log.d(TAG, "Item Category: " + category);
                Log.d(TAG, "Item Date: " + date);
                Log.d(TAG, "Item Location: " + locationString);

                if (passedPurchaseIndex == -1) {
                    addPurchase(name, Double.parseDouble(price), category, date, location);
                } else {
                    updatePurchase(name, Double.parseDouble(price), category, date, location, item.getDocumentUID());
                }

                onBackPressed();
                break;
            case R.id.cancel_button:
                onBackPressed();
                break;
            case R.id.item_date:
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                new DatePickerDialog(AddPurchase.this,
                        dateSetListener, year, month, day).show();
                break;
            default:
                break;
        }
    }
}

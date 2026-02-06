package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private Button deleteCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;

    private FirebaseFirestore db;
    private CollectionReference citiesRef;
    private City selectedCity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");
        citiesRef.addSnapshotListener((QuerySnapshot value, FirebaseFirestoreException error) -> {
            if (error != null){
                Log.e("Firestore", error.toString());
            }
            if (value != null){
                cityArrayList.clear();
                for (QueryDocumentSnapshot snapshot : value){
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    cityArrayList.add(new City(name, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        deleteCityButton = findViewById(R.id.buttonDeleteCity);
        cityListView = findViewById(R.id.listviewCities);

        // create city array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        deleteCityButton.setEnabled(false);

        // set listeners
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment cityDialogFragment = new CityDialogFragment();
            cityDialogFragment.show(getSupportFragmentManager(),"Add City");
        });

        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            selectedCity = cityArrayAdapter.getItem(i);
            deleteCityButton.setEnabled(selectedCity != null);
        });

        cityListView.setOnItemLongClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city == null) {
                return true;
            }
            CityDialogFragment cityDialogFragment = CityDialogFragment.newInstance(city);
            cityDialogFragment.show(getSupportFragmentManager(),"City Details");
            return true;
        });

        deleteCityButton.setOnClickListener(view -> {
            City city = selectedCity;
            if (city == null) {
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Delete City")
                    .setMessage("Delete \"" + city.getName() + "\"?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        citiesRef.whereEqualTo("name", city.getName())
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    querySnapshot.getDocuments()
                                            .forEach(documentSnapshot -> citiesRef.document(documentSnapshot.getId()).delete());
                                });
                        selectedCity = null;
                        deleteCityButton.setEnabled(false);
                    })
                    .show();
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        if (city == null) {
            return;
        }
        String oldName = city.getName();
        String oldProvince = city.getProvince();

        city.setName(title);
        city.setProvince(year);
        cityArrayAdapter.notifyDataSetChanged();

        Map<String, Object> data = new HashMap<>();
        data.put("name", title);
        data.put("province", year);

        citiesRef.whereEqualTo("name", oldName)
                .whereEqualTo("province", oldProvince)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        citiesRef.add(data);
                        return;
                    }
                    QueryDocumentSnapshot snapshot = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                    citiesRef.document(snapshot.getId()).set(data);
                });
    }

    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        DocumentReference docRef = citiesRef.document(city.getName());
        docRef.set(city);
    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}

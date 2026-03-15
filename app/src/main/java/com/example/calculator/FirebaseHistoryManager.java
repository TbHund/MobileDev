package com.example.calculator;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseHistoryManager {
    private final FirebaseFirestore db;
    private final CollectionReference historyRef;

    public FirebaseHistoryManager() {
        db = FirebaseFirestore.getInstance();
        historyRef = db.collection("calculations");
    }

    public Task<Void> saveCalculation(String expression, String result) {
        CalculationHistory history = new CalculationHistory(
                expression,
                result,
                System.currentTimeMillis()
        );
        return historyRef.document().set(history);
    }

    public Task<QuerySnapshot> loadHistory() {
        return historyRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get();
    }
}
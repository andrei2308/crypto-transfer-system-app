package com.example.crypto_payment_system.ui.transaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.transaction.Transaction;
import com.google.android.material.button.MaterialButton;

public class TransactionResultFragment extends Fragment {
    private static final String ARG_SUCCESS = "success";
    private static final String ARG_HASH = "hash";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_TYPE = "type";
    private static final String ARG_TIMESTAMP = "timestamp";
    private static final String ARG_MESSAGE = "message";

    public static TransactionResultFragment newInstance(boolean success, String hash, String amount, String type, long timestamp, String message) {
        TransactionResultFragment fragment = new TransactionResultFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SUCCESS, success);
        args.putString(ARG_HASH, hash);
        args.putString(ARG_AMOUNT, amount);
        args.putString(ARG_TYPE, type);
        args.putLong(ARG_TIMESTAMP, timestamp);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        boolean success = args.getBoolean(ARG_SUCCESS);
        String hash = args.getString(ARG_HASH, "");
        String amount = args.getString(ARG_AMOUNT, "");
        String type = args.getString(ARG_TYPE, "");
        long timestamp = args.getLong(ARG_TIMESTAMP, 0L);
        String message = args.getString(ARG_MESSAGE, "");

        ImageView statusIcon = view.findViewById(R.id.statusIcon);
        TextView statusText = view.findViewById(R.id.statusText);
        TextView hashText = view.findViewById(R.id.hashText);
        TextView amountText = view.findViewById(R.id.amountText);
        TextView typeText = view.findViewById(R.id.typeText);
        TextView timestampText = view.findViewById(R.id.timestampText);
        TextView messageText = view.findViewById(R.id.messageText);
        MaterialButton okButton = view.findViewById(R.id.okButton);

        if (success) {
            statusIcon.setImageResource(R.drawable.ic_check_circle_green);
            statusText.setText("Transactions sent successfully");
            statusText.setTextColor(getResources().getColor(R.color.colorSuccess));
        } else {
            statusIcon.setImageResource(R.drawable.ic_error_red);
            statusText.setText(R.string.transaction_failed);
            statusText.setTextColor(getResources().getColor(R.color.colorError));
        }

        hashText.setText(hash);
        amountText.setText(amount);
        typeText.setText(type);
        timestampText.setText(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp));
        messageText.setText(message);

        okButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_main, com.example.crypto_payment_system.ui.home.HomeFragment.newInstance())
                .commitNowAllowingStateLoss();
            requireActivity().getSupportFragmentManager().popBackStack(null, 1);
        });
    }
} 
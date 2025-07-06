package com.example.crypto_payment_system.ui.transaction;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.databinding.DialogTransactionDetailsBinding;
import com.example.crypto_payment_system.domain.transaction.Transaction;

import java.util.Date;

public class TransactionDetailsDialogFragment extends DialogFragment {

    private DialogTransactionDetailsBinding binding;
    private Transaction transaction;
    private int amountColor;

    public static TransactionDetailsDialogFragment newInstance(Transaction transaction, String amount, int amountColor) {
        TransactionDetailsDialogFragment fragment = new TransactionDetailsDialogFragment();
        Bundle args = new Bundle();

        args.putString("amount", amount);
        args.putLong("timestamp", transaction.getTimestamp());
        args.putString("tokenAddress", transaction.getTokenAddress());
        args.putString("transactionHash", transaction.getTransactionHash());
        args.putString("transactionType", transaction.getTransactionType());
        args.putString("walletAddress", transaction.getWalletAddress());
        args.putString("walletAddressTo",transaction.getWalletAddressTo());
        args.putString("exchangeRate", transaction.getExchangeRate());
        args.putInt("sentCurrency", transaction.getSentCurrency());
        args.putInt("receivedCurrency", transaction.getReceivedCurrency());
        args.putInt("amountColor", amountColor);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            String amount = getArguments().getString("amount", "0");
            long timestamp = getArguments().getLong("timestamp", 0L);
            String tokenAddress = getArguments().getString("tokenAddress", "");
            String transactionHash = getArguments().getString("transactionHash", "");
            String transactionType = getArguments().getString("transactionType", "");
            String walletAddress = getArguments().getString("walletAddress", "");
            String walletaddressTo = getArguments().getString("walletAddressTo","");
            String exchangeRate = getArguments().getString("exchangeRate","");
            int sentCurrency = getArguments().getInt("sentCurrency");
            int receivedCurrency = getArguments().getInt("receivedCurrency");
            amountColor = getArguments().getInt("amountColor");

            transaction = new Transaction(
                    amount, timestamp, tokenAddress, transactionHash, transactionType, walletAddress,exchangeRate,sentCurrency,receivedCurrency,walletaddressTo
            );
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogTransactionDetailsBinding.inflate(LayoutInflater.from(getContext()));

        if (transaction != null) {
            populateTransactionDetails();
            setupClickListeners();
        }

        return new AlertDialog.Builder(requireContext())
                .setTitle(R.string.transaction_details)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.close, null)
                .create();
    }

    private void populateTransactionDetails() {
        String formattedType = formatTransactionType(transaction.getTransactionType());
        binding.transactionTypeValue.setText(formattedType);

        String amount = transaction.getAmount();

        binding.transactionAmountValue.setText(amount);
        binding.transactionAmountValue.setTextColor(amountColor);

        String formattedDate = formatTimestamp(transaction.getTimestamp());
        binding.transactionTimestampValue.setText(formattedDate);

        binding.transactionHashValue.setText(transaction.getTransactionHash());

        binding.tokenAddressValue.setText(transaction.getTokenAddress());

        binding.walletAddressValue.setText(transaction.getWalletAddress());
    }

    private void setupClickListeners() {
        binding.copyHashButton.setOnClickListener(v -> {
            copyToClipboard("Transaction Hash", transaction.getTransactionHash());
        });

        binding.copyTokenAddressButton.setOnClickListener(v -> {
            copyToClipboard("Token Address", transaction.getTokenAddress());
        });

        binding.copyWalletAddressButton.setOnClickListener(v -> {
            copyToClipboard("Wallet Address", transaction.getWalletAddress());
        });

        binding.viewOnExplorerButton.setOnClickListener(v -> {
            openUrl("https://sepolia.etherscan.io/tx/" + transaction.getTransactionHash());
        });
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));

        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No browser available to open URL", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private String formatTransactionType(String type) {
        String[] words = type.replace("_", " ").split("\\s");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }

            if (i < words.length - 1) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        return DateFormat.format("MMMM dd, yyyy HH:mm:ss", date).toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.crypto_payment_system.utils.adapter.transaction;

import static com.example.crypto_payment_system.config.Constants.EUR_TOKEN_CONTRACT_ADDRESS;
import static com.example.crypto_payment_system.config.Constants.USD_TOKEN_CONTRACT_ADDRESS;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.transaction.Transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    private final TransactionClickListener transactionClickListener;
    private final String currentUserAddress;
    private List<String> prefferedCurrencies = new ArrayList<>();

    public interface TransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    public TransactionAdapter(TransactionClickListener listener, String currentUserAddress) {
        super(new TransactionDiffCallback());
        this.transactionClickListener = listener;
        this.currentUserAddress = currentUserAddress;
    }

    public void setPrefferedCurrencies(List<String> prefferedCurrencies) {
        this.prefferedCurrencies = prefferedCurrencies;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view, transactionClickListener, currentUserAddress, prefferedCurrencies);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final ImageView typeIcon;
        private final TextView typeText;
        private final TextView hashText;
        private final TextView amountText;
        private final TextView timestampText;
        private final TransactionClickListener listener;
        private final String currentUserAddress;
        private List<String> prefferedCurrencies = new ArrayList<>();

        public TransactionViewHolder(@NonNull View itemView, TransactionClickListener listener, String currentUserAddress, List<String> prefferedCurrencies) {
            super(itemView);
            this.listener = listener;
            typeIcon = itemView.findViewById(R.id.transactionTypeIcon);
            typeText = itemView.findViewById(R.id.transactionTypeText);
            hashText = itemView.findViewById(R.id.transactionHashText);
            amountText = itemView.findViewById(R.id.transactionAmount);
            timestampText = itemView.findViewById(R.id.transactionTimestamp);
            this.currentUserAddress = currentUserAddress;
            this.prefferedCurrencies = prefferedCurrencies;
        }

        public void bind(final Transaction transaction) {
            int iconResId;
            String type = transaction.getTransactionType();

            if ("ADD_LIQUIDITY".equals(type)) {
                iconResId = R.drawable.ic_transaction_add;
            } else if ("REMOVE_LIQUIDITY".equals(type)) {
                iconResId = R.drawable.ic_transaction_remove;
            } else if (("EUR TRANSFER".equals(type) || "USD TRANSFER".equals(type)) && transaction.getWalletAddress().equals(transaction.getWalletAddressTo())) {
                iconResId = R.drawable.ic_transaction_swap;
            } else {
                iconResId = R.drawable.ic_transaction_default;
            }
            typeIcon.setImageResource(iconResId);

            typeText.setText(formatTransactionType(transaction.getTransactionType()));

            hashText.setText(shortenHash(transaction.getTransactionHash()));

            String currency = "";
            String exchangeRate = "100000000"; // exchange rate with 8 decimals for blockchain
            if (transaction.getSentCurrency() == 1 && transaction.getWalletAddress().equals(currentUserAddress)) {
                currency += " EUR";
            } else if (transaction.getSentCurrency() == 2 && transaction.getWalletAddress().equals(currentUserAddress)) {
                currency += " USD";
            } else if (transaction.getReceivedCurrency() == 1 && transaction.getWalletAddressTo().equals(currentUserAddress)) {
                currency += " EUR";
                if(!prefferedCurrencies.contains("EUR")){
                    exchangeRate = transaction.getExchangeRate();
                }
            } else if (transaction.getReceivedCurrency() == 2 && transaction.getWalletAddressTo().equals(currentUserAddress)) {
                currency += " USD";
                if(!prefferedCurrencies.contains("USD")){
                    exchangeRate = transaction.getExchangeRate();
                }
            }
            String amount = formatAmount(transaction, transaction.getAmount(), exchangeRate);
            amount += currency;
            amountText.setText(amount);

            boolean isOutgoing = transaction.getWalletAddress().equalsIgnoreCase(currentUserAddress);

            int amountColor;

            if (isOutgoing) {
                amountColor = R.color.colorNegative;
            } else {
                amountColor = R.color.colorPositive;
            }
            amountText.setTextColor(ContextCompat.getColor(itemView.getContext(), amountColor));


            timestampText.setText(formatTimestamp(transaction.getTimestamp()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction);
                }
            });
        }

        private String formatTransactionType(String type) {
            return capitalizeWords(type.replace("_", " "));
        }

        private String shortenHash(String hash) {
            if (hash.length() > 16) {
                return hash.substring(0, 10) + "..." + hash.substring(hash.length() - 6);
            } else {
                return hash;
            }
        }

        private String formatAmount(Transaction transaction, String amountStr, String exchangeRateStr) {
            BigDecimal amount;
            BigDecimal exchangeRate;
            try {
                amount = new BigDecimal(amountStr)
                        .divide(new BigDecimal("1000000"));
                exchangeRate = new BigDecimal(exchangeRateStr)
                        .divide(new BigDecimal("100000000"));
                amount = amount.multiply(exchangeRate);
            } catch (Exception e) {
                amount = BigDecimal.ZERO;
            }

            String currentUserAddress = getCurrentUserAddress();

            String sign;
            if (transaction.getWalletAddress().equals(currentUserAddress)) {
                sign = "-";
            } else {
                sign = "+";
            }

            return sign + amount.abs().toPlainString();
        }

        private String getCurrentUserAddress() {
            return currentUserAddress;
        }

        private String formatTimestamp(long timestamp) {
            Date date = new Date(timestamp);
            return DateFormat.format("MMM dd, yyyy HH:mm", date).toString();
        }

        private String capitalizeWords(String text) {
            StringBuilder result = new StringBuilder();
            String[] words = text.split("\\s");

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
    }

    static class TransactionDiffCallback extends DiffUtil.ItemCallback<Transaction> {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getTransactionHash().equals(newItem.getTransactionHash());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.equals(newItem);
        }
    }
}

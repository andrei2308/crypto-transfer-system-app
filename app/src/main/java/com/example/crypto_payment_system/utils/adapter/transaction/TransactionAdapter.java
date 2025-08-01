package com.example.crypto_payment_system.utils.adapter.transaction;

import static com.example.crypto_payment_system.config.Constants.EURSC;
import static com.example.crypto_payment_system.config.Constants.MINT_USD;
import static com.example.crypto_payment_system.config.Constants.USDT;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.crypto_payment_system.R;
import com.example.crypto_payment_system.domain.transaction.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder> {

    private final TransactionClickListener transactionClickListener;
    private final String currentUserAddress;
    private List<String> prefferedCurrencies = new ArrayList<>();

    private String currentSelectedCurrency = EURSC;

    public interface TransactionClickListener {
        void onTransactionClick(Transaction transaction, String amount, int amountColor);
    }

    public TransactionAdapter(TransactionClickListener listener, String currentUserAddress) {
        super(new TransactionDiffCallback());
        this.transactionClickListener = listener;
        this.currentUserAddress = currentUserAddress;
    }

    public void setPrefferedCurrencies(List<String> prefferedCurrencies) {
        this.prefferedCurrencies = prefferedCurrencies;
    }

    public int getNumberOfPreferredCurrencies() {
        return prefferedCurrencies.size();
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
        holder.setCurrentSelectedCurrency(currentSelectedCurrency);
        holder.bind(getItem(position));
    }

    public void setCurrentSelectedCurrency(String currency) {
        this.currentSelectedCurrency = currency;
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView statusIndicator;
        private final TextView dateMonthText;
        private final TextView dateIndicatorText;
        private final TextView merchantNameTextView;
        private final TextView transactionTypeTextView;
        private final TextView amountTextView;
        private final TransactionClickListener listener;
        private final String currentUserAddress;
        private List<String> prefferedCurrencies = new ArrayList<>();
        private String currentSelectedCurrency = EURSC;

        public TransactionViewHolder(@NonNull View itemView, TransactionClickListener listener, String currentUserAddress, List<String> prefferedCurrencies) {
            super(itemView);
            this.listener = listener;
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            dateIndicatorText = itemView.findViewById(R.id.dateIndicatorText);
            merchantNameTextView = itemView.findViewById(R.id.merchantNameTextView);
            transactionTypeTextView = itemView.findViewById(R.id.transactionTypeTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            dateMonthText = itemView.findViewById(R.id.dateMonthText);
            this.currentUserAddress = currentUserAddress;
            this.prefferedCurrencies = prefferedCurrencies;
        }

        public void setCurrentSelectedCurrency(String currency) {
            this.currentSelectedCurrency = currency;
        }

        public void bind(final Transaction transaction) {
            Date date = new Date(transaction.getTimestamp());

            dateIndicatorText.setText(DateFormat.format("dd", date).toString());

            dateMonthText.setText(DateFormat.format("MMM", date).toString().toUpperCase());
            
            merchantNameTextView.setText(shortenHash(transaction.getTransactionHash()));

            transactionTypeTextView.setText(formatTransactionType(transaction.getTransactionType()));

            String currency = "";
            String exchangeRate = "100000000"; // exchange rate with 8 decimals for blockchain
            boolean isOutgoing = transaction.getWalletAddress().equalsIgnoreCase(currentUserAddress);
            if (!transaction.getWalletAddressTo().equals(transaction.getWalletAddress())) {
                if (transaction.getSentCurrency() == 1 && transaction.getWalletAddress().equals(currentUserAddress) && currentSelectedCurrency.equals(EURSC)) {
                    currency += " " + EURSC;
                } else if (transaction.getSentCurrency() == 2 && transaction.getWalletAddress().equals(currentUserAddress) && currentSelectedCurrency.equals(USDT)) {
                    currency += " " + USDT;
                } else if (transaction.getReceivedCurrency() == 1 && transaction.getWalletAddressTo().equals(currentUserAddress) && currentSelectedCurrency.equals(EURSC)) {
                    currency += " " + EURSC;
                    if (!prefferedCurrencies.contains(EURSC)) {
                        exchangeRate = transaction.getExchangeRate();
                    }
                } else if (transaction.getReceivedCurrency() == 2 && transaction.getWalletAddressTo().equals(currentUserAddress) && currentSelectedCurrency.equals(USDT)) {
                    currency += " " + USDT;
                    if (!prefferedCurrencies.contains(USDT)) {
                        exchangeRate = transaction.getExchangeRate();
                    }
                } else if (transaction.getTransactionType().equals(MINT_USD)){
                    currency += " " + USDT;
                } else {
                    return;
                }
            } else {
                if (transaction.getSentCurrency() == 1 && currentSelectedCurrency.equals(EURSC)) {
                    currency += " " + EURSC;
                } else if (transaction.getSentCurrency() == 2 && currentSelectedCurrency.equals(USDT)) {
                    currency += " " + USDT;
                } else if (transaction.getSentCurrency() == 2 && currentSelectedCurrency.equals(EURSC)) {
                    currency += " " + EURSC;
                    isOutgoing = false;
                    exchangeRate = transaction.getExchangeRate();
                } else if (transaction.getSentCurrency() == 1 && currentSelectedCurrency.equals(USDT)) {
                    currency += " " + USDT;
                    isOutgoing = false;
                    exchangeRate = transaction.getExchangeRate();
                }
            }
            String amount = formatAmount(transaction, transaction.getAmount(), exchangeRate);
            amount += currency;
            amountTextView.setText(amount);

            int amountColor;

            if (isOutgoing) {
                amountColor = R.color.colorNegative;
            } else {
                amountColor = R.color.colorPositive;
            }
            amountTextView.setTextColor(ContextCompat.getColor(itemView.getContext(), amountColor));

            String finalAmount = amount;
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTransactionClick(transaction, finalAmount, amountColor);
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
                amount = amount.multiply(exchangeRate)
                        .setScale(3, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
            } catch (Exception e) {
                amount = BigDecimal.ZERO;
            }

            String currentUserAddress = getCurrentUserAddress();

            String sign;
            if (!transaction.getWalletAddressTo().equals(transaction.getWalletAddress())) {
                if (transaction.getWalletAddress().equals(currentUserAddress)) {
                    sign = "-";
                } else {
                    sign = "+";
                }
            } else {
                if (transaction.getSentCurrency() == 1 && currentSelectedCurrency.equals(EURSC)) {
                    sign = "-";
                } else if (transaction.getSentCurrency() == 2 && currentSelectedCurrency.equals(USDT)) {
                    sign = "-";
                } else if (transaction.getSentCurrency() == 1 && currentSelectedCurrency.equals(USDT)) {
                    sign = "+";
                } else if (transaction.getSentCurrency() == 2 && currentSelectedCurrency.equals(EURSC)) {
                    sign = "+";
                } else {
                    sign = "";
                }
            }

            return sign + amount.abs().toPlainString();
        }

        private String getCurrentUserAddress() {
            return currentUserAddress;
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

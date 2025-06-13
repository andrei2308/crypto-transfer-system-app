package com.example.crypto_payment_system.utils.events;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Int;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.ArrayList;
import java.util.List;

public class EventParser {

    private static final String MONEY_SENT_EVENT_HASH =
            "0x3e509f97a730e507fdf3a690ff8b9d47d4a10abad44fc4a5f82b950ce7e2bfe3";

    private static final String EXCHANGE_COMPLETED_EVENT_HASH =
            "0x737cbca5dd8304210f05bcf24592c10d397a64c9982827672d3739f27d1df4ce";

    public static class ExchangeInfo {
        private int sendCurrency;
        private int receiveCurrency;
        private String exchangeRate;
        private TransactionReceipt receipt;
        private boolean isExchange;

        public ExchangeInfo(int sendCurrency, int receiveCurrency, String exchangeRate,
                            TransactionReceipt receipt, boolean isExchange) {
            this.sendCurrency = sendCurrency;
            this.receiveCurrency = receiveCurrency;
            this.exchangeRate = exchangeRate;
            this.receipt = receipt;
            this.isExchange = isExchange;
        }

        public int getSendCurrency() { return sendCurrency; }
        public int getReceiveCurrency() { return receiveCurrency; }
        public String getExchangeRate() { return exchangeRate; }
        public TransactionReceipt getReceipt() { return receipt; }
        public boolean isExchange() { return isExchange; }
    }

    private static <T extends Type> TypeReference<T> createTypeReference(Class<T> type) {
        return TypeReference.create(type);
    }

    public static ExchangeInfo extractExchangeInfo(TransactionReceipt receipt) {
        if (receipt == null) {
            return null;
        }

        List<Log> logs = receipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            System.out.println("No logs found in receipt");
            return new ExchangeInfo(0, 0, "0", receipt, false);
        }

        for (Log log : logs) {
            if (log.getTopics() == null || log.getTopics().isEmpty()) {
                continue;
            }

            String eventHash = log.getTopics().get(0);
            System.out.println("Found event hash: " + eventHash);

            if (eventHash.equalsIgnoreCase(MONEY_SENT_EVENT_HASH)) {
                System.out.println("Found MoneySent event!");
                String data = log.getData();
                System.out.println("Event data: " + data);

                List outputParameters = new ArrayList<>();
                outputParameters.add(createTypeReference(Uint256.class));
                outputParameters.add(createTypeReference(Uint256.class));
                outputParameters.add(createTypeReference(Uint8.class));
                outputParameters.add(createTypeReference(Uint8.class));
                outputParameters.add(createTypeReference(Int.class));

                try {
                    List<Type> results = FunctionReturnDecoder.decode(data, outputParameters);
                    System.out.println("Decoded results size: " + results.size());

                    for (int i = 0; i < results.size(); i++) {
                        System.out.println("Result " + i + ": " + results.get(i));
                    }

                    int sendCurrency = ((Uint8) results.get(2)).getValue().intValue();
                    int receiveCurrency = ((Uint8) results.get(3)).getValue().intValue();
                    String exchangeRate = ((Int) results.get(4)).getValue().toString();

                    System.out.println("Extracted values - sendCurrency: " + sendCurrency +
                            ", receiveCurrency: " + receiveCurrency +
                            ", exchangeRate: " + exchangeRate);

                    return new ExchangeInfo(sendCurrency, receiveCurrency, exchangeRate, receipt, false);
                } catch (Exception e) {
                    System.out.println("Error decoding MoneySent event data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            else if (eventHash.equalsIgnoreCase(EXCHANGE_COMPLETED_EVENT_HASH)) {
                System.out.println("Found ExchangeCompleted event!");
                String data = log.getData();
                System.out.println("Event data: " + data);

                List outputParameters = new ArrayList<>();
                outputParameters.add(createTypeReference(Uint256.class));
                outputParameters.add(createTypeReference(Uint256.class));
                outputParameters.add(createTypeReference(Uint8.class));
                outputParameters.add(createTypeReference(Uint8.class));
                outputParameters.add(createTypeReference(Int.class));

                try {
                    List<Type> results = FunctionReturnDecoder.decode(data, outputParameters);
                    System.out.println("Decoded results size: " + results.size());

                    for (int i = 0; i < results.size(); i++) {
                        System.out.println("Result " + i + ": " + results.get(i));
                    }

                    int sourceCurrency = ((Uint8) results.get(2)).getValue().intValue();
                    int targetCurrency = ((Uint8) results.get(3)).getValue().intValue();
                    String exchangeRate = ((Int) results.get(4)).getValue().toString();

                    System.out.println("Extracted values - sourceCurrency: " + sourceCurrency +
                            ", targetCurrency: " + targetCurrency +
                            ", exchangeRate: " + exchangeRate);

                    return new ExchangeInfo(sourceCurrency, targetCurrency, exchangeRate, receipt, true);
                } catch (Exception e) {
                    System.out.println("Error decoding ExchangeCompleted event data: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Could not find or decode any relevant events");
        return new ExchangeInfo(0, 0, "0", receipt, false);
    }
}
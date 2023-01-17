package me.mrsofiane.grpcclient.web;

import io.grpc.stub.StreamObserver;
import lombok.Builder;
import lombok.Data;
import me.mrsofiane.grpcserver.grpc.stub.Bank;
import me.mrsofiane.grpcserver.grpc.stub.BankServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
public class BankRestApi {

    @GrpcClient("bankService")
    private BankServiceGrpc.BankServiceBlockingStub blockingStub;
    @GrpcClient("bankService")
    private BankServiceGrpc.BankServiceStub bankServiceStub;

    @GetMapping("/convert")
    public ConvertResponseDTO convert(
            @RequestParam String currencyFrom,
            @RequestParam String currencyTo,
            @RequestParam double amount) {

        Bank.ConvertCurrencyRequest convertCurrencyRequest = Bank.ConvertCurrencyRequest.newBuilder()
                .setCurrencyFrom(currencyFrom)
                .setCurrencyTo(currencyTo)
                .setAmount(amount)
                .build();
        Bank.ConvertCurrencyResponse convertCurrencyResponse = blockingStub.convertCurrency(convertCurrencyRequest);
        return ConvertResponseDTO.builder()
                .from(convertCurrencyResponse.getCurrencyFrom())
                .to(convertCurrencyResponse.getCurrencyTo())
                .amount(convertCurrencyResponse.getAmount())
                .result(convertCurrencyResponse.getConversionResult())
                .build();

    }

    @GetMapping(value = "transactions/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public List<TransactionResponseDTO> getTransactions(@PathVariable("id") String accountId) {
        Bank.GetStreamOfTransactionsRequest request = Bank.GetStreamOfTransactionsRequest.newBuilder()
                .setAccountId(accountId)
                .build();
            List<TransactionResponseDTO> transactionResponseDTO = new ArrayList<>();
        bankServiceStub.getStreamOfTransactions(request, new StreamObserver<Bank.Transaction>() {
                @Override
                public void onNext(Bank.Transaction transaction) {
                    TransactionResponseDTO transactionResponse = TransactionResponseDTO.builder()
                            .id(transaction.getId())
                            .accountId(transaction.getAccountId())
                            .amount(transaction.getAmount())
                            .status(transaction.getStatus().name())
                            .type(transaction.getType().name())
                            .timestamp(transaction.getTimestamp())
                            .build();
                    transactionResponseDTO.add(transactionResponse);
                    System.out.println("*****TRANSACTION STREAMING*****");

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {
                    System.out.println("***** END OF TRANSACTIONS *****");
                }
            });
        return transactionResponseDTO;




    }
}

@Data
@Builder
class ConvertResponseDTO {
    private String from;
    private String to;
    private double amount;
    private double result;

}

@Data
@Builder
class TransactionResponseDTO {
    private Long id;
    private String accountId;
    private Long timestamp;
    private double amount;
    private String type;
    private String status;
    /*
            "id": "40",
            "accountId": "CC1",
            "timestamp": "1673622642630",
            "amount": 70395.20445297417,
            "type": "DEBIT",
            "status": "PENDING"
    */
}

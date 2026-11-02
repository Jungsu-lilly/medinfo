package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayRepository;
import com.medinfo.nonpay.NonpaySummaryRow;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class NonpaySummaryWriter implements ItemWriter<NonpaySummaryRow> {

    private final NonpayRepository repository;

    public NonpaySummaryWriter(NonpayRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends NonpaySummaryRow> chunk) {
        repository.saveSummary(chunk.getItems());
    }
}

package com.medinfo.nonpay.batch;

import com.medinfo.nonpay.NonpayRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public class NonpayDetailWriter implements ItemWriter<HospitalDetail> {

    private final NonpayRepository repository;

    public NonpayDetailWriter(NonpayRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends HospitalDetail> chunk) {
        for (HospitalDetail detail : chunk) {
            repository.replaceDetail(detail.hospitalId(), detail.rows());
        }
    }
}

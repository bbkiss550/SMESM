package com.smeservicemanager.job;

import com.smeservicemanager.shared.domain.DomainTypes.VatMode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JobFinancialCalculationTests {

    @Test
    void calculatesInclusiveVatWithoutIncreasingCustomerTotal() {
        Job job = jobWithItem("107.00");

        job.recalculateTotals();

        assertThat(job.getGrandTotal()).isEqualByComparingTo("107.00");
        assertThat(job.getTaxBase()).isEqualByComparingTo("100.00");
        assertThat(job.getVatAmount()).isEqualByComparingTo("7.00");
    }

    @Test
    void excludesInactiveItemsAndNeverProducesNegativeTotal() {
        Job job = jobWithItem("100.00");
        JobItem removed = item("500.00");
        removed.deactivate();
        job.getItems().add(removed);
        job.setDiscount(new BigDecimal("150.00"));

        job.recalculateTotals();

        assertThat(job.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(job.getGrandTotal()).isZero();
        assertThat(job.getTaxBase()).isZero();
        assertThat(job.getVatAmount()).isZero();
    }

    @Test
    void supportsNoVatMode() {
        Job job = jobWithItem("100.00");
        job.setVatMode(VatMode.NO_VAT);

        job.recalculateTotals();

        assertThat(job.getGrandTotal()).isEqualByComparingTo("100.00");
        assertThat(job.getTaxBase()).isEqualByComparingTo("100.00");
        assertThat(job.getVatAmount()).isZero();
    }

    private Job jobWithItem(String amount) {
        Job job = new Job();
        job.getItems().add(item(amount));
        return job;
    }

    private JobItem item(String amount) {
        JobItem item = new JobItem();
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal(amount));
        item.calculateTotal();
        return item;
    }
}

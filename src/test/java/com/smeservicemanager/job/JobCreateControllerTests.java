package com.smeservicemanager.job;

import com.smeservicemanager.catalog.ProductRepository;
import com.smeservicemanager.catalog.ServiceCatalogRepository;
import com.smeservicemanager.customer.CustomerRepository;
import com.smeservicemanager.payment.*;
import com.smeservicemanager.security.UserRepository;
import com.smeservicemanager.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobCreateControllerTests {
    private JobService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(JobService.class);
        mvc = MockMvcBuilders.standaloneSetup(new JobController(mock(JobRepository.class), service,
                mock(CustomerRepository.class), mock(ServiceCatalogRepository.class), mock(ProductRepository.class),
                mock(UserRepository.class), mock(PaymentRepository.class), mock(RefundRepository.class),
                mock(PaymentService.class), mock(CancellationService.class))).build();
    }

    private MockHttpServletRequestBuilder validRequest() {
        return post("/jobs").param("customerId", "1").param("serviceId", "1").param("title", "ทดสอบใบงาน")
                .param("address", "สถานที่ทดสอบ").param("appointmentStartDate", "2026-10-02")
                .param("appointmentStartTime", "09:00").param("appointmentEndDate", "2026-10-02")
                .param("appointmentEndTime", "10:00");
    }

    @Test
    void validationFailureAlwaysProvidesModalModeAndPreservesForm() throws Exception {
        mvc.perform(post("/jobs").param("title", "ข้อมูลยังไม่ครบ"))
                .andExpect(status().isOk()).andExpect(view().name("job/form"))
                .andExpect(model().attribute("modalMode", false))
                .andExpect(model().attributeHasFieldErrors("jobForm", "customerId", "serviceId", "address"));
        verifyNoInteractions(service);
    }

    @Test
    void scheduleConflictReturnsFormErrorsInsteadOfServerFailure() throws Exception {
        when(service.create(any())).thenThrow(new BusinessException("หัวหน้าช่างมีงานซ้อนในช่วงเวลาที่เลือก"));
        mvc.perform(validRequest()).andExpect(status().isOk()).andExpect(view().name("job/form"))
                .andExpect(model().attribute("modalMode", false)).andExpect(model().attributeHasErrors("jobForm"));
        verify(service).create(any());
    }

    @Test
    void validFormStillCreatesOneJob() throws Exception {
        Job job = new Job(); job.setId(129L);
        when(service.create(any())).thenReturn(job);
        mvc.perform(validRequest()).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/jobs/129"));
        verify(service, times(1)).create(any());
    }

    @Test
    void modalAssignmentReturnsJsonWithoutRedirect() throws Exception {
        mvc.perform(post("/jobs/129/assign/modal").param("technicianId", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("มอบหมายหัวหน้าช่างเรียบร้อยแล้ว"));
        verify(service).assign(129L, 2L);
    }

    @Test
    void modalAssignmentPreservesScheduleConflictMessage() throws Exception {
        doThrow(new BusinessException("หัวหน้าช่างมีงานซ้อนในช่วงเวลาที่เลือก")).when(service).assign(129L, 2L);
        mvc.perform(post("/jobs/129/assign/modal").param("technicianId", "2"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("หัวหน้าช่างมีงานซ้อนในช่วงเวลาที่เลือก"));
    }
}

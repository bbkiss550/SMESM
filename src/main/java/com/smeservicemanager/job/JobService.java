package com.smeservicemanager.job;

import com.smeservicemanager.catalog.Product;
import com.smeservicemanager.catalog.ProductRepository;
import com.smeservicemanager.catalog.ServiceCatalog;
import com.smeservicemanager.catalog.ServiceCatalogRepository;
import com.smeservicemanager.customer.Customer;
import com.smeservicemanager.customer.CustomerRepository;
import com.smeservicemanager.notification.Notification;
import com.smeservicemanager.notification.NotificationRepository;
import com.smeservicemanager.security.User;
import com.smeservicemanager.security.UserRepository;
import com.smeservicemanager.shared.domain.DomainTypes.*;
import com.smeservicemanager.shared.exception.BusinessException;
import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import com.smeservicemanager.shared.service.DocumentNumberService;
import com.smeservicemanager.stock.StockTransaction;
import com.smeservicemanager.stock.StockTransactionRepository;
import com.smeservicemanager.storage.StorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
public class JobService {
    private static final Map<JobStatus, Set<JobStatus>> ALLOWED_TRANSITIONS = Map.of(
            JobStatus.NEW, Set.of(JobStatus.SCHEDULED, JobStatus.ASSIGNED, JobStatus.CANCELLED),
            JobStatus.SCHEDULED, Set.of(JobStatus.ASSIGNED, JobStatus.CANCELLED),
            JobStatus.ASSIGNED, Set.of(JobStatus.IN_PROGRESS, JobStatus.CANCELLED),
            JobStatus.IN_PROGRESS, Set.of(JobStatus.WAITING_PART, JobStatus.COMPLETED, JobStatus.CANCELLED),
            JobStatus.WAITING_PART, Set.of(JobStatus.IN_PROGRESS, JobStatus.CANCELLED),
            JobStatus.COMPLETED, Set.of(),
            JobStatus.CANCELLED, Set.of()
    );

    private final JobRepository jobs;
    private final JobItemRepository items;
    private final JobActivityRepository activities;
    private final JobAttachmentRepository attachments;
    private final CustomerRepository customers;
    private final ServiceCatalogRepository services;
    private final ProductRepository products;
    private final UserRepository users;
    private final StockTransactionRepository stockTransactions;
    private final NotificationRepository notifications;
    private final DocumentNumberService numbers;
    private final StorageService storage;

    public JobService(JobRepository jobs, JobItemRepository items, JobActivityRepository activities,
                      JobAttachmentRepository attachments, CustomerRepository customers,
                      ServiceCatalogRepository services, ProductRepository products, UserRepository users,
                      StockTransactionRepository stockTransactions, NotificationRepository notifications,
                      DocumentNumberService numbers, StorageService storage) {
        this.jobs = jobs; this.items = items; this.activities = activities; this.attachments = attachments;
        this.customers = customers; this.services = services; this.products = products; this.users = users;
        this.stockTransactions = stockTransactions; this.notifications = notifications;
        this.numbers = numbers; this.storage = storage;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public Job create(JobForm form) {
        validateSchedule(form.getAppointmentStart(), form.getAppointmentEnd(), form.getTechnicianId(), null);
        Customer customer = customers.findById(form.getCustomerId()).orElseThrow(() -> new ResourceNotFoundException("ไม่พบลูกค้า"));
        ServiceCatalog service = services.findById(form.getServiceId()).orElseThrow(() -> new ResourceNotFoundException("ไม่พบบริการ"));

        Job job = new Job();
        job.setJobNo(numbers.next("JOB", "JOB"));
        job.setCustomer(customer);
        job.setService(service);
        job.setTitle(form.getTitle().trim());
        job.setDescription(form.getDescription());
        job.setPriority(form.getPriority());
        job.setJobType(form.getJobType());
        job.setAppointmentStart(form.getAppointmentStart());
        job.setAppointmentEnd(form.getAppointmentEnd());
        job.setContactPhone(form.getContactPhone());
        job.setAddress(form.getAddress().trim());
        job.setInternalNote(form.getInternalNote());
        job.setUnderWarranty(form.isUnderWarranty());
        job.setChargeable(form.isChargeable());
        job.setVatMode(form.getVatMode());
        job.setDiscount(form.getDiscount() == null ? BigDecimal.ZERO : form.getDiscount());
        if (form.getOriginalJobId() != null) {
            job.setOriginalJob(jobs.findById(form.getOriginalJobId()).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงานต้นฉบับ")));
            job.setJobType(JobType.REWORK);
            job.setReworkReason(form.getReworkReason());
        }
        if (form.getTechnicianId() != null) {
            User technician = technician(form.getTechnicianId());
            job.setTechnician(technician);
            job.setJobStatus(JobStatus.ASSIGNED);
        } else {
            job.setJobStatus(JobStatus.SCHEDULED);
        }
        jobs.save(job);

        JobItem serviceItem = new JobItem();
        serviceItem.setJob(job);
        serviceItem.setItemType(JobItemType.SERVICE);
        serviceItem.setDescription(service.getServiceName());
        serviceItem.setQuantity(BigDecimal.ONE);
        serviceItem.setUnit("งาน");
        serviceItem.setUnitPrice(form.isChargeable() ? service.getDefaultPrice() : BigDecimal.ZERO);
        serviceItem.calculateTotal();
        items.save(serviceItem);
        job.getItems().add(serviceItem);
        job.recalculateTotals();

        addActivity(job, "JOB_CREATED", "สร้างใบงาน " + job.getJobNo(), null, job.getJobStatus().name());
        if (job.getTechnician() != null) {
            addActivity(job, "TECHNICIAN_ASSIGNED", "มอบหมายงานให้ " + job.getTechnician().getFullName(), null, job.getTechnician().getUsername());
            notifyUser(job.getTechnician(), "JOB_ASSIGNED", "ได้รับมอบหมายงาน", job.getJobNo() + " · " + job.getTitle(), "/my-jobs/" + job.getId());
        }
        return jobs.save(job);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public void assign(Long jobId, Long technicianId) {
        Job job = jobs.findByIdForUpdate(jobId).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงาน"));
        if (!Set.of(JobStatus.NEW, JobStatus.SCHEDULED, JobStatus.ASSIGNED).contains(job.getJobStatus())) {
            throw new BusinessException("เปลี่ยนหัวหน้าช่างได้เฉพาะงานที่ยังไม่เริ่มดำเนินการ");
        }
        User newTechnician = technician(technicianId);
        validateSchedule(job.getAppointmentStart(), job.getAppointmentEnd(), newTechnician.getId(), job.getId());
        String previousTechnician = job.getTechnician() == null ? null : job.getTechnician().getFullName();
        job.setTechnician(newTechnician);
        job.setJobStatus(JobStatus.ASSIGNED);
        jobs.save(job);
        addActivity(job, "TECHNICIAN_ASSIGNED", "มอบหมายงานให้ " + newTechnician.getFullName(), previousTechnician, newTechnician.getFullName());
        notifyUser(newTechnician, "JOB_ASSIGNED", "ได้รับมอบหมายงาน", job.getJobNo() + " · " + job.getTitle(), "/my-jobs/" + job.getId());
    }

    @Transactional
    public void accept(Long jobId) {
        Job job = ownedJobForUpdate(jobId);
        if (job.getJobStatus() != JobStatus.ASSIGNED) throw new BusinessException("รับงานได้เฉพาะงานที่มอบหมายแล้ว");
        addActivity(job, "JOB_ACCEPTED", "หัวหน้าช่างรับงานแล้ว", null, currentUsername());
    }

    @Transactional
    public void transition(Long jobId, JobStatus target, String reason) {
        Job job = ownedJobForUpdate(jobId);
        JobStatus current = job.getJobStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessException("ไม่สามารถเปลี่ยนสถานะจาก " + current + " เป็น " + target);
        }
        if (target == JobStatus.CANCELLED && (reason == null || reason.isBlank())) {
            throw new BusinessException("กรุณาระบุเหตุผลที่ยกเลิกงาน");
        }
        job.setJobStatus(target);
        if (target == JobStatus.COMPLETED) job.setCompletedDate(LocalDateTime.now());
        if (target == JobStatus.CANCELLED) job.setCancelReason(reason);
        jobs.save(job);
        String activityType = target == JobStatus.IN_PROGRESS ? "JOB_STARTED" : target == JobStatus.COMPLETED ? "JOB_COMPLETED" : "STATUS_CHANGED";
        addActivity(job, activityType, "เปลี่ยนสถานะเป็น " + target, current.name(), target.name());
    }

    @Transactional
    public JobItem addItem(Long jobId, JobItemForm form) {
        Job job = ownedJobForUpdate(jobId);
        ensureEditable(job);
        JobItem item = new JobItem();
        item.setJob(job);
        item.setItemType(form.getItemType());
        item.setQuantity(form.getQuantity());
        item.setUnit(form.getUnit());
        item.setUnitPrice(form.getUnitPrice());
        if (form.getItemType() == JobItemType.PRODUCT) {
            if (form.getProductId() == null) throw new BusinessException("กรุณาเลือกสินค้า/อะไหล่");
            Product product = products.findByIdForUpdate(form.getProductId()).orElseThrow(() -> new ResourceNotFoundException("ไม่พบสินค้า"));
            if (product.getStockQty().compareTo(form.getQuantity()) < 0) throw new BusinessException("สต๊อก " + product.getProductName() + " ไม่เพียงพอ");
            BigDecimal before = product.getStockQty();
            product.setStockQty(before.subtract(form.getQuantity()));
            item.setProduct(product);
            item.setDescription(product.getProductName());
            item.setUnit(product.getUnit());
            item.setUnitPrice(product.getSalePrice());
            saveStock(product, job, StockTransactionType.OUT, form.getQuantity(), before, product.getStockQty(), job.getJobNo(), "เบิกใช้ในใบงาน");
        } else {
            if (form.getDescription() == null || form.getDescription().isBlank()) throw new BusinessException("กรุณาระบุรายละเอียดรายการ");
            item.setDescription(form.getDescription().trim());
        }
        item.calculateTotal();
        items.save(item);
        job.getItems().add(item);
        job.recalculateTotals();
        jobs.save(job);
        addActivity(job, "PRODUCT_ADDED", "เพิ่มรายการ " + item.getDescription(), null, item.getQuantity().toPlainString());
        return item;
    }

    @Transactional
    public void removeItem(Long jobId, Long itemId) {
        Job job = ownedJobForUpdate(jobId);
        ensureEditable(job);
        JobItem item = items.findById(itemId).filter(i -> i.getJob().getId().equals(jobId))
                .orElseThrow(() -> new ResourceNotFoundException("ไม่พบรายการ"));
        if (item.getItemType() == JobItemType.PRODUCT && item.getProduct() != null) {
            Product product = products.findByIdForUpdate(item.getProduct().getId()).orElseThrow();
            BigDecimal before = product.getStockQty();
            product.setStockQty(before.add(item.getQuantity()));
            saveStock(product, job, StockTransactionType.RETURN, item.getQuantity(), before, product.getStockQty(), job.getJobNo(), "คืนจากการลบรายการในใบงาน");
        }
        item.deactivate();
        items.save(item);
        job.recalculateTotals();
        jobs.save(job);
        addActivity(job, "PRODUCT_REMOVED", "นำรายการออก " + item.getDescription(), item.getQuantity().toPlainString(), null);
    }

    @Transactional
    public JobAttachment addAttachment(Long jobId, AttachmentType type, MultipartFile file) {
        Job job = ownedJobForUpdate(jobId);
        ensureEditable(job);
        StorageService.StoredFile stored = storage.store(file);
        JobAttachment attachment = new JobAttachment();
        attachment.setJob(job); attachment.setAttachmentType(type);
        attachment.setOriginalFileName(stored.originalName()); attachment.setStoredFileName(stored.storedName());
        attachment.setFilePath(stored.relativePath()); attachment.setContentType(stored.contentType()); attachment.setFileSize(stored.size());
        attachments.save(attachment);
        addActivity(job, "ATTACHMENT_UPLOADED", "อัปโหลดไฟล์ " + stored.originalName(), null, type.name());
        return attachment;
    }

    @Transactional(readOnly = true)
    public Job requireDetailed(Long id) {
        Job job = jobs.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงาน"));
        verifyTechnicianOwnership(job);
        job.getCustomer().getPhones().size();
        job.getItems().forEach(item -> { if (item.getProduct() != null) item.getProduct().getProductName(); });
        job.getActivities().forEach(activity -> { if (activity.getUser() != null) activity.getUser().getUsername(); });
        job.getAttachments().size();
        return job;
    }

    private Job ownedJobForUpdate(Long id) {
        Job job = jobs.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบใบงาน"));
        verifyTechnicianOwnership(job);
        return job;
    }

    private void verifyTechnicianOwnership(Job job) {
        boolean technicianRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TECHNICIAN"));
        if (technicianRole && (job.getTechnician() == null || !job.getTechnician().getUsername().equals(currentUsername()))) {
            throw new BusinessException("คุณไม่มีสิทธิ์เข้าถึงใบงานนี้");
        }
    }

    private void validateSchedule(LocalDateTime start, LocalDateTime end, Long technicianId, Long excludeJobId) {
        if (start == null || end == null || !end.isAfter(start)) throw new BusinessException("เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่ม");
        if (technicianId != null && jobs.hasScheduleConflict(technicianId, excludeJobId, start, end)) {
            throw new BusinessException("หัวหน้าช่างมีงานซ้อนในช่วงเวลาที่เลือก");
        }
    }

    private User technician(Long id) {
        User user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("ไม่พบหัวหน้าช่าง"));
        if (!user.isActive() || !"TECHNICIAN".equals(user.getRole().getCode())) throw new BusinessException("ผู้ใช้นี้ไม่ใช่หัวหน้าช่างที่พร้อมรับงาน");
        return user;
    }

    private void ensureEditable(Job job) {
        if (job.getJobStatus() == JobStatus.COMPLETED || job.getJobStatus() == JobStatus.CANCELLED) {
            throw new BusinessException("ใบงานที่ปิดหรือยกเลิกแล้วไม่สามารถแก้ไขได้");
        }
    }

    private void addActivity(Job job, String type, String description, String oldValue, String newValue) {
        JobActivity activity = new JobActivity();
        activity.setJob(job); activity.setActivityType(type); activity.setDescription(description);
        activity.setOldValue(oldValue); activity.setNewValue(newValue); activity.setActivityDate(LocalDateTime.now());
        users.findByUsernameIgnoreCase(currentUsername()).ifPresent(activity::setUser);
        activities.save(activity);
    }

    private void notifyUser(User user, String type, String title, String message, String url) {
        Notification n = new Notification(); n.setUser(user); n.setType(type); n.setTitle(title); n.setMessage(message); n.setTargetUrl(url);
        notifications.save(n);
    }

    private void saveStock(Product product, Job job, StockTransactionType type, BigDecimal qty,
                           BigDecimal before, BigDecimal after, String reference, String note) {
        products.save(product);
        StockTransaction tx = new StockTransaction(); tx.setProduct(product); tx.setJob(job); tx.setTransactionType(type);
        tx.setQuantity(qty); tx.setQuantityBefore(before); tx.setQuantityAfter(after); tx.setReference(reference); tx.setNote(note);
        stockTransactions.save(tx);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}

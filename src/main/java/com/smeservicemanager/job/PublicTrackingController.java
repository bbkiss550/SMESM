package com.smeservicemanager.job;

import com.smeservicemanager.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class PublicTrackingController {
    private final JobRepository jobs;

    public PublicTrackingController(JobRepository jobs) { this.jobs = jobs; }

    @GetMapping("/track/{token}")
    public String track(@PathVariable UUID token, Model model) {
        Job job = jobs.findByPublicToken(token).orElseThrow(() -> new ResourceNotFoundException("ไม่พบข้อมูลติดตามงาน"));
        job.getAttachments().size();
        model.addAttribute("job", job);
        return "public/tracking";
    }
}

package com.smeservicemanager.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
public class SettingService {
    private static final Set<String> EDITABLE=Set.of("company.name","company.taxId","company.address","company.phone","company.email","company.website","tax.enabled","tax.rate","notification.newJob","notification.appointment","notification.payment","notification.dailySummary");
    private final SettingRepository settings;
    public SettingService(SettingRepository settings){this.settings=settings;}
    @Transactional public void update(Map<String,String> values){values.forEach((key,value)->{if(EDITABLE.contains(key))settings.findByKey(key).ifPresent(s->s.setValue(value));});}
}

package com.smeservicemanager.settings;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.util.MultiValueMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    private final SettingRepository settings;private final SettingService service;
    public SettingsController(SettingRepository settings,SettingService service){this.settings=settings;this.service=service;}
    @GetMapping public String index(Model model){model.addAttribute("settings",settings.findAll().stream().collect(Collectors.toMap(Setting::getKey,s->s.getValue()==null?"":s.getValue())));return "settings/index";}
    @PostMapping public String update(@RequestParam MultiValueMap<String,String> submitted,RedirectAttributes r){Map<String,String> values=new LinkedHashMap<>();submitted.forEach((key,list)->values.put(key,list.getLast()));service.update(values);r.addFlashAttribute("successMessage","บันทึกการตั้งค่าแล้ว");return "redirect:/settings";}
}

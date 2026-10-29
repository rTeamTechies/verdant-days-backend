package com.verdantdays.service;

import com.verdantdays.model.BillSettings;
import com.verdantdays.repository.BillSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillSettingsService {

    @Autowired
    private BillSettingsRepository repository;

    @Transactional
    public BillSettings get() {
        BillSettings current = repository.findAll().stream().findFirst().orElse(null);
        if (current == null) {
            return createDefault();
        }
        boolean changed = false;
        if (current.getBusinessName() == null || current.getBusinessName().isBlank()
                || "Verdant Days".equals(current.getBusinessName().trim())) {
            current.setBusinessName(BillSettings.DEFAULT_BUSINESS_NAME);
            changed = true;
        }
        if (current.getAddress() == null || current.getAddress().isBlank()) {
            current.setAddress(BillSettings.DEFAULT_ADDRESS);
            changed = true;
        }
        if (current.getPhone() == null || current.getPhone().isBlank()) {
            current.setPhone(BillSettings.DEFAULT_PHONE);
            changed = true;
        }
        if ("Pure, Honest Oils".equals(current.getTagline())) {
            current.setTagline("");
            changed = true;
        }
        if (current.getOrderWhatsappTemplate() == null || current.getOrderWhatsappTemplate().isBlank()) {
            current.setOrderWhatsappTemplate(BillSettings.DEFAULT_ORDER_WHATSAPP);
            changed = true;
        }
        if (current.getReminderSubscribedTemplate() == null || current.getReminderSubscribedTemplate().isBlank()) {
            current.setReminderSubscribedTemplate(BillSettings.DEFAULT_REMINDER_SUBSCRIBED);
            changed = true;
        }
        if (current.getReminderNonSubscribedTemplate() == null || current.getReminderNonSubscribedTemplate().isBlank()) {
            current.setReminderNonSubscribedTemplate(BillSettings.DEFAULT_REMINDER_NON_SUBSCRIBED);
            changed = true;
        }
        return changed ? repository.save(current) : current;
    }

    @Transactional
    public BillSettings save(BillSettings incoming) {
        BillSettings current = get();
        if (incoming.getBusinessName() != null && !incoming.getBusinessName().isBlank()) {
            current.setBusinessName(incoming.getBusinessName().trim());
        }
        current.setTagline(incoming.getTagline() != null ? incoming.getTagline().trim() : "");
        current.setAddress(incoming.getAddress() != null ? incoming.getAddress().trim() : "");
        current.setPhone(incoming.getPhone() != null ? incoming.getPhone().trim() : "");
        current.setGstin(incoming.getGstin() != null ? incoming.getGstin().trim() : "");
        current.setFooterNote(incoming.getFooterNote() != null ? incoming.getFooterNote().trim() : "");
        current.setShowMrp(incoming.isShowMrp());
        if (incoming.getOrderWhatsappTemplate() != null && !incoming.getOrderWhatsappTemplate().isBlank()) {
            current.setOrderWhatsappTemplate(incoming.getOrderWhatsappTemplate());
        }
        if (incoming.getReminderSubscribedTemplate() != null && !incoming.getReminderSubscribedTemplate().isBlank()) {
            current.setReminderSubscribedTemplate(incoming.getReminderSubscribedTemplate());
        }
        if (incoming.getReminderNonSubscribedTemplate() != null && !incoming.getReminderNonSubscribedTemplate().isBlank()) {
            current.setReminderNonSubscribedTemplate(incoming.getReminderNonSubscribedTemplate());
        }
        return repository.save(current);
    }

    private BillSettings createDefault() {
        BillSettings s = new BillSettings();
        s.setBusinessName(BillSettings.DEFAULT_BUSINESS_NAME);
        s.setTagline("");
        s.setAddress(BillSettings.DEFAULT_ADDRESS);
        s.setPhone(BillSettings.DEFAULT_PHONE);
        s.setOrderWhatsappTemplate(BillSettings.DEFAULT_ORDER_WHATSAPP);
        s.setReminderSubscribedTemplate(BillSettings.DEFAULT_REMINDER_SUBSCRIBED);
        s.setReminderNonSubscribedTemplate(BillSettings.DEFAULT_REMINDER_NON_SUBSCRIBED);
        return repository.save(s);
    }
}

package com.verdantdays.model;

import jakarta.persistence.*;

@Entity
@Table(name = "bill_settings")
public class BillSettings {

    public static final String DEFAULT_BUSINESS_NAME = "Verdant Days Organics Pvt Ltd";
    public static final String DEFAULT_ADDRESS = "13B, Happinest OMR\nKalavakkam,\nThiruporur - 603110";
    public static final String DEFAULT_PHONE = "9894018993";

    public static final String DEFAULT_ORDER_WHATSAPP =
            "🌿 Verdant Days – Order Confirmation (Cold-Pressed Oils)\n\n"
                    + "📋 Order #{{orderNumber}}\n\n"
                    + "Thank you for your purchase from Verdant Days 🙏\n\n"
                    + "{{items}}\n\n"
                    + "{{discountLine}}"
                    + "{{amountLine}}\n"
                    + "💳 Payment: {{paymentType}} - {{paymentStatus}}\n\n"
                    + "This is a fresh batch of traditional cold-pressed oil,\n"
                    + "✔ No chemicals\n"
                    + "✔ No additives\n"
                    + "✔ Small-batch freshness\n\n"
                    + "🎁Loyalty Gift: 500ml Groundnut Oil after every 3 months of regular order (Min. 3L per month, including Sesame oil).\n\n"
                    + "Thank you for supporting Verdant Days – Pure, Honest oils.\n\n"
                    + "☎️For orders, please Whatsapp or call this number.";

    public static final String DEFAULT_REMINDER_SUBSCRIBED =
            "Hi 😊\n\n"
                    + "This is a friendly reminder for this month's fresh oil delivery.\n\n"
                    + "Please confirm your quantity so we can schedule your supply from the current batch.\n\n"
                    + "Thank you as always 🙏\n"
                    + "— Verdant Days";

    public static final String DEFAULT_REMINDER_NON_SUBSCRIBED =
            "Hi 😊\n\n"
                    + "Our fresh small-batch cold-pressed oil is ready, and we are scheduling deliveries soon.\n\n"
                    + "Kindly share your requirement if you would like to be included in this batch. We prepare in limited quantities.\n\n"
                    + "Thank you for your support 🙏\n"
                    + "— Verdant Days";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "business_name", nullable = false, length = 120)
    private String businessName = DEFAULT_BUSINESS_NAME;

    @Column(name = "tagline", length = 200)
    private String tagline = "";

    @Column(name = "address", length = 500)
    private String address = DEFAULT_ADDRESS;

    @Column(name = "phone", length = 30)
    private String phone = DEFAULT_PHONE;

    @Column(name = "gstin", length = 30)
    private String gstin = "";

    @Column(name = "footer_note", length = 500)
    private String footerNote = "Thank you for your purchase!";

    @Column(name = "show_mrp", nullable = false)
    private boolean showMrp = false;

    @Lob
    @Column(name = "order_whatsapp_template", columnDefinition = "TEXT")
    private String orderWhatsappTemplate = DEFAULT_ORDER_WHATSAPP;

    @Lob
    @Column(name = "reminder_subscribed_template", columnDefinition = "TEXT")
    private String reminderSubscribedTemplate = DEFAULT_REMINDER_SUBSCRIBED;

    @Lob
    @Column(name = "reminder_non_subscribed_template", columnDefinition = "TEXT")
    private String reminderNonSubscribedTemplate = DEFAULT_REMINDER_NON_SUBSCRIBED;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
    public String getFooterNote() { return footerNote; }
    public void setFooterNote(String footerNote) { this.footerNote = footerNote; }
    public boolean isShowMrp() { return showMrp; }
    public void setShowMrp(boolean showMrp) { this.showMrp = showMrp; }
    public String getOrderWhatsappTemplate() { return orderWhatsappTemplate; }
    public void setOrderWhatsappTemplate(String orderWhatsappTemplate) { this.orderWhatsappTemplate = orderWhatsappTemplate; }
    public String getReminderSubscribedTemplate() { return reminderSubscribedTemplate; }
    public void setReminderSubscribedTemplate(String reminderSubscribedTemplate) { this.reminderSubscribedTemplate = reminderSubscribedTemplate; }
    public String getReminderNonSubscribedTemplate() { return reminderNonSubscribedTemplate; }
    public void setReminderNonSubscribedTemplate(String reminderNonSubscribedTemplate) { this.reminderNonSubscribedTemplate = reminderNonSubscribedTemplate; }
}

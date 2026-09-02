package com.barangay.zshared.dataseeds;

import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * ClearanceTemplateDataSeeds.java
 * Initializer responsible for seeding the 17 active standard Barangay clearance & certificate templates.
 * Directly registers "UTILITY CONNECTION CLEARANCE" (covering electrical, power, and utility connections)
 * without requiring any frontend overrides. Excludes deprecated/archived templates.
 */
@Slf4j
@Component
@Order(8)
@RequiredArgsConstructor
public class ClearanceTemplateDataSeeds implements CommandLineRunner {

    private final CertificateTemplateRepository certificateTemplateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(String... args) {
        seedClearanceTemplates();
    }

    private void seedClearanceTemplates() {
        log.info("Seeding 17 Active Barangay Clearance Templates...");

        User rootUser = userRepository.findAll().stream().findFirst().orElse(null);

        // 1. Barangay Clearance
        createAndSaveTemplate(
                "BARANGAY CLEARANCE",
                "[{\"id\": \"body-1\", \"text\": \"This is to CERTIFY that the person whose name and signature, right thumbmark and picture appeared herein is a bonafide resident of this barangay, requesting for a record and clearance from this office to wit:\", \"isEditable\": true}, {\"id\": \"body-2\", \"text\": \"This is to further certify that upon verification of the records filed in this office, subject individual have: **NO DEROGATORY RECORD**\", \"isEditable\": false}]",
                "[\"full_name\", \"address\", \"date_of_birth\", \"place_of_birth\", \"purpose\", \"residency_since\"]",
                true, true, 6, rootUser
        );

        // 2. Building Clearance
        createAndSaveTemplate(
                "BUILDING CLEARANCE",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}}, {{age}} years old, {{civil_status}}, a bonafide resident of {{address}}, Barangay Ugong, City of Valenzuela, is applying for a building clearance for a {{building_type}} structure located at {{business_address}} with a lot area of {{lot_area}}.\\n\\nThis clearance is issued for whatever legal purpose it may serve.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"building_type\", \"business_address\", \"lot_area\", \"blueprint\", \"picture_area\", \"land_title\", \"owner_id\", \"secretary_cert_or_cedula\"]",
                true, true, 6, rootUser
        );

        // 3. Certification for Bail
        createAndSaveTemplate(
                "CERTIFICATION FOR BAIL",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}}, {{age}} years old, {{civil_status}}, a bonafide resident of {{address}}, Barangay Ugong, City of Valenzuela, is a bonafide resident in good standing, vouching for {{defendant_name}}.\\n\\nPertinent case details: {{case_details}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"defendant_name\", \"case_details\", \"house_sketch\", \"police_report\", \"representative_id\", \"defendant_id\"]",
                false, false, 3, rootUser
        );

        // 4. Certification for Deceased
        createAndSaveTemplate(
                "CERTIFICATION FOR DECEASED",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{deceased_name}}, deceased, formerly residing at {{address}}, passed away on {{date_of_death}}.\\n\\nThis certification is issued upon the request of {{full_name}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"deceased_name\", \"date_of_death\", \"death_certificate\", \"deceased_or_representative_id\"]",
                false, false, 6, rootUser
        );

        // 5. Contractor Permit
        createAndSaveTemplate(
                "CONTRACTOR PERMIT",
                "[{\"id\": \"body-1\", \"text\": \"This permit is issued to {{full_name}}, contractor representing {{business_name}}, located at {{business_address}}, to perform construction work.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"business_name\", \"business_address\", \"purpose\", \"moa_agreement\"]",
                false, false, 6, rootUser
        );

        // 6. Demolition Clearance
        createAndSaveTemplate(
                "DEMOLITION CLEARANCE",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}} of {{address}} is cleared to perform demolition of structure at {{business_address}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"business_address\", \"purpose\", \"land_title\", \"owner_id\"]",
                false, false, 6, rootUser
        );

        // 7. Ebike/Etrike Garage
        createAndSaveTemplate(
                "EBIKE/ETRIKE GARAGE",
                "[{\"id\": \"body-1\", \"text\": \"This certifies that {{full_name}} has a valid garage for E-bike/E-trike vehicle at {{address}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\", \"ebike_papers\", \"land_title\", \"hoa_certificate\", \"owner_id\"]",
                false, false, 12, rootUser
        );

        // 8. Utility Connection Clearance (Directly named, replaces electrical clearance override)
        createAndSaveTemplate(
                "UTILITY CONNECTION CLEARANCE",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}} is cleared for utility installation (electrical/power/service connection) at {{address}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\", \"land_title\", \"hoa_certificate\", \"owner_id\"]",
                false, false, 6, rootUser
        );

        // 9. Etrike Registration
        createAndSaveTemplate(
                "ETRIKE REGISTRATION",
                "[{\"id\": \"body-1\", \"text\": \"This registration certificate is issued to {{full_name}} for the E-trike vehicle with make/model {{make}} and vehicle number {{vehicle_no}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"make\", \"vehicle_no\", \"purpose\", \"ebike_papers\", \"land_title\", \"hoa_certificate\", \"owner_id\"]",
                false, false, 12, rootUser
        );

        // 10. Excavation Clearance
        createAndSaveTemplate(
                "EXCAVATION CLEARANCE",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}} is cleared to perform excavation work at {{business_address}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"business_address\", \"purpose\", \"picture_area\", \"land_title\", \"owner_id\"]",
                false, false, 6, rootUser
        );

        // 11. Good Moral Character
        createAndSaveTemplate(
                "GOOD MORAL CHARACTER",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}}, {{age}} years old, {{civil_status}}, and a resident of {{address}}, is known to be of good moral character and has no pending case or derogatory record in this Barangay.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\", \"valid_id\"]",
                false, false, 6, rootUser
        );

        // 12. Guardianship
        createAndSaveTemplate(
                "GUARDIANSHIP",
                "[{\"id\": \"body-1\", \"text\": \"This certifies that {{full_name}} of {{address}} is the recognized guardian of the child in this Barangay.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\", \"valid_id\"]",
                false, false, 6, rootUser
        );

        // 13. Indigency
        createAndSaveTemplate(
                "INDIGENCY",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}}, {{age}} years old, {{civil_status}}, residing at {{address}}, is a bonafide resident of this Barangay and belongs to the indigent family.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\", \"valid_id\"]",
                false, false, 6, rootUser
        );

        // 14. Residency
        createAndSaveTemplate(
                "RESIDENCY",
                "[{\"id\": \"body-1\", \"text\": \"This is to certify that {{full_name}}, {{age}} years old, residing at {{address}}, is a resident of this Barangay since {{residency_since}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"residency_since\", \"purpose\", \"valid_id\"]",
                false, false, 6, rootUser
        );

        // 15. Telephone Connection
        createAndSaveTemplate(
                "TELEPHONE CONNECTION",
                "[{\"id\": \"body-1\", \"text\": \"This clearance is issued to {{full_name}} to apply for a telephone line connection at {{address}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\", \"land_title_or_hoa_cert\"]",
                false, false, 6, rootUser
        );

        // 16. Tricycle Franchise
        createAndSaveTemplate(
                "TRICYCLE FRANCHISE",
                "[{\"id\": \"body-1\", \"text\": \"This certifies that {{full_name}} is authorized to operate a tricycle service with Plate No. {{plate_no}} and Motor No. {{motor_no}} under franchise.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"plate_no\", \"motor_no\", \"purpose\", \"or_number\", \"cr_number\", \"toda_certificate\", \"land_title\", \"owner_id\", \"tricycle_picture\"]",
                false, false, 12, rootUser
        );

        // 17. Water Connection
        createAndSaveTemplate(
                "WATER CONNECTION",
                "[{\"id\": \"body-1\", \"text\": \"This clearance is issued to {{full_name}} to apply for a water line connection at {{address}}.\", \"isEditable\": true}]",
                "[\"full_name\", \"address\", \"age\", \"gender\", \"civil_status\", \"contact_no\", \"purpose\"]",
                false, false, 6, rootUser
        );

        log.info("Successfully seeded 17 Active Barangay Clearance Templates!");
    }

    private void createAndSaveTemplate(
            String title,
            String bodySectionsJson,
            String issueFieldsJson,
            boolean requiresPhoto,
            boolean requiresThumbmark,
            int validityMonths,
            User rootUser
    ) {
        try {
            CertificateTemplate template = certificateTemplateRepository.findByCertTitle(title)
                    .orElse(new CertificateTemplate());

            template.setCertTitle(title);
            template.setBodySections(mapper.readTree(bodySectionsJson));
            template.setIssueFields(mapper.readTree(issueFieldsJson));
            template.setRequiresPhoto(requiresPhoto);
            template.setRequiresThumbmark(requiresThumbmark);
            template.setValidityMonths(validityMonths);
            template.setFooterText("Not valid without dry seal.");
            template.setIsArchived(false);
            template.setIsDeleted(false);
            template.setIsPermanentDeleted(false);

            if (rootUser != null) {
                template.setCreatedBy(rootUser);
            }

            certificateTemplateRepository.save(template);
            log.info("Seeded clearance template: {}", title);
        } catch (Exception e) {
            log.error("Failed to seed template '{}': {}", title, e.getMessage(), e);
        }
    }
}

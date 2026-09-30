package com.barangay.zshared.dataseeds;

import com.barangay.contentmanagement.model.Announcement;
import com.barangay.contentmanagement.model.BarangayService;
import com.barangay.contentmanagement.model.Official;
import com.barangay.contentmanagement.repository.AnnouncementRepository;
import com.barangay.contentmanagement.repository.OfficialRepository;
import com.barangay.contentmanagement.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

@Component
@Order(12)
@RequiredArgsConstructor
public class CmsDataSeeder implements CommandLineRunner {

    private final AnnouncementRepository announcementRepository;
    private final ServiceRepository serviceRepository;
    private final OfficialRepository officialRepository;

    @Override
    public void run(String... args) {
        seedAnnouncements();
        seedServices();
        seedOfficials();
    }

    private void seedAnnouncements() {
        if (announcementRepository.count() > 0) return;

        Announcement a1 = new Announcement();
        a1.setCategory("Health Advisory");
        a1.setCategoryColor("bg-emerald-500");
        a1.setTitle("Free Dengue Rapid Testing & Clean-up Drive");
        a1.setExcerpt("The Health Center will be conducting free rapid dengue testing and distribution of mosquito repellent lotions across all 15 puroks.");
        a1.setAuthor("Brgy. Health Station");
        a1.setContent("In response to the rainy season, Barangay Ugong is launching an intensified anti-dengue campaign. Starting this Monday, our health workers will provide free rapid diagnostic testing for residents exhibiting fever symptoms. We urge all families to participate in the '4S' strategy to eliminate mosquito breeding grounds.");
        a1.setCreatedAt(LocalDateTime.now().minusDays(1));
        a1.setPublished(true);
        a1.setIsArchived(false);
        a1.setAdditionalImages(new ArrayList<>());

        Announcement a2 = new Announcement();
        a2.setCategory("Advisory");
        a2.setCategoryColor("bg-amber-500");
        a2.setTitle("Scheduled Meralco Maintenance in Zones 4 & 5");
        a2.setExcerpt("Temporary power interruption on Thursday, 9:00 AM to 3:00 PM for line upgrading and pole replacement along M.H. del Pilar.");
        a2.setAuthor("Barangay Secretariat");
        a2.setContent("Meralco has advised our office of scheduled maintenance activities to improve electrical capacity and safety. Affected zones are encouraged to prepare accordingly. Barangay generators will provide emergency power at the Barangay Hall for medical equipment charging.");
        a2.setCreatedAt(LocalDateTime.now().minusDays(3));
        a2.setPublished(true);
        a2.setIsArchived(false);
        a2.setAdditionalImages(new ArrayList<>());

        Announcement a3 = new Announcement();
        a3.setCategory("Community Program");
        a3.setCategoryColor("bg-blue-500");
        a3.setTitle("First-Time Jobseekers Assistance Desk Open Every Wednesday");
        a3.setExcerpt("New graduates and youth seeking employment can avail of free barangay certifications and oath of undertaking pursuant to RA 11261.");
        a3.setAuthor("Youth & Employment Desk");
        a3.setContent("Barangay Ugong is committed to supporting young job seekers. Bring a valid government ID or school ID and proof of residency to claim your free FTJS certification needed for government-issued pre-employment requirements.");
        a3.setCreatedAt(LocalDateTime.now().minusDays(5));
        a3.setPublished(true);
        a3.setIsArchived(false);
        a3.setAdditionalImages(new ArrayList<>());

        announcementRepository.saveAll(Arrays.asList(a1, a2, a3));
    }

    private void seedServices() {
        if (serviceRepository.count() > 0) return;

        BarangayService s1 = new BarangayService();
        s1.setIconName("FileText");
        s1.setTitle("Barangay Clearance");
        s1.setType("DOCUMENT");
        s1.setDescription("Official certification issued to individuals residing in Barangay Ugong for employment, postal ID, and business compliance.");
        s1.setFee("₱50.00");
        s1.setProcessingTime("10–15 Minutes");
        s1.setLocation("Ground Floor, Window 2");
        s1.setOperatingDays("Monday to Friday");
        s1.setOperatingHours("8:00 AM – 5:00 PM");
        s1.setRequirements("[\"Valid Government-issued ID\", \"Cedula (Community Tax Certificate)\", \"Proof of Residency (Utility Bill or Purok Certificate)\"]");
        s1.setSteps("[\"Get a queue ticket at the entrance\", \"Submit complete requirements at Window 2\", \"Pay the clearance fee at the Cashier\", \"Claim signed and stamped clearance\"]");
        s1.setClaimProcess("Present official receipt at the Release Desk.");
        s1.setCreatedAt(LocalDateTime.now().minusDays(10));
        s1.setIsPublished(true);
        s1.setIsArchived(false);

        BarangayService s2 = new BarangayService();
        s2.setIconName("Shield");
        s2.setTitle("Certificate of Indigency");
        s2.setType("DOCUMENT");
        s2.setDescription("Issued to financially disadvantaged residents requiring medical, legal, educational, or burial assistance from government agencies.");
        s2.setFee("Free (₱0.00)");
        s2.setProcessingTime("15–20 Minutes");
        s2.setLocation("Social Services Desk, Window 4");
        s2.setOperatingDays("Monday to Friday");
        s2.setOperatingHours("8:00 AM – 5:00 PM");
        s2.setRequirements("[\"Valid ID with Ugong Address\", \"Purok Leader Endorsement / Certification\", \"Clinical abstract or request letter (for medical/hospital assistance)\"]");
        s2.setSteps("[\"Submit endorsement from Purok Leader\", \"Undergo quick interview with Social Services Officer\", \"Claim Certificate of Indigency signed by the Punong Barangay\"]");
        s2.setClaimProcess("Released immediately upon approval.");
        s2.setCreatedAt(LocalDateTime.now().minusDays(10));
        s2.setIsPublished(true);
        s2.setIsArchived(false);

        BarangayService s3 = new BarangayService();
        s3.setIconName("Briefcase");
        s3.setTitle("First-Time Jobseeker (RA 11261)");
        s3.setType("DOCUMENT");
        s3.setDescription("Waives government fees on clearances, licenses, and permits for first-time jobseekers residing in the barangay.");
        s3.setFee("Free (₱0.00)");
        s3.setProcessingTime("15 Minutes");
        s3.setLocation("Youth & Employment Desk");
        s3.setOperatingDays("Monday to Friday");
        s3.setOperatingHours("8:00 AM – 5:00 PM");
        s3.setRequirements("[\"Barangay Certification of Residency\", \"Duly signed Oath of Undertaking\", \"One (1) valid 1x1 or 2x2 ID picture\"]");
        s3.setSteps("[\"Fill out the Oath of Undertaking form\", \"Verification of records\", \"Issuance of Certificate\"]");
        s3.setClaimProcess("Direct release to applicant.");
        s3.setCreatedAt(LocalDateTime.now().minusDays(10));
        s3.setIsPublished(true);
        s3.setIsArchived(false);

        serviceRepository.saveAll(Arrays.asList(s1, s2, s3));
    }

    private void seedOfficials() {
        if (officialRepository.count() > 0) return;

        Official o1 = new Official();
        o1.setName("Hon. Maria Elena C. Roxas");
        o1.setPosition("Punong Barangay");
        o1.setType("CAPTAIN");
        o1.setCreatedAt(LocalDateTime.now().minusDays(20));
        o1.setUpdatedAt(LocalDateTime.now());
        o1.setIsArchived(false);

        Official o2 = new Official();
        o2.setName("Hon. Roberto S. Mendoza");
        o2.setPosition("Barangay Kagawad - Committee on Peace & Order");
        o2.setType("KAGAWAD");
        o2.setCreatedAt(LocalDateTime.now().minusDays(20));
        o2.setUpdatedAt(LocalDateTime.now());
        o2.setIsArchived(false);

        Official o3 = new Official();
        o3.setName("Hon. Clarissa T. Cruz");
        o3.setPosition("Barangay Kagawad - Committee on Health & Sanitation");
        o3.setType("KAGAWAD");
        o3.setCreatedAt(LocalDateTime.now().minusDays(20));
        o3.setUpdatedAt(LocalDateTime.now());
        o3.setIsArchived(false);

        Official o4 = new Official();
        o4.setName("Hon. Antonio V. Santos");
        o4.setPosition("Barangay Kagawad - Committee on Infrastructure");
        o4.setType("KAGAWAD");
        o4.setCreatedAt(LocalDateTime.now().minusDays(20));
        o4.setUpdatedAt(LocalDateTime.now());
        o4.setIsArchived(false);

        Official o5 = new Official();
        o5.setName("Hon. Joshua P. Diaz");
        o5.setPosition("SK Chairperson - Committee on Youth & Sports");
        o5.setType("SK");
        o5.setCreatedAt(LocalDateTime.now().minusDays(20));
        o5.setUpdatedAt(LocalDateTime.now());
        o5.setIsArchived(false);

        officialRepository.saveAll(Arrays.asList(o1, o2, o3, o4, o5));
    }
}

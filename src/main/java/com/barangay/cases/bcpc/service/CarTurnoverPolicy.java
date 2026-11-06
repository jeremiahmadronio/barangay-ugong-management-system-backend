package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCustodyLogDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Validations and formatting for BCPC CAR custody handoff and placement records.
 */
public final class CarTurnoverPolicy {

    public static final String PARENT = "Parent / Guardian / Caregiver";
    public static final String TEMPORARY = "Temporarily staying at the barangay while awaiting turnover";
    public static final Set<String> DESTINATIONS = Set.of(
            PARENT,
            "LSWDO",
            "Barangay of Residence",
            "Other Authorized Person",
            TEMPORARY
    );

    private static final ObjectMapper JSON = new ObjectMapper();

    public static ObjectNode receiver(String raw) {
        try {
            JsonNode node = JSON.readTree(raw == null ? "{}" : raw);
            if (node instanceof ObjectNode object) return object;
        } catch (Exception ignored) {
        }
        ObjectNode value = JSON.createObjectNode();
        if (raw != null) value.put("firstName", raw);
        return value;
    }

    public static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public static String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    public static boolean parentRole(String role) {
        if (role == null) return false;
        return role.toLowerCase(Locale.ROOT).matches(".*\\b(parent|parents|mother|father|guardian|caregiver)\\b.*");
    }

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    public static void validate(BcpcCustodyLogDto request, boolean guidance) {
        require(request.getNeedsMedicalAttention() != null, "Answer the Immediate Needs question.");
        String destination = request.getTemporaryPlacement();
        require(destination != null && DESTINATIONS.contains(destination), "Select a valid turnover destination.");

        ObjectNode person = receiver(request.getInitialResponsibility());
        String firstName = text(person, "firstName");
        String role = text(person, "relationship");

        if (PARENT.equals(destination)) {
            require(!firstName.isEmpty(), "Receiving parent / guardian / caregiver is required.");
            require(!role.isEmpty() && !"Other".equalsIgnoreCase(role), "Specify the receiving person's relationship / role.");
            require(!role.toLowerCase(Locale.ROOT).contains("social worker") && !role.toLowerCase(Locale.ROOT).contains("barangay"), "Parent turnover cannot use official agency roles.");
        } else if ("Other Authorized Person".equals(destination)) {
            require(!firstName.isEmpty(), "Receiving person is required for other authorized person turnover.");
            require(!role.isEmpty() && !"Other".equalsIgnoreCase(role), "Specify the receiving person's relationship / role.");
            require(!parentRole(role), "Parent / guardian roles must use Parent / Guardian / Caregiver as destination.");
            require(!role.toLowerCase(Locale.ROOT).contains("social worker"), "Social worker roles should use appropriate LSWDO turnover or referral.");
        } else if (TEMPORARY.equals(destination)) {
            require(!firstName.isEmpty(), "Responsible barangay personnel is required.");
        } else if ("Barangay of Residence".equals(destination)) {
            String receivingBarangay = text(person, "receivingBarangay");
            if (receivingBarangay.isEmpty()) receivingBarangay = text(person, "receivingOffice");
            require(!receivingBarangay.isEmpty(), "Receiving Barangay is required.");
        } else if ("LSWDO".equals(destination)) {
            require(!parentRole(role), "LSWDO turnover cannot use a parent role.");
            String receivingOffice = text(person, "receivingOffice");
            if (receivingOffice.isEmpty()) receivingOffice = text(person, "receivingBarangay");
            if (receivingOffice.isEmpty()) receivingOffice = "LSWDO";
            require(!receivingOffice.isEmpty(), "Receiving Office is required.");
        }

        if (firstName.length() > 50) {
            require(false, "Name cannot exceed 50 characters.");
        }
        if (text(person, "middleName").length() > 50) {
            require(false, "Middle name cannot exceed 50 characters.");
        }
        if (text(person, "lastName").length() > 50) {
            require(false, "Last name cannot exceed 50 characters.");
        }

        if (!blank(request.getContactNumber())) {
            String contact = request.getContactNumber().trim();
            require(contact.matches("^09\\d{9}$"), "Contact number must be exactly 11 digits starting with 09.");
        }

        require(request.getTransferDate() != null && !blank(request.getTransferTime()), "Turnover date and time are required.");
        try {
            LocalTime.parse(request.getTransferTime());
        } catch (Exception e) {
            require(false, "Enter a valid turnover time (e.g., HH:mm).");
        }

        require(!blank(request.getTemporaryPlacementReason()), "Reason / Basis for Turnover is required.");
        if (Boolean.TRUE.equals(request.getNeedsMedicalAttention())) {
            require(!text(person, "immediateActionNotes").isEmpty(), "Immediate Action / Notes is required when medical attention is noted.");
        }
        if (guidance) {
            require(Boolean.TRUE.equals(request.getChildGuidanceProvided()) && Boolean.TRUE.equals(request.getParentAdviceProvided()), "Complete both applicable guidance confirmations.");
        }
    }

    public static Map<String, String> snapshot(BcpcCustodyLogDto request) {
        Map<String, String> fields = new LinkedHashMap<>();
        JsonNode person = receiver(request.getInitialResponsibility());
        fields.put("Destination", request.getTemporaryPlacement());
        fields.put("Receiving person", (text(person, "firstName") + " " + text(person, "middleName") + " " + text(person, "lastName")).trim().replaceAll(" +", " "));
        fields.put("Role", text(person, "relationship"));
        String office = text(person, "receivingOffice");
        if (office.isEmpty()) office = text(person, "receivingBarangay");
        if ("LSWDO".equals(request.getTemporaryPlacement()) && office.isEmpty()) office = "LSWDO";
        fields.put("Receiving office / barangay", office);
        fields.put("Office / Position", text(person, "officePosition"));
        fields.put("Contact", request.getContactNumber());
        fields.put("Date", request.getTransferDate() == null ? "" : request.getTransferDate().toString());
        fields.put("Time", request.getTransferTime());
        fields.put("Reason", request.getTemporaryPlacementReason());
        fields.put("Remarks", request.getTransferNotes());
        fields.put("Immediate attention", String.valueOf(request.getNeedsMedicalAttention()));
        fields.put("Immediate action notes", text(person, "immediateActionNotes"));
        fields.put("Health coordination", String.valueOf(request.getHealthCoordinationConfirmed()));
        fields.put("Child guidance", String.valueOf(request.getChildGuidanceProvided()));
        fields.put("Parent advice", String.valueOf(request.getParentAdviceProvided()));
        fields.put("Guidance notes", request.getGuidanceNotes());
        fields.put("Expected next action", text(person, "expectedNextAction"));
        return fields;
    }

    public static String describe(Map<String, String> fields) {
        StringBuilder out = new StringBuilder();
        fields.forEach((key, value) -> out.append(key).append(": ").append(blank(value) ? "—" : value).append('\n'));
        return out.toString();
    }

    public static String changes(Map<String, String> before, Map<String, String> after) {
        StringBuilder out = new StringBuilder();
        after.forEach((key, value) -> {
            if (!Objects.equals(before.get(key), value)) {
                out.append(key).append("\n  Old: ").append(blank(before.get(key)) ? "—" : before.get(key))
                        .append("\n  New: ").append(blank(value) ? "—" : value).append('\n');
            }
        });
        return out.toString();
    }
}


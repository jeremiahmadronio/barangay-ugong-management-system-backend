package com.barangay.zshared.dto;
/**
 * Request payload for attaching a narrative statement (e.g. sworn statement
 * document) to a case.
 *
 * @param fileName          original file name of the uploaded document
 * @param statementSummary  short summary of the statement's contents
 * @param fileData          raw binary content of the uploaded document
 */
public record NarrativeCreateDto (
        String fileName,
        String statementSummary,
        byte[] fileData

){
}

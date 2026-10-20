package com.barangay.zshared.security;


import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
/**
 * JPA converter that automatically encrypts and decrypts entity fields.
 * Any field annotated with @Convert(converter = EncryptedFieldConverter.class)
 * will be stored encrypted in the database and decrypted when loaded back.

 * Uses a static reference to AesEncryptionService to work around
 * the limitation that JPA converters are not managed by Spring by default.
 */
@Converter
@Component
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static AesEncryptionService encryptionService;

    /**
     * Injects the AesEncryptionService via a static setter.
     * This is needed because JPA instantiates converters directly,
     * not through Spring, so normal @Autowired on a field does not work here.
     */
    @Autowired
    public void setEncryptionService(AesEncryptionService service) {
        EncryptedFieldConverter.encryptionService = service;
    }

    /**
     * Called automatically before saving a value to the database.
     * Encrypts the field value using AesEncryptionService.
     * Throws IllegalStateException if the service is not available,
     * so data is never saved unencrypted.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (encryptionService == null) {
            throw new IllegalStateException(
                    "AesEncryptionService is not initialized. Refusing to save data unencrypted.");
        }
        return encryptionService.encryptSafe(attribute);
    }

    /**
     * Called automatically after loading a value from the database.
     * Decrypts the field value using AesEncryptionService.
     * Throws IllegalStateException if the service is not available.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (encryptionService == null) {
            throw new IllegalStateException(
                    "AesEncryptionService is not initialized. Cannot read encrypted data.");
        }
        return encryptionService.decryptSafe(dbData);
    }
}

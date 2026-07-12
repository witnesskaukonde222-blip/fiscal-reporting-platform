package com.portfolio.fiscal.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Builds the QR payload printed on fiscalized receipts. In a full FDMS
 * integration this would be validated against the device's public key by
 * ZIMRA's verification portal; here we encode the fields a verifier needs
 * plus the verification code derived from the invoice's hash-chain entry.
 */
@Service
public class QrCodeService {

    public String buildPayload(UUID orgId, String invoiceNumber, LocalDate issueDate,
                                long globalReceiptNo, String verificationCode) {
        return String.join("|",
                orgId.toString(),
                invoiceNumber,
                issueDate.toString(),
                String.valueOf(globalReceiptNo),
                verificationCode
        );
    }
}

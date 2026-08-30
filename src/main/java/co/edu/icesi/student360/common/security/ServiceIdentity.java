package co.edu.icesi.student360.common.security;

/** The calling service (issuer) and the service the token was minted for (audience). */
public record ServiceIdentity(String issuer, String audience) {}

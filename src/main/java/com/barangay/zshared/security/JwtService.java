    package com.barangay.zshared.security;


    import io.jsonwebtoken.Claims;
    import io.jsonwebtoken.Jwts;
    import io.jsonwebtoken.SignatureAlgorithm;
    import io.jsonwebtoken.io.Decoders;
    import io.jsonwebtoken.security.Keys;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.stereotype.Service;

    import java.security.Key;
    import java.util.Date;
    import java.util.Map;
    import java.util.function.Function;


    /**
     * Handles everything related to JWT tokens.
     * Creates tokens after login, reads data from tokens,
     * and checks if a token is still valid.
     */
    @Service
    public class JwtService {


        // The secret key used to sign and verify tokens (loaded from application properties)
        @Value("${jwt.secret-key}")
        private String secretKey;

        // How long the token stays valid in milliseconds (loaded from application properties)
        @Value("${jwt.expiration}")
        private long jwtExpiration;

        /** Extracts the subject (username) stored inside a JWT token. */
        public String extractUsername(String token) {
            return extractClaim(token, Claims::getSubject);
        }

        /** Reads any specific piece of data stored inside a JWT token. */
        public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        }


        /** Creates a signed JWT token for the given user with an expiration date. */
        public String generateToken(
                Map<String, Object> extraClaims,
                UserDetails userDetails
        ) {
            return Jwts.builder()
                    .setClaims(extraClaims)
                    .setSubject(userDetails.getUsername())
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                    .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                    .compact();
        }

        /** Checks if the token belongs to the given user and has not expired yet. */
        public boolean isTokenValid(String token, UserDetails userDetails) {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        }

        /** Returns true if the token's expiration date has already passed. */
        private boolean isTokenExpired(String token) {
            return extractClaim(token, Claims::getExpiration).before(new Date());
        }

        /** Opens the token and returns all the data stored inside it. */
        private Claims extractAllClaims(String token) {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        }

        /** Converts the secret key from application properties into a usable signing key. */
        private Key getSignInKey() {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }
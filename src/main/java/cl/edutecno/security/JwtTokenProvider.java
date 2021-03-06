package cl.edutecno.security;

import java.util.Base64;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import cl.edutecno.exception.RestServiceException;
import cl.edutecno.model.Role;
import cl.edutecno.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JwtTokenProvider {

	@Value("${security.jwt.token.secret-key}")
	private String secretKey;

	@Value("${security.jwt.token.expire-length}")
	private long validateInMilliseconds;

	@Autowired
	private UserService userService;

	@PostConstruct
	protected void initi() {
		secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
	}

	public String createToken(String username, Role role) {

		Claims claims = Jwts.claims().setSubject(username);
		SimpleGrantedAuthority sga = new SimpleGrantedAuthority(role.getAuthority());
		claims.put("auth", sga);
//		claims.put("auth", roles.stream().map(s -> new SimpleGrantedAuthority(s.getAuthority()))
//				.filter(Objects::nonNull).collect(Collectors.toList()));
		Date now = new Date();
		Date validity = new Date(now.getTime() + validateInMilliseconds);
		return Jwts.builder().setClaims(claims).setExpiration(validity).signWith(SignatureAlgorithm.HS256, secretKey)
				.compact();
	}

	public Authentication getAuthentication(String token) {
		UserDetails userDetails = userService.loadUserByUsername(getUsername(token));
		return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
	}

	private String getUsername(String token) {
		return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
	}

	public String resolveToken(HttpServletRequest req) {
		String bearerToken = req.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	public boolean validateToken(String token) {

		try {
			Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			throw new RestServiceException("Expired or invalid JWT Token", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}

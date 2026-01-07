package com.securegate.auth;

import com.securegate.auth.dto.LoginRequest;
import com.securegate.auth.dto.SignupRequest;
import com.securegate.entities.Identity;
import com.securegate.repositories.IdentityRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response; // Use standard Response
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Optional;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

/**
 * REST Controller for User Authentication.
 * Provides endpoints for user registration (Sign up) and login.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthenticationController {

    @Inject
    private IdentityRepository identityRepository;

    @Inject
    private SessionService sessionService;

    /**
     * User Registration Endpoint.
     * 
     * @param request The signup request containing username, password, and roles.
     * @return 201 Created on success, 409 Conflict if username exists.
     */
    @POST
    @Path("/signup")
    public Response signup(SignupRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Validation Error", "Username and password are required"))
                    .build();
        }

        if (identityRepository.findByUsername(request.getUsername()).isPresent()) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(error("Duplicate User", "Username already exists"))
                    .build();
        }

        // Hash password
        String hashedPassword = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());

        Identity newUser = new Identity();
        newUser.setId(UUID.randomUUID().toString());
        newUser.setUsername(request.getUsername());
        newUser.setPasswordHash(hashedPassword);
        newUser.setRoles(request.getRoles() != null ? request.getRoles() : java.util.Collections.singleton("user"));

        identityRepository.save(newUser);

        return Response.status(Response.Status.CREATED)
                .entity(Json.createObjectBuilder().add("message", "User created successfully").build())
                .build();
    }

    /**
     * User Login Endpoint.
     * 
     * @param request The login request containing username and password.
     * @return 200 OK with Session Cookie on success, 401 Unauthorized on failure.
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        Optional<Identity> userOpt = identityRepository.findByUsername(request.getUsername());

        // Use BCrypt for checking if password matches
        if (userOpt.isPresent() && BCrypt.checkpw(request.getPassword(), userOpt.get().getPasswordHash())) {
            // Create Session
            String sessionId = sessionService.createSession(request.getUsername());

            // Create HTTP-only cookie
            NewCookie sessionCookie = new NewCookie.Builder("SESSION_ID")
                    .value(sessionId)
                    .path("/")
                    .httpOnly(true)
                    // .secure(true) // Enable in production with HTTPS
                    .maxAge(3600)
                    .build();

            return Response.ok(Json.createObjectBuilder().add("message", "Login successful").build())
                    .cookie(sessionCookie)
                    .build();
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(error("Authentication Failed", "Invalid username or password"))
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam("SESSION_ID") String sessionId) {
        if (sessionId != null) {
            sessionService.invalidateSession(sessionId);
        }
        NewCookie expiredCookie = new NewCookie.Builder("SESSION_ID")
                .value("")
                .path("/")
                .httpOnly(true)
                .maxAge(0) // Expire immediately
                .build();

        return Response.ok(Json.createObjectBuilder().add("message", "Logged out").build())
                .cookie(expiredCookie)
                .build();
    }

    private JsonObject error(String error, String description) {
        return Json.createObjectBuilder()
                .add("error", error)
                .add("error_description", description)
                .build();
    }
}

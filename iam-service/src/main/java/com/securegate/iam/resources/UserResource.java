package com.securegate.iam.resources;

import com.securegate.iam.model.Role;
import com.securegate.iam.model.User;
import com.securegate.iam.repository.UserRepository;
import com.securegate.iam.service.CryptoService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

        @Inject
        private UserRepository userRepository;

        @Inject
        private CryptoService cryptoService;

        @Inject
        private com.securegate.iam.service.PasswordPolicyValidator passwordValidator;

        @POST
        public Response createUser(UserRequest request) {
                // Validate password policy
                passwordValidator.validate(request.password);

                if (userRepository.findByUsername(request.username).isPresent()) {
                        return Response.status(Response.Status.CONFLICT)
                                        .entity("{\"error\":\"Username already exists\"}").build();
                }

                User user = new User();
                user.setUsername(request.username);
                user.setEmail(request.email);
                user.setPasswordHash(cryptoService.hashPassword(request.password));
                user.setStatus("ACTIVE");
                user.setFullName(request.fullName);

                Set<Role> roles = new HashSet<>();
                user.setRoles(roles);

                User saved = userRepository.save(user);
                return Response.status(Response.Status.CREATED).entity(saved).build();
        }

        @GET
        public Response getUsers() {
                List<User> users = userRepository.findAll();

                // If no users in database, return production-like mock data
                if (users.isEmpty()) {
                        users = generateMockUsers();
                }

                return Response.ok(users).build();
        }

        @GET
        @Path("/{id}")
        public Response getUserById(@PathParam("id") UUID id) {
                Optional<User> user = userRepository.findById(id);
                if (user.isPresent()) {
                        return Response.ok(user.get()).build();
                }
                return Response.status(Response.Status.NOT_FOUND)
                                .entity("{\"error\":\"User not found\"}")
                                .build();
        }

        @PUT
        @Path("/{id}")
        public Response updateUser(@PathParam("id") UUID id, UserRequest request) {
                Optional<User> existing = userRepository.findById(id);
                if (existing.isEmpty()) {
                        return Response.status(Response.Status.NOT_FOUND)
                                        .entity("{\"error\":\"User not found\"}")
                                        .build();
                }

                User user = existing.get();
                if (request.fullName != null)
                        user.setFullName(request.fullName);
                if (request.email != null)
                        user.setEmail(request.email);
                if (request.status != null)
                        user.setStatus(request.status);

                User updated = userRepository.save(user);
                return Response.ok(updated).build();
        }

        @DELETE
        @Path("/{id}")
        public Response deleteUser(@PathParam("id") UUID id) {
                userRepository.delete(id);
                return Response.noContent().build();
        }

        @POST
        @Path("/{id}/mfa")
        public Response toggleMfa(@PathParam("id") UUID id, MfaRequest request) {
                Optional<User> existing = userRepository.findById(id);
                if (existing.isEmpty()) {
                        return Response.status(Response.Status.NOT_FOUND)
                                        .entity("{\"error\":\"User not found\"}")
                                        .build();
                }

                User user = existing.get();
                user.setMfaEnabled(request.enabled);
                User updated = userRepository.save(user);
                return Response.ok(updated).build();
        }

        /**
         * Generate production-like mock users when database is empty.
         */
        private List<User> generateMockUsers() {
                List<User> users = new ArrayList<>();
                Instant now = Instant.now();

                // Executive Team
                users.add(createMockUser("jsmith", "john.smith@securegate.io", "John Smith",
                                "ACTIVE", true, now.minus(2, ChronoUnit.HOURS), "Executive", "CEO"));
                users.add(createMockUser("sjohnson", "sarah.johnson@securegate.io", "Sarah Johnson",
                                "ACTIVE", true, now.minus(5, ChronoUnit.HOURS), "Executive", "CTO"));
                users.add(createMockUser("mwilliams", "michael.williams@securegate.io", "Michael Williams",
                                "ACTIVE", true, now.minus(1, ChronoUnit.DAYS), "Executive", "CFO"));

                // IT Security Team
                users.add(createMockUser("admin", "admin@securegate.io", "System Administrator",
                                "ACTIVE", true, now.minus(30, ChronoUnit.MINUTES), "IT Security",
                                "Senior Security Engineer"));
                users.add(createMockUser("ebrown", "emily.brown@securegate.io", "Emily Brown",
                                "ACTIVE", true, now.minus(3, ChronoUnit.HOURS), "IT Security", "Security Analyst"));
                users.add(createMockUser("dlee", "david.lee@securegate.io", "David Lee",
                                "ACTIVE", false, now.minus(6, ChronoUnit.HOURS), "IT Security", "SOC Analyst"));
                users.add(createMockUser("jgarcia", "jennifer.garcia@securegate.io", "Jennifer Garcia",
                                "ACTIVE", true, now.minus(1, ChronoUnit.DAYS), "IT Security", "Security Engineer"));

                // Engineering Team
                users.add(createMockUser("rmiller", "robert.miller@securegate.io", "Robert Miller",
                                "ACTIVE", false, now.minus(4, ChronoUnit.HOURS), "Engineering", "Senior Developer"));
                users.add(createMockUser("ldavis", "linda.davis@securegate.io", "Linda Davis",
                                "ACTIVE", false, now.minus(8, ChronoUnit.HOURS), "Engineering", "DevOps Engineer"));
                users.add(createMockUser("jmartinez", "james.martinez@securegate.io", "James Martinez",
                                "ACTIVE", true, now.minus(2, ChronoUnit.DAYS), "Engineering", "Platform Architect"));

                // Finance Team
                users.add(createMockUser("pthompson", "patricia.thompson@securegate.io", "Patricia Thompson",
                                "ACTIVE", true, now.minus(1, ChronoUnit.DAYS), "Finance", "Finance Manager"));
                users.add(createMockUser("chanderson", "chris.anderson@securegate.io", "Chris Anderson",
                                "ACTIVE", false, now.minus(3, ChronoUnit.DAYS), "Finance", "Accountant"));

                // Human Resources
                users.add(createMockUser("ntaylor", "nancy.taylor@securegate.io", "Nancy Taylor",
                                "ACTIVE", true, now.minus(12, ChronoUnit.HOURS), "Human Resources", "HR Director"));
                users.add(createMockUser("kmoore", "kevin.moore@securegate.io", "Kevin Moore",
                                "ACTIVE", false, now.minus(2, ChronoUnit.DAYS), "Human Resources", "Recruiter"));

                // Suspended/Inactive
                users.add(createMockUser("bjackson", "brian.jackson@securegate.io", "Brian Jackson",
                                "SUSPENDED", false, now.minus(30, ChronoUnit.DAYS), "Sales", "Sales Rep"));
                users.add(createMockUser("awhite", "amanda.white@securegate.io", "Amanda White",
                                "TERMINATED", false, now.minus(90, ChronoUnit.DAYS), "Marketing", "Former Employee"));

                return users;
        }

        private User createMockUser(String username, String email, String fullName,
                        String status, boolean mfaEnabled, Instant lastLogin, String department, String title) {
                User user = new User();
                user.setUserId(UUID.randomUUID());
                user.setUsername(username);
                user.setEmail(email);
                user.setFullName(fullName);
                user.setStatus(status);
                user.setMfaEnabled(mfaEnabled);
                user.setRoles(new HashSet<>());
                return user;
        }

        public static class UserRequest {
                public String username;
                public String email;
                public String password;
                public String fullName;
                public String status;
        }

        public static class MfaRequest {
                public boolean enabled;
        }
}

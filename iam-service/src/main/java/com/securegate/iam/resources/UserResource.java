package com.securegate.iam.resources;

import com.securegate.iam.model.Role;
import com.securegate.iam.model.User;
import com.securegate.iam.repository.RoleRepository;
import com.securegate.iam.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import com.securegate.iam.service.SanitizationService;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

        @Inject
        private UserRepository userRepository;

        @Inject
        private RoleRepository roleRepository;

        @Inject
        private SanitizationService sanitizationService;

        @POST
        @Path("/{username}/promote")
        public Response promoteToAdmin(@PathParam("username") String username, @Context SecurityContext sc) {
                // Security Check: Only an existing ADMIN can promote others
                if (!sc.isUserInRole("ADMIN")) {
                        return Response.status(Response.Status.FORBIDDEN)
                                        .entity("{\"error\":\"Only administrators can promote users\"}").build();
                }

                String sanitizedUsername = sanitizationService.sanitize(username);
                User user = userRepository.findByUsername(sanitizedUsername)
                                .orElseThrow(() -> new NotFoundException("User not found"));

                Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                        Role r = new Role();
                        r.setRoleName("ADMIN");
                        r.setDescription("Administrator with full access");
                        return roleRepository.save(r);
                });

                if (!user.getRoles().contains(adminRole)) {
                        user.getRoles().add(adminRole);
                        userRepository.save(user);
                        return Response.ok("{\"message\":\"User " + sanitizationService.sanitize(username)
                                        + " promoted to ADMIN\"}").build();
                }

                return Response.ok("{\"message\":\"User " + sanitizationService.sanitize(username)
                                + " is already an ADMIN\"}").build();
        }
}

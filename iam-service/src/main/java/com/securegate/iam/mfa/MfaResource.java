package com.securegate.iam.mfa;

import com.securegate.iam.service.TotpService;
import com.securegate.iam.model.User;
import com.securegate.iam.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/mfa")
public class MfaResource {

    @Inject
    private TotpService totpService;

    @Inject
    private UserRepository userRepository;

    // In a real app, this would need the user to be authenticated already via
    // session/cookie
    // For demo, we might pass userId (insecure, but functional for structure)

    @POST
    @Path("/setup")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setupMfa(@FormParam("userId") String userId) {
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Generate Secret (Library based)
        String secretBase32 = totpService.generateSecret();

        // Save to User
        user.setMfaSecretEnc(secretBase32);
        userRepository.save(user);

        return Response.ok("{\"secret\":\"" + secretBase32 + "\", \"qr\":\""
                + totpService.getQrCodeUrl(user.getUsername(), secretBase32) + "\"}").build();
    }

    @POST
    @Path("/verify")
    public Response verifyMfa(@FormParam("userId") String userId, @FormParam("code") int code) {
        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new NotFoundException("User not found"));

        String secretStr = user.getMfaSecretEnc();
        if (secretStr == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("MFA not set up").build();
        }

        boolean valid = totpService.verifyCode(secretStr, code);

        if (valid) {
            user.setMfaEnabled(true);
            userRepository.save(user);
            return Response.ok("{\"valid\":true}").build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"valid\":false}").build();
        }
    }
}

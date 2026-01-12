package com.securegate.iam.mfa;

import com.securegate.iam.model.User;
import com.securegate.iam.repository.UserRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Base64;

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
        // Generate Secret
        byte[] secret = totpService.generateSecret();
        String secretBase32 = Base64.getEncoder().encodeToString(secret); // Should use Base32

        // Save to User (in production encrypt this!)
        // User user = userRepository.findById(...);
        // user.setMfaSecretEnc(secretBase32);
        // userRepository.save(user);

        return Response.ok("{\"secret\":\"" + secretBase32 + "\", \"qr\":\"otpauth://totp/SecureGate?secret="
                + secretBase32 + "\"}").build();
    }

    @POST
    @Path("/verify")
    public Response verifyMfa(@FormParam("userId") String userId, @FormParam("code") int code) {
        // Fetch User and Secret
        // String secretStr = user.getMfaSecretEnc();
        // byte[] secret = Base64.getDecoder().decode(secretStr);

        // boolean valid = totpService.verifyCode(secret, code);
        // if (valid) user.setMfaEnabled(true);

        return Response.ok("{\"valid\":true}").build();
    }
}

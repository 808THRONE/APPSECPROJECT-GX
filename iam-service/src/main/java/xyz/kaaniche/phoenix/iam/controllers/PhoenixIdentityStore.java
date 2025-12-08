package xyz.kaaniche.phoenix.iam.controllers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.credential.UsernamePasswordCredential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import jakarta.transaction.Transactional;
import xyz.kaaniche.phoenix.iam.entities.Identity;
import xyz.kaaniche.phoenix.iam.security.Argon2Utility;

import java.util.*;

@Singleton
@Transactional
public class PhoenixIdentityStore implements IdentityStore {
    @Inject
    private EntityManager entityManager;

    @Override
    public CredentialValidationResult validate(Credential credential){
        if(!(credential instanceof UsernamePasswordCredential upc)){
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        return validate(upc);
    }
    private CredentialValidationResult validate(UsernamePasswordCredential upc){
        try {
            Identity identity = entityManager.
                    createQuery("select i from Identity i where i.username = :username",Identity.class).
                    setParameter("username",upc.getCaller()).getSingleResult();
            Objects.requireNonNull(identity,"Identity should be not null");
            if(Argon2Utility.check(identity.getPassword(),upc.getPassword().getValue())){
                return new CredentialValidationResult(upc.getCaller(),toCallerGroups(identity.getRoles()));
            }
            return CredentialValidationResult.INVALID_RESULT;
        }catch (Throwable e){
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    private Set<String> toCallerGroups(Long roles){
        if(roles == 0L) return Collections.singleton(Role.GUEST.id());
        if(roles == Long.MAX_VALUE) return Collections.singleton(Role.ROOT.id());
        if (roles<0L){
            throw new IllegalArgumentException("Permission level cannot be negative");
        }
        Set<String> ret = new HashSet<>();
        for(long value = 1L; value<=62L; ++value){
            if((value&roles) !=0){
                ret.add(Role.byValue(value));
            }
        }
        return ret;
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        return validationResult.getCallerGroups();
    }
}

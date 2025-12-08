package xyz.kaaniche.phoenix.iam.controllers;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.ws.rs.NotAuthorizedException;
import xyz.kaaniche.phoenix.core.dao.GenericDAO;
import xyz.kaaniche.phoenix.core.entities.SimplePKEntity;
import xyz.kaaniche.phoenix.iam.security.IdentityUtility;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Decorator
@Priority(Interceptor.Priority.APPLICATION)
public abstract class AuthorizationDecorator<E extends SimplePKEntity<ID>, ID extends java.io.Serializable>
        implements GenericDAO<E, ID> {

    @Any
    @Inject
    @Delegate
    private GenericDAO<E, ID> delegate;

    private final EnumMap<Role, Set<Permission>> roleToPermissions = new EnumMap<>(Role.class);

    @Override
    public <S extends E> S save(S entity) {
        authorize(SecureAction.SAVE, entity);
        return delegate.save(entity);
    }

    @Override
    public void delete(E entity) {
        authorize(SecureAction.DELETE, entity);
        delegate.delete(entity);
    }

    private void authorize(SecureAction action, E entity) {
        Set<String> roleNames = IdentityUtility.getRoles();
        if (roleNames == null || roleNames.isEmpty()) {
            // No authorization check if no roles - could throw exception instead
            return;
        }

        Set<Role> roles = roleNames.stream()
                .map(roleName -> {
                    try {
                        return Role.valueOf(roleName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Permission> permissions = roles.stream()
                .map(roleToPermissions::get)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        ID id = entity.getId();
        Class<?> type = entity.getClass();

        Permission requiredPermission = new Permission(action, type, id);
        if (!permissions.contains(requiredPermission)) {
            throw new NotAuthorizedException("Unauthorized action: " + action.name());
        }
    }

    private static class Permission {
        private final String action;
        private final Class<?> type;
        private final Object id;

        public Permission(SecureAction action, Class<?> type, Object id) {
            this.action = action.name();
            this.type = type;
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof Permission))
                return false;
            Permission that = (Permission) obj;
            return Objects.equals(action, that.action) &&
                    Objects.equals(type, that.type) &&
                    Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(action, type, id);
        }
    }

    private enum SecureAction {
        SAVE, EDIT, DELETE
    }

    private enum Role {
        ADMIN, USER
    }
}

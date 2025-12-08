package xyz.kaaniche.phoenix.core.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;

/**
 * Base entity with a compound primary key
 * 
 * @param <PK> the type of the compound primary key
 */
@MappedSuperclass
public abstract class CompoundPKEntity<PK extends CompoundPK> implements Serializable {

    @EmbeddedId
    private PK id;

    public PK getId() {
        return id;
    }

    public void setId(PK id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CompoundPKEntity<?> that = (CompoundPKEntity<?>) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}

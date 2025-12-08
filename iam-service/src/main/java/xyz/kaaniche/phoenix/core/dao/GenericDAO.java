package xyz.kaaniche.phoenix.core.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Generic DAO interface for basic CRUD operations
 * 
 * @param <T>  the entity type
 * @param <ID> the ID type
 */
public interface GenericDAO<T, ID extends Serializable> {

    /**
     * Save or update an entity
     * 
     * @param entity the entity to save
     * @return the saved entity
     */
    <S extends T> S save(S entity);

    /**
     * Find an entity by ID
     * 
     * @param id the entity ID
     * @return an Optional containing the entity if found
     */
    Optional<T> findById(ID id);

    /**
     * Find all entities
     * 
     * @return list of all entities
     */
    List<T> findAll();

    /**
     * Delete an entity
     * 
     * @param entity the entity to delete
     */
    void delete(T entity);

    /**
     * Delete an entity by ID
     * 
     * @param id the entity ID
     */
    void deleteById(ID id);

    /**
     * Check if an entity exists by ID
     * 
     * @param id the entity ID
     * @return true if exists
     */
    boolean existsById(ID id);

    /**
     * Count all entities
     * 
     * @return the count
     */
    long count();
}

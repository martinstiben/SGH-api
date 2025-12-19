package com.horarios.SGH.Repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory para crear consultas dinámicas usando el patrón Factory Method.
 * Implementa el patrón Specification para construir consultas JPA de forma programática.
 *
 * Esta clase centraliza la lógica de construcción de consultas complejas,
 * eliminando duplicación de código y proporcionando una API fluida para consultas dinámicas.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Component
public class RepositoryFactory {

    /**
     * Builder interno para construir Specifications de forma fluida.
     * Implementa el patrón Builder para consultas complejas.
     *
     * @param <T> Tipo de la entidad
     */
    public static class SpecificationBuilder<T> {
        private final List<Specification<T>> specifications = new ArrayList<>();

        /**
         * Agrega un predicado de igualdad.
         *
         * @param field nombre del campo
         * @param value valor a comparar
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> equal(String field, Object value) {
            if (value != null) {
                specifications.add((root, query, cb) -> cb.equal(root.get(field), value));
            }
            return this;
        }

        /**
         * Agrega un predicado de LIKE (búsqueda parcial).
         *
         * @param field nombre del campo
         * @param value valor a buscar
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> like(String field, String value) {
            if (value != null && !value.trim().isEmpty()) {
                specifications.add((root, query, cb) ->
                    cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%"));
            }
            return this;
        }

        /**
         * Agrega un predicado de rango de fechas.
         *
         * @param field     nombre del campo fecha
         * @param startDate fecha de inicio
         * @param endDate   fecha de fin
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> between(String field, LocalDateTime startDate, LocalDateTime endDate) {
            if (startDate != null && endDate != null) {
                specifications.add((root, query, cb) ->
                    cb.between(root.get(field), startDate, endDate));
            }
            return this;
        }

        /**
         * Agrega un predicado de fecha mayor o igual.
         *
         * @param field nombre del campo fecha
         * @param date  fecha límite
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> greaterThanOrEqual(String field, LocalDateTime date) {
            if (date != null) {
                specifications.add((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get(field), date));
            }
            return this;
        }

        /**
         * Agrega un predicado de fecha menor o igual.
         *
         * @param field nombre del campo fecha
         * @param date  fecha límite
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> lessThanOrEqual(String field, LocalDateTime date) {
            if (date != null) {
                specifications.add((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get(field), date));
            }
            return this;
        }

        /**
         * Agrega un predicado IN (valor en lista).
         *
         * @param field  nombre del campo
         * @param values lista de valores
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> in(String field, List<?> values) {
            if (values != null && !values.isEmpty()) {
                specifications.add((root, query, cb) -> root.get(field).in(values));
            }
            return this;
        }

        /**
         * Agrega un predicado de nulidad.
         *
         * @param field nombre del campo
         * @param isNull true para IS NULL, false para IS NOT NULL
         * @return this builder para encadenamiento
         */
        public SpecificationBuilder<T> isNull(String field, boolean isNull) {
            if (isNull) {
                specifications.add((root, query, cb) -> cb.isNull(root.get(field)));
            } else {
                specifications.add((root, query, cb) -> cb.isNotNull(root.get(field)));
            }
            return this;
        }

        /**
         * Construye la Specification final combinando todos los predicados con AND.
         *
         * @return Specification completa
         */
        public Specification<T> build() {
            if (specifications.isEmpty()) {
                return (root, query, cb) -> cb.conjunction();
            }

            Specification<T> result = specifications.get(0);
            for (int i = 1; i < specifications.size(); i++) {
                result = result.and(specifications.get(i));
            }
            return result;
        }

        /**
         * Construye una Specification vacía (sin filtros).
         *
         * @return Specification que devuelve todas las entidades
         */
        public Specification<T> buildAll() {
            return (root, query, cb) -> cb.conjunction();
        }
    }

    /**
     * Crea un nuevo SpecificationBuilder.
     *
     * @param <T> tipo de la entidad
     * @return nuevo builder
     */
    public <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /**
     * Factory method para crear Specifications de búsqueda por término.
     * Implementa búsqueda full-text básica en campos comunes.
     *
     * @param searchTerm término de búsqueda
     * @param searchFields campos donde buscar
     * @param <T> tipo de la entidad
     * @return Specification para búsqueda
     */
    public <T> Specification<T> createSearchSpecification(String searchTerm, List<String> searchFields) {
        if (searchTerm == null || searchTerm.trim().isEmpty() || searchFields == null || searchFields.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> {
            List<Predicate> searchPredicates = new ArrayList<>();
            for (String field : searchFields) {
                searchPredicates.add(cb.like(cb.lower(root.get(field)), "%" + searchTerm.toLowerCase() + "%"));
            }
            return cb.or(searchPredicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Factory method para crear Specifications de entidades activas.
     * Busca entidades que no estén eliminadas lógicamente.
     *
     * @param activeField nombre del campo que indica si está activo
     * @param <T> tipo de la entidad
     * @return Specification para entidades activas
     */
    public <T> Specification<T> createActiveSpecification(String activeField) {
        return (root, query, cb) -> cb.equal(root.get(activeField), true);
    }

    /**
     * Factory method para crear Specifications por rango de fechas.
     *
     * @param dateField nombre del campo fecha
     * @param startDate fecha de inicio
     * @param endDate fecha de fin
     * @param <T> tipo de la entidad
     * @return Specification por rango de fechas
     */
    public <T> Specification<T> createDateRangeSpecification(String dateField,
                                                           LocalDateTime startDate,
                                                           LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate != null && endDate != null) {
                return cb.between(root.get(dateField), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get(dateField), startDate);
            } else if (endDate != null) {
                return cb.lessThanOrEqualTo(root.get(dateField), endDate);
            } else {
                return cb.conjunction();
            }
        };
    }
}
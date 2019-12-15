package com.zeenea.cli.export.xlsx;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Définition de l'export d'un groupe de colonnes.
 *
 * @param <T> Type de l'asset exporté.
 */
public final class ColumnGroupExport<T> {
    private final String label;
    private final List<ColumnExport<T>> columns;

    private ColumnGroupExport(Builder<T> builder) {
        this.label = requireNonNull(builder.label);
        this.columns = builder.columns.build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Libellé du groupe de colonnes.
     *
     * @return Le libellé du groupe de colonnes.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Liste des colonnes à exporter.
     *
     * @return La liste des colonnes.
     */
    public List<ColumnExport<T>> getColumns() {
        return columns;
    }

    /**
     * Nombre de colonnes dans le groupe.
     *
     * @return Le nombre de colonnes.
     */
    public int size() {
        return getColumns().size();
    }

    /**
     * Indique si le groupe contient des colonnes.
     *
     * @return {@code true} si le groupe est vide, {@code false} autrement.
     */
    public boolean isEmpty() {
        return columns.isEmpty();
    }

    public static class Builder<T> {
        private String label;
        private ImmutableList.Builder<ColumnExport<T>> columns = ImmutableList.builder();


        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }

        public Builder<T> addColumn(ColumnExport<T> column) {
            this.columns.add(column);
            return this;
        }

        public ColumnGroupExport<T> build() {
            return new ColumnGroupExport<>(this);
        }

    }
}

package com.zeenea.cli.export.xlsx;

/**
 * Définition de l'export d'une colonne.
 *
 * @param <T> Type de l'asset exporté.
 */
public final class ColumnExport<T> {
    private final String label;
    private final int width;
    private final ValueExporter<T> exporter;

    private ColumnExport(Builder<T> builder) {
        this.label = builder.label;
        this.width = builder.width;
        this.exporter = builder.exporter;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * @return Le libellé de la colonne.
     */
    public String getLabel() {
        return label;
    }

    /**
     * La taille minimale de la colonne.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return L'object qui permet d'exporter une valeur.
     */
    public ValueExporter<T> getExporter() {
        return exporter;
    }

    /**
     * Méthode pratique pour exporter la valeur de la colonne à partir de l'exporter.
     *
     * @param writer Object permettant d'écrire dans la colonne Excel appropriée.
     * @param item   Élément en cours d'export.
     */
    void exportValue(SheetExport.Writer writer, T item) {
        getExporter().export(writer, item);
    }

    public static class Builder<T> {
        private String label;
        private int width = 0;
        private ValueExporter<T> exporter;

        public Builder<T> label(String label) {
            this.label = label;
            return this;
        }

        public Builder<T> width(int width) {
            this.width = width;
            return this;
        }

        public Builder<T> exporter(ValueExporter<T> exporter) {
            this.exporter = exporter;
            return this;
        }

        public ColumnExport<T> build() {
            return new ColumnExport<>(this);
        }

    }
}

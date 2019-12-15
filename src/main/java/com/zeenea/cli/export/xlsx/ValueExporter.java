package com.zeenea.cli.export.xlsx;

/**
 * Interface fonctionnelle base des fonctions d'export d'une valeur d'un élément dans une cellule.
 *
 * @param <T> Type de l'élément à exporter.
 */
@FunctionalInterface
public interface ValueExporter<T> {
    void export(SheetExport.Writer writer, T item);
}

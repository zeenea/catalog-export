package com.zeenea.cli.export.xlsx;

import com.google.common.collect.ImmutableList;
import com.zeenea.client.api.StreamResult;
import com.zeenea.client.api.asset.Description;
import com.zeenea.client.api.id.Identifiant;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.ParametersAreNullableByDefault;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Export vers une feuille dans un classeur Excel.
 *
 * @param <T> Type de l'élément à exporter.
 */
@ParametersAreNonnullByDefault
public final class SheetExport<T> {
    private final String name;
    private final Workbook workbook;
    private final Sheet sheet;
    private final ExportStyles styles;

    @Nullable
    private Long expectedItemCount;
    private final AtomicLong itemCount = new AtomicLong();
    private final AtomicInteger rowCount = new AtomicInteger();
    private final List<ColumnExport<T>> mainSection;
    private final List<ColumnGroupExport<T>> groups;

    private final CreationHelper createHelper;

    /**
     * Construit l'export vers une feuille de calcul.
     *
     * <p>Ce constructeur:</p>
     * <ol>
     *     <li>récupère les paramètres fournis par le builder,</li>
     *     <li>crée une nouvelle feuille de calcul si le builder n'est contient pas,</li>
     *     <li>crée les entêtes de la feuille.</li>
     * </ol>
     *
     * @param builder Le builder contenant la description de l'export.
     */
    private SheetExport(Builder<T> builder) {
        this.name = requireNonNull(builder.name);
        this.styles = requireNonNull(builder.styles);
        this.mainSection = builder.mainSection.build();
        this.groups = builder.groups.build();

        this.workbook = requireNonNull(builder.workbook);
        this.sheet = builder.sheet != null ? builder.sheet : workbook.createSheet(builder.name);

        createHelper = workbook.getCreationHelper();

        createHeaders();
    }

    /**
     * Créer les entêtes de la feuille.
     */
    private void createHeaders() {
        Row groupHeaderRow = sheet.createRow(rowCount.getAndIncrement());
        Row headerRow = sheet.createRow(rowCount.getAndIncrement());

        int colIdx = 0;
        for (ColumnExport<T> columnExport : mainSection) {
            setColumnWidth(colIdx, columnExport);
            Cell cell = headerRow.createCell(colIdx);
            cell.setCellStyle(styles.getMainHeaderStyle());
            cell.setCellValue(columnExport.getLabel());
            ++colIdx;
        }

        int groupIdx = 0;
        for (ColumnGroupExport<T> group : groups) {
            if (group.isEmpty()) continue;

            Cell groupCell = groupHeaderRow.createCell(colIdx);
            groupCell.setCellStyle(styles.getPropertyGroupHeaderStyle(groupIdx));
            groupCell.setCellValue(group.getLabel());

            if (group.size() >= 2) {
                CellRangeAddress groupRegion = new CellRangeAddress(0, 0, colIdx, colIdx + group.size() - 1);
                sheet.addMergedRegion(groupRegion);
                RegionUtil.setBorderTop(BorderStyle.MEDIUM, groupRegion, sheet);
                RegionUtil.setBorderLeft(BorderStyle.MEDIUM, groupRegion, sheet);
                RegionUtil.setBorderRight(BorderStyle.MEDIUM, groupRegion, sheet);
                RegionUtil.setBorderBottom(BorderStyle.THIN, groupRegion, sheet);

                int groupColor = styles.getPropertyGroupColor(groupIdx);
                RegionUtil.setTopBorderColor(groupColor, groupRegion, sheet);
                RegionUtil.setLeftBorderColor(groupColor, groupRegion, sheet);
                RegionUtil.setRightBorderColor(groupColor, groupRegion, sheet);
                RegionUtil.setBottomBorderColor(groupColor, groupRegion, sheet);
            }

            for (ColumnExport<T> columnExport : group.getColumns()) {
                setColumnWidth(colIdx, columnExport);
                Cell colCell = headerRow.createCell(colIdx);
                colCell.setCellStyle(styles.getPropertyHeaderStyle(groupIdx));
                colCell.setCellValue(columnExport.getLabel());
                ++colIdx;
            }

            ++groupIdx;
        }
    }

    /**
     * Défini la taille de la colonne depuis la définition de la colonne à exporter.
     *
     * @param colIdx       Indice de la colonne.
     * @param columnExport Définition de la colonne à exporté.
     */
    private void setColumnWidth(int colIdx, ColumnExport<T> columnExport) {
        int length = Math.max(columnExport.getWidth(), columnExport.getLabel().length());
        sheet.setColumnWidth(colIdx, Math.min(length + 2, 255) << 8);
    }

    /**
     * Export le résutat d'une requête dans l'API Zeenea.
     *
     * @param streamResult Le flux résultat de recherche.
     */
    public void export(StreamResult<T> streamResult) {
        expectedItemCount = sum(expectedItemCount, streamResult.getEstimatedSize());
        streamResult.getStream().forEach(item -> {
            itemCount.incrementAndGet();
            Row row = sheet.createRow(rowCount.getAndIncrement());

            Writer writer = new Writer(row);
            for (ColumnExport<T> columnExport : mainSection) {
                columnExport.exportValue(writer, item);
                writer.forward();
            }

            for (ColumnGroupExport<T> group : groups) {
                if (group.isEmpty()) continue;

                for (ColumnExport<T> columnExport : group.getColumns()) {
                    columnExport.exportValue(writer, item);
                    writer.forward();
                }
            }
        });
    }

    /**
     * Méthode pratique pour additionner deux entiers longs qui peuvent être nulls.
     * <p>Les valeurs nulles sont ignorées, si toutes les valeurs sont nulles, le résultat est null.</p>
     *
     * @param a premier entier
     * @param b deuxième entier
     * @return La somme des nombre ou null s'ils sont tous les deux nulls.
     */
    @Nullable
    private Long sum(@Nullable Long a, @Nullable Long b) {
        if (b == null)
            return a;
        if (a == null)
            return b;
        return a + b;
    }

    /**
     * @return Le nom de la feuille.
     */
    public String getName() {
        return name;
    }

    /**
     * Nombre d'éléments attendus.
     *
     * @return Le nombre d'éléments.
     */
    @Nullable
    public Long getExpectedItemCount() {
        return expectedItemCount;
    }

    /**
     * Modifie le nombre d'éléments attendus.
     *
     * @param expectedItemCount Nouvelle valeur.
     */
    public void setExpectedItemCount(@Nullable Long expectedItemCount) {
        this.expectedItemCount = expectedItemCount;
    }

    /**
     * Nombre d'éléments comptés.
     *
     * @return le nombre d'éléments.
     */
    public AtomicLong getItemCount() {
        return itemCount;
    }

    /**
     * Nombre de lignes dans la feuilles de calcul Excel.
     *
     * @return Le nombre de lignes.
     */
    public AtomicInteger getRowCount() {
        return rowCount;
    }

    /**
     * Liste des exports de colonne de la section principale.
     *
     * @return La liste des
     */
    public List<ColumnExport<T>> getMainSection() {
        return mainSection;
    }

    /**
     * Classe utilitaire permettant à un exporteur de valeur d'écrire dans la cellule courante de la feuille Excel.
     */
    @ParametersAreNullableByDefault
    public class Writer {
        private final Row row;
        private int colIdx = 0;

        private Writer(@Nonnull Row row) {
            this.row = requireNonNull(row);
        }

        public void write(String value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value);
            }
        }

        public <E extends Enum<? extends E>> void write(E value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value.toString());
            }
        }

        public void writeHyperlink(String label, String address) {
            if (label != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(label);

                Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
                link.setAddress(address);
                cell.setHyperlink(link);
                cell.setCellStyle(styles.getDataCellStyle(DataStyle.hyperlinkStyle));
            }
        }


        public void write(Identifiant value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value.getUuid());
                cell.setCellStyle(styles.getDataCellStyle(DataStyle.identifiantStyle));
            }
        }

        public void writeDescription(Description value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value.getText());
                cell.setCellStyle(styles.getDataCellStyle(DataStyle.descriptionStyle));
            }
        }

        public void writeDescription(String value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value);
                cell.setCellStyle(styles.getDataCellStyle(DataStyle.descriptionStyle));
            }
        }

        public void write(Instant value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(Date.from(value));
                cell.setCellStyle(styles.getDataCellStyle(DataStyle.dateStyle));
            }
        }

        public void write(Integer value) {
            if (value != null) {
                write(value.intValue());
            }
        }

        public void write(int value) {
            Cell cell = row.createCell(colIdx);
            cell.setCellValue(value);
            cell.setCellStyle(styles.getDataCellStyle(DataStyle.integerStyle));
        }

        public void write(Long value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value);
                cell.setCellStyle(styles.getDataCellStyle(DataStyle.integerStyle));
            }
        }

        public void write(double value) {
            Cell cell = row.createCell(colIdx);
            cell.setCellValue(value);
            cell.setCellStyle(styles.getDataCellStyle(DataStyle.decimalStyle));
        }

        public void write(Double value) {
            if (value != null) {
                write(value.doubleValue());
            }
        }

        public void write(boolean value) {
            Cell cell = row.createCell(colIdx);
            cell.setCellValue(value);
//            cell.setCellStyle(styles.getDataCellStyle(DataStyle.booleanStyle));
        }

        public void write(BigDecimal value) {
            if (value != null) {
                Cell cell = row.createCell(colIdx);
                cell.setCellValue(value.doubleValue());
                if (value.scale() > 0) {
                    cell.setCellStyle(styles.getDataCellStyle(DataStyle.decimalStyle));
                } else {
                    cell.setCellStyle(styles.getDataCellStyle(DataStyle.integerStyle));
                }
            }
        }

        private void forward() {
            ++colIdx;
        }
    }

    /**
     * Crée une instance d'un monteur d'une instance d'export d'une feuille Excel.
     *
     * @param <T> Type des éléments à exporter.
     * @return La nouvelle instance.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Monteur d'une instance d'export d'une feuille Excel.
     *
     * @param <T> Type des éléments à exporter.
     */
    public static class Builder<T> {
        private String name;
        private Workbook workbook;
        private Sheet sheet;
        private ExportStyles styles;
        private ImmutableList.Builder<ColumnExport<T>> mainSection = ImmutableList.builder();
        private ImmutableList.Builder<ColumnGroupExport<T>> groups = ImmutableList.builder();

        /**
         * Nom de la feuille.
         *
         * @param name nouvelle valeur.
         * @return ce monteur.
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Feuille de calcul Excel.
         * <p>La feuille doit appartenir au classeur fourni avec la méthode {@link Builder#workbook(Workbook workbook)}</p>
         * <p>Cette donnée n'est pas obligatoire, une nouvelle sera créée si elle n'est pas fournie.</p>
         *
         * @param sheet la nouvelle valeur.
         * @return ce monteur.
         */
        public Builder<T> sheet(Sheet sheet) {
            this.sheet = sheet;
            return this;
        }

        /**
         * Classeur Excel.
         *
         * @param workbook la nouvelle valeur.
         * @return ce monteur.
         */
        public Builder<T> workbook(Workbook workbook) {
            this.workbook = workbook;
            return this;
        }

        /**
         * Cache des styles associés
         *
         * @param styles la nouvelle valeur.
         * @return ce monteur.
         */
        public Builder<T> styles(ExportStyles styles) {
            this.styles = styles;
            return this;
        }

        /**
         * Ajoute une colonne dans la section principale.
         *
         * @param factory fabrique d'export de la colonne.
         * @return ce monteur.
         */
        public Builder<T> addColumn(Consumer<ColumnExport.Builder<T>> factory) {
            ColumnExport.Builder<T> builder = ColumnExport.<T>builder();
            factory.accept(builder);
            mainSection.add(builder.build());
            return this;
        }

        /**
         * Ajoute un groupe de colonnes "propriétées".
         *
         * @param factory Fabrique de l'export du groupe.
         * @return ce monteur.
         */
        public Builder<T> addGroup(Consumer<ColumnGroupExport.Builder<T>> factory) {
            ColumnGroupExport.Builder<T> builder = ColumnGroupExport.<T>builder();
            factory.accept(builder);
            groups.add(builder.build());
            return this;
        }


        /**
         * Assemble une nouvelle version d'export de feuille de calcul.
         *
         * @return la nouvelle instance.
         */
        public SheetExport<T> build() {
            return new SheetExport<>(this);
        }
    }
}

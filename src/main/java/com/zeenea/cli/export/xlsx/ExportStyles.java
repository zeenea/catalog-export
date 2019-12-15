package com.zeenea.cli.export.xlsx;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cache contenant les styles de cellules utilisées par l'export.
 * La réutilisation des styles permet une économie circonstanciel de la taille du fichier résultat et de la
 * consommation mémoire associée.
 */
public final class ExportStyles {
    private static final short MAIN_STYLE_BG_COLOR_INDEX = 50;
    private static final short IDENTIFIANT_COLOR_INDEX = 22;
    private static final short[] PROPERTY_GROUP_STYLE_COLOR_INDEX = {51, 52, 53};

    private final SXSSFWorkbook workbook;
    private final Font headerFont;
    private final Map<DataStyle, CellStyle> dataStyles = new EnumMap<>(DataStyle.class);
    private final CellStyle mainHeaderStyle;
    private final List<CellStyle> propertyHeaderStyles = new CopyOnWriteArrayList<>();
    private final List<CellStyle> propertyGroupHeaderStyles = new CopyOnWriteArrayList<>();

    private ExportStyles(SXSSFWorkbook wb) {
        this.workbook = wb;

        CreationHelper creationHelper = wb.getCreationHelper();
        headerFont = wb.createFont();
        headerFont.setBold(true);
        mainHeaderStyle = createStyleWithColor(wb, MAIN_STYLE_BG_COLOR_INDEX, headerFont);

        Font grayFont = wb.createFont();
        grayFont.setFontName("Consolas");
        grayFont.setColor(IDENTIFIANT_COLOR_INDEX);

        CellStyle style;
        style = wb.createCellStyle();
        style.setFont(grayFont);
        dataStyles.put(DataStyle.identifiantStyle, style);

        style = wb.createCellStyle();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        dataStyles.put(DataStyle.dateStyle, style);

        style = wb.createCellStyle();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0"));
        dataStyles.put(DataStyle.integerStyle, style);

        style = wb.createCellStyle();
        style.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0.00"));
        dataStyles.put(DataStyle.decimalStyle, style);

        style = wb.createCellStyle();
        style.setWrapText(true);
        dataStyles.put(DataStyle.descriptionStyle, style);

        Font hlinkFont = wb.createFont();
        hlinkFont.setUnderline(Font.U_SINGLE);
        hlinkFont.setColor(IndexedColors.BLUE.getIndex());
        style = wb.createCellStyle();
        style.setFont(hlinkFont);
        dataStyles.put(DataStyle.hyperlinkStyle, style);
    }

    /**
     * Construit une nouvelle instance de {@code ExportStyles} pour le classeur fournit en argument.
     *
     * @param wb Classeur Excel contenant les styles.
     * @return La nouvelle instance.
     */
    public static ExportStyles of(SXSSFWorkbook wb) {
        return new ExportStyles(wb);
    }


    /**
     * Créer un nouveau style de cellule avec la couleur de fond et la police de caractères fournies en argument.
     *
     * @param wb    Classeur excel.
     * @param color Couleur de fond.
     * @param font  Police de caractères.
     * @return Le nouveau style.
     */
    private static CellStyle createStyleWithColor(SXSSFWorkbook wb, short color, Font font) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }


    /**
     * Style de cellule Excel associé à un type de style.
     *
     * @param dataStyle Type de style.
     * @return le style associé.
     */
    public CellStyle getDataCellStyle(DataStyle dataStyle) {
        return dataStyles.get(dataStyle);
    }

    /**
     * Style des entêtes des colonnes principales.
     *
     * @return Le style.
     */
    public CellStyle getMainHeaderStyle() {
        return mainHeaderStyle;
    }

    /**
     * Style des cellules d'entête des propriétes du groupe d'indice passé en paramètre.
     *
     * @param groupIdx Indice du groupe de la propriété.
     * @return Le syle associé.
     */
    public CellStyle getPropertyHeaderStyle(int groupIdx) {
        int idx = groupIdx % PROPERTY_GROUP_STYLE_COLOR_INDEX.length;
        for (int i = propertyHeaderStyles.size(); i <= idx; ++i) {
            propertyHeaderStyles.add(createStyleWithColor(workbook, PROPERTY_GROUP_STYLE_COLOR_INDEX[idx], headerFont));
        }
        return propertyHeaderStyles.get(idx);
    }

    /**
     * Style des cellules d'entête du groupe de propriétes d'indice passé en paramètre.
     *
     * @param groupIdx Indice du groupe de la propriété.
     * @return Le syle associé.
     */
    public CellStyle getPropertyGroupHeaderStyle(int groupIdx) {
        int idx = groupIdx % PROPERTY_GROUP_STYLE_COLOR_INDEX.length;
        for (int i = propertyGroupHeaderStyles.size(); i <= idx; ++i) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            short groupColor = PROPERTY_GROUP_STYLE_COLOR_INDEX[idx];

            font.setFontName("Consolas");
            font.setColor(groupColor);
            font.setBold(true);
            style.setFont(font);
            style.setAlignment(HorizontalAlignment.CENTER);

            style.setBorderTop(BorderStyle.MEDIUM);
            style.setBorderLeft(BorderStyle.MEDIUM);
            style.setBorderRight(BorderStyle.MEDIUM);
            style.setBorderBottom(BorderStyle.THIN);

            style.setTopBorderColor(groupColor);
            style.setLeftBorderColor(groupColor);
            style.setRightBorderColor(groupColor);
            style.setBottomBorderColor(groupColor);

            propertyGroupHeaderStyles.add(style);
        }
        return propertyGroupHeaderStyles.get(idx);
    }

    /**
     * Couleur  du groupe de propriétés.
     *
     * @param groupIdx Indice du groupe.
     * @return Indice de couleur tel qu'attendu par la méthode {@link org.apache.poi.ss.util.RegionUtil#setTopBorderColor(int, org.apache.poi.ss.util.CellRangeAddress, org.apache.poi.ss.usermodel.Sheet) RegionUtil#setTopBorderColor(color, range, sheet)}
     */
    public int getPropertyGroupColor(int groupIdx) {
        int idx = groupIdx % PROPERTY_GROUP_STYLE_COLOR_INDEX.length;
        return PROPERTY_GROUP_STYLE_COLOR_INDEX[idx];
    }

}

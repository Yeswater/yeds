package com.yeswater.bids.export.infrastructure.excel;

import cn.idev.excel.FastExcel;
import com.yeswater.bids.export.api.ExportColumnSpec;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class FastExcelExportWriter {

    public List<ExportColumnSpec> visibleColumns(List<ExportColumnSpec> columns) {
        return columns.stream()
                .filter(ExportColumnSpec::visible)
                .sorted(Comparator.comparingInt(ExportColumnSpec::sortOrder))
                .toList();
    }

    public void writeXlsx(Path target, List<ExportColumnSpec> columns, List<Map<String, Object>> rows) throws IOException {
        List<ExportColumnSpec> visible = visibleColumns(columns);
        List<List<String>> head = new ArrayList<>();
        for (ExportColumnSpec column : visible) {
            head.add(List.of(column.label()));
        }
        List<List<Object>> data = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            List<Object> line = new ArrayList<>();
            for (ExportColumnSpec column : visible) {
                Object raw = row.get(column.columnName());
                line.add(RowMasker.toCellValue(RowMasker.mask(raw, column.maskType())));
            }
            data.add(line);
        }
        try (OutputStream out = java.nio.file.Files.newOutputStream(target)) {
            FastExcel.write(out).head(head).sheet("data").doWrite(data);
        }
    }
}

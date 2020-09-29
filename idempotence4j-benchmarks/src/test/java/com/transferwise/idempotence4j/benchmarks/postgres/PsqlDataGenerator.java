package com.transferwise.idempotence4j.benchmarks.postgres;

import de.bytefish.pgbulkinsert.row.SimpleRowWriter;
import de.bytefish.pgbulkinsert.util.PostgreSqlUtils;
import org.postgresql.PGConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class PsqlDataGenerator {
    private static final int BATCH_SIZE = 100_000;

    public static void generateActions(DataSource dataSource, int rows) throws ExecutionException, InterruptedException {
        SimpleRowWriter.Table table = tableDefinition();

        int batches = (rows + BATCH_SIZE - 1) / BATCH_SIZE;
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(batches, 10));
        CompletableFuture<?>[] completables = IntStream.rangeClosed(1, batches)
            .boxed()
            .map(i -> CompletableFuture.runAsync(() -> {
                try(Connection connection = dataSource.getConnection()) {
                    int numRows = Math.min(BATCH_SIZE, rows - ((i-1) * BATCH_SIZE));
                    PGConnection pgConnection = PostgreSqlUtils.getPGConnection(connection);
                    batchInsert(pgConnection, table, numRows);
                } catch (Exception exception) { }
            }, pool))
            .toArray(CompletableFuture<?>[]::new);

        CompletableFuture allOf = CompletableFuture.allOf(completables);
        allOf.get();
    }

    private static void batchInsert(PGConnection pgConnection, SimpleRowWriter.Table table, int rows) throws SQLException {
        try (SimpleRowWriter writer = new SimpleRowWriter(table, pgConnection)) {
            for(int rowIdx = 0; rowIdx < rows; rowIdx++) {

                writer.startRow((row) -> {
                    row.setText("key", UUID.randomUUID().toString());
                    row.setText("type", "SOME_ACTION");
                    row.setText("client", "merchant");
                    row.setTimeStamp("created_at", LocalDateTime.MIN);
                });
            }
        }
    }

    private static SimpleRowWriter.Table tableDefinition() {
        String[] columns = new String[] {
            "key",
            "type",
            "client",
            "created_at"
        };
        return new SimpleRowWriter.Table("public", "idempotent_action", columns);
    }
}

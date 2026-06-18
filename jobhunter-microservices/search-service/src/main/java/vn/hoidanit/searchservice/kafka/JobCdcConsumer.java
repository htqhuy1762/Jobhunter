package vn.hoidanit.searchservice.kafka;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.searchservice.domain.JobDocument;
import vn.hoidanit.searchservice.repository.JobSearchRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobCdcConsumer {

    private final ObjectMapper objectMapper;
    private final JobSearchRepository repository;

    @KafkaListener(topics = "${app.cdc.jobs-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            JsonNode envelope = root.has("payload") ? root.path("payload") : root;

            String operation = envelope.path("op").asText();
            JsonNode beforeNode = envelope.path("before");
            JsonNode afterNode = envelope.path("after");

            if ("d".equals(operation) && beforeNode != null && !beforeNode.isNull()) {
                Long deletedId = asLong(beforeNode.get("id"));
                if (deletedId != null) {
                    repository.deleteById(deletedId);
                    log.debug("Deleted job {} from Elasticsearch index", deletedId);
                }
                return;
            }

            if (afterNode == null || afterNode.isNull()) {
                return;
            }

            JobDocument document = mapToDocument(afterNode);
            if (document.getId() == null) {
                log.warn("Skip CDC event because job id is missing: {}", rawMessage);
                return;
            }

            repository.save(document);
            log.debug("Indexed/updated job {} in Elasticsearch", document.getId());
        } catch (IOException ex) {
            log.error("Failed to parse CDC message: {}", rawMessage, ex);
        } catch (Exception ex) {
            log.error("Failed to process CDC message", ex);
        }
    }

    private JobDocument mapToDocument(JsonNode afterNode) {
        JobDocument document = new JobDocument();
        document.setId(asLong(afterNode.get("id")));
        document.setName(asText(afterNode.get("name")));
        document.setDescription(asText(afterNode.get("description")));
        document.setLocation(asText(afterNode.get("location")));
        document.setSalary(asDouble(afterNode.get("salary")));
        document.setQuantity(asInteger(afterNode.get("quantity")));
        document.setLevel(asText(afterNode.get("level")));
        document.setCompanyId(asLong(afterNode.get("company_id")));
        document.setActive(asBoolean(afterNode.get("active")));
        document.setStartDate(parseTemporal(afterNode.get("start_date"), true));
        document.setEndDate(parseTemporal(afterNode.get("end_date"), true));
        document.setCreatedAt(parseTemporal(afterNode.get("created_at"), false));
        document.setUpdatedAt(parseTemporal(afterNode.get("updated_at"), false));
        document.setCreatedBy(asText(afterNode.get("created_by")));
        document.setUpdatedBy(asText(afterNode.get("updated_by")));
        return document;
    }

    private String asText(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private Long asLong(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }

    private Integer asInteger(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asInt();
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private Double asDouble(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : Double.valueOf(value);
    }

    private Boolean asBoolean(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return Boolean.valueOf(node.asText());
    }

    private Instant parseTemporal(JsonNode node, boolean dateOnly) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isNumber()) {
            if (dateOnly) {
                // Debezium can emit DATE columns as epoch days when using JsonConverter.
                LocalDate date = LocalDate.ofEpochDay(node.asLong());
                return date.atStartOfDay().toInstant(ZoneOffset.UTC);
            }

            long raw = node.asLong();
            // Debezium often emits TIMESTAMP columns as epoch microseconds.
            long epochMillis = raw > 10_000_000_000_000L ? raw / 1_000 : raw;
            return Instant.ofEpochMilli(epochMillis);
        }

        String raw = node.asText();
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            if (raw.length() == 10) {
                return LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC);
        }
    }
}




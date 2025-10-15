package com.teletronics.storage;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests covering functional requirements:
 * 1.1 Parallel upload same filename
 * 1.2 Parallel upload same contents
 * 1.3 Upload of a 2GB file
 * 1.4 Unauthorized delete attempt
 * 1.5 Listing of public files
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StorageApplicationIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    private HttpHeaders headersForUser(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", userId);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private MultiValueMap<String, Object> multipart(String filename, byte[] content) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new org.springframework.core.io.ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return body;
    }

    // =============================================================
    // 1.1 Simulate parallel UPLOAD of a file with the same FILENAME
    // =============================================================
    @Test
    @Order(1)
    void parallelUploadSameFilename() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        byte[] content = "duplicate filename test".getBytes(StandardCharsets.UTF_8);

        Callable<ResponseEntity<String>> uploadTask = () -> {
            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(multipart("duplicate.txt", content), headersForUser("userDuplicateFilename"));
            return restTemplate.postForEntity(getBaseUrl() + "/files/upload", request, String.class);
        };

        List<Future<ResponseEntity<String>>> futures = executor.invokeAll(Collections.nCopies(5, uploadTask));
        executor.shutdown();

        // Collect results
        List<Integer> statuses = futures.stream().map(f -> {
            try {
                return f.get().getStatusCode().value();
            } catch (Exception e) {
                return 500;
            }
        }).collect(Collectors.toList());

        long successCount = statuses.stream().filter(code -> code >= 200 && code < 300).count();

        System.out.println("Upload same filename responses: " + statuses);
        // Expect that only 1 succeeds (others rejected or conflict)
        assertThat(successCount).isEqualTo(1L);
    }

    // =============================================================
    // 1.2 Simulate parallel UPLOAD of a file with the same CONTENTS
    // =============================================================
    @Test
    @Order(2)
    void parallelUploadSameContents() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        byte[] identicalContent = "identical content test".getBytes(StandardCharsets.UTF_8);

        Callable<ResponseEntity<String>> uploadTask = () -> {
            String randomName = "file_" + UUID.randomUUID() + ".txt";
            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(multipart(randomName, identicalContent), headersForUser("userDuplicateContent"));
            return restTemplate.postForEntity(getBaseUrl() + "/files/upload", request, String.class);
        };

        List<Future<ResponseEntity<String>>> futures = executor.invokeAll(Collections.nCopies(5, uploadTask));
        executor.shutdown();

        List<Integer> statuses = futures.stream().map(f -> {
            try {
                return f.get().getStatusCode().value();
            } catch (Exception e) {
                return 500;
            }
        }).collect(Collectors.toList());

        long successCount = statuses.stream().filter(code -> code >= 200 && code < 300).count();

        System.out.println("Upload same content responses: " + statuses);
        assertThat(successCount).isEqualTo(1L);
    }

    // =============================================================
    // 1.3 Simulate UPLOAD of a FILE that is at least 2GB size
    // =============================================================
    @Test
    @Order(3)
    void uploadLargeFileSimulated() {
        final long sizeBytes = 2L * 1024 * 1024 * 1024; // 2GB
        final String filename = "larges_2GB_test.bin";

        // Fake stream that produces 2GB of zero bytes lazily
        InputStream fakeStream = new InputStream() {
            private long remaining = sizeBytes;
            @Override
            public int read() {
                if (remaining-- > 0) return 0;
                return -1;
            }
        };

        InputStreamResource resource = new InputStreamResource(fakeStream) {
            @Override
            public String getFilename() {
                return filename;
            }

            @Override
            public long contentLength() {
                return sizeBytes;
            }
        };

        HttpHeaders headers = headersForUser("userLargeFileSimulated");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        long start = System.currentTimeMillis();
        ResponseEntity<String> response =
                restTemplate.postForEntity(getBaseUrl() + "/files/upload", request, String.class);
        long duration = System.currentTimeMillis() - start;

        System.out.printf("2GB upload simulated: %d ms, status=%s%n",
                duration, response.getStatusCode());

        assertThat(response.getStatusCode().is2xxSuccessful()
                || response.getStatusCode().is5xxServerError()).isTrue();


        HttpHeaders userLargeFileHeaders = new HttpHeaders();
        userLargeFileHeaders.add("X-User-Id", "userLargeFileSimulated");
        HttpEntity<Void> listRequest = new HttpEntity<>(userLargeFileHeaders);

        ResponseEntity<String> listResponse = restTemplate.exchange(
                getBaseUrl() + "/files",
                HttpMethod.GET,
                listRequest,
                String.class
        );

        assertThat(listResponse.getBody()).contains("larges_2GB_test.bin");

    }

    // =============================================================
// 1.4 Try to delete file that does not belong to user
// =============================================================
    @Test
    @Order(4)
    void deleteFileNotOwnedByUser() {
        HttpHeaders ownerHeaders = new HttpHeaders();
        ownerHeaders.add("X-User-Id", "userOwner");

        MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
        uploadBody.add("file", new org.springframework.core.io.ByteArrayResource("delete secured content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_delete.txt";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(uploadBody, ownerHeaders);

        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(
                getBaseUrl() + "/files/upload", uploadRequest, Map.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploadResponse.getBody()).isNotNull();

        String fileId = (String) uploadResponse.getBody().get("id");
        assertThat(fileId).isNotEmpty();

        System.out.println("File uploaded by userOwner with id: " + fileId);

        HttpHeaders intruderHeaders = new HttpHeaders();
        intruderHeaders.add("X-User-Id", "intruder");

        HttpEntity<Void> deleteRequest = new HttpEntity<>(intruderHeaders);
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                getBaseUrl() + "/files/" + fileId,
                HttpMethod.DELETE,
                deleteRequest,
                String.class
        );

        System.out.println("Delete attempt by intruder -> status: " + deleteResponse.getStatusCode());
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        HttpHeaders ownerHeadersAfterDelete = new HttpHeaders();
        ownerHeadersAfterDelete.add("X-User-Id", "userOwner");
        HttpEntity<Void> request = new HttpEntity<>(ownerHeadersAfterDelete);

        ResponseEntity<String> listResponse = restTemplate.exchange(
                getBaseUrl() + "/files",
                HttpMethod.GET,
                request,
                String.class
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).contains("test_delete.txt");
    }


    // =============================================================
    // 1.5 List all public files
    // =============================================================
    @Test
    @Order(5)
    void listAllPublicFiles() {
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.add("X-User-Id", "userList");

        MultiValueMap<String, Object> uploadBody = new LinkedMultiValueMap<>();
        uploadBody.add("file", new org.springframework.core.io.ByteArrayResource("test list secured content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test_list_public.txt";
            }
        });
        uploadBody.add("visibility", "PUBLIC");

        HttpEntity<MultiValueMap<String, Object>> uploadRequest = new HttpEntity<>(uploadBody, uploadHeaders);

        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(
                getBaseUrl() + "/files/upload", uploadRequest, Map.class);

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploadResponse.getBody()).isNotNull();


        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.add("X-User-Id", "anonymous");
        listHeaders.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(listHeaders);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/files/public",
                HttpMethod.GET,
                request,
                String.class
        );

        System.out.println("Public files list: " + response.getBody());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("downloadLink");
        assertThat(response.getBody()).doesNotContain("PRIVATE");
        assertThat(response.getBody()).contains("PUBLIC");

    }
}
